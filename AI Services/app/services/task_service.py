"""任务业务逻辑：接收任务 → 异步调度 Agent 工作流 → 回调结果。"""

import logging
import threading
from concurrent.futures import ThreadPoolExecutor

from app.agents.workflow import AgentWorkflow
from app.models.task import TaskCreateRequest, TaskCreateResponse
from app.services.callback_service import CallbackService

logger = logging.getLogger(__name__)

STATUS_PROCESSING = "PROCESSING"
STATUS_SUCCESS = "SUCCESS"
STATUS_FAILED = "FAILED"

_WORKFLOW_STATUS_MAP = {
    "completed": STATUS_SUCCESS,
    "rejected": STATUS_FAILED,  # 审核不通过按 FAILED 处理，结论在 result.review 中
}


class TaskService:
    """AI 任务编排入口，异步执行。

    流程：接收任务 → 立即返回 PROCESSING → 后台执行工作流 → 回调 Spring Boot。
    """

    def __init__(self, workflow: AgentWorkflow | None = None, callback: CallbackService | None = None):
        self._workflow = workflow
        self._callback = callback
        self._code_rag = None
        self._tasks: dict[int, str] = {}
        self._lock = threading.Lock()
        self._executor = ThreadPoolExecutor(max_workers=4)

    @property
    def workflow(self) -> AgentWorkflow:
        if self._workflow is None:
            from app.agents import build_agent_workflow

            self._workflow = build_agent_workflow()
        return self._workflow

    @property
    def callback(self) -> CallbackService:
        if self._callback is None:
            from app.config.config import get_settings

            settings = get_settings()
            self._callback = CallbackService(
                settings.callback_url,
                settings.callback_timeout,
                retries=settings.callback_retries,
                retry_delay=settings.callback_retry_delay,
                internal_secret=settings.internal_secret,
            )
        return self._callback

    @property
    def code_review_rag(self):
        if self._code_rag is None:
            from app.config.config import get_settings
            from app.rag import build_code_review_rag

            settings = get_settings()
            builder = build_code_review_rag()
            if settings.code_review_dir:
                try:
                    builder.ingest(settings.code_review_dir)
                except Exception as exc:
                    logger.warning("代码入库失败，CODE_REVIEW 降级为无代码上下文: %s", exc)
            self._code_rag = builder
        return self._code_rag

    def create_task(self, request: TaskCreateRequest) -> TaskCreateResponse:
        task_id = request.taskId
        self._set_status(task_id, STATUS_PROCESSING)
        self._executor.submit(self._execute, request)
        logger.info("任务受理 taskId=%s taskType=%s", request.taskId, request.taskType)
        return TaskCreateResponse(
            taskId=request.taskId,
            status=STATUS_PROCESSING,
            message="任务已受理",
        )

    def get_status(self, task_id: int) -> str | None:
        with self._lock:
            return self._tasks.get(task_id)

    def _execute(self, request: TaskCreateRequest) -> None:
        task_id = request.taskId
        try:
            context = ""
            if request.taskType.upper() == "CODE_REVIEW":
                context = self._build_code_context()
            result = self.workflow.run(
                task_id=task_id,
                task=request.content,
                task_type=request.taskType,
                context=context,
            )
            status = _WORKFLOW_STATUS_MAP.get(result.status, STATUS_FAILED)
            result_payload = result.model_dump()
            result_payload["projectId"] = request.projectId
            self._set_status(task_id, status)
            if not self.callback.send(task_id, status, result_payload):
                logger.error("回调失败，任务标记 FAILED taskId=%s", request.taskId)
                self._set_status(task_id, STATUS_FAILED)
        except Exception as exc:
            logger.exception("任务执行失败 taskId=%s", request.taskId)
            self._set_status(task_id, STATUS_FAILED)
            self.callback.send(
                task_id,
                STATUS_FAILED,
                {"projectId": request.projectId, "error": str(exc)},
            )

    def _build_code_context(self) -> str:
        from app.config.config import get_settings

        settings = get_settings()
        if not settings.code_review_dir:
            return ""
        return self.code_review_rag.retrieve(settings.code_review_query)

    def _set_status(self, task_id: int, status: str) -> None:
        with self._lock:
            self._tasks[task_id] = status


task_service = TaskService()
