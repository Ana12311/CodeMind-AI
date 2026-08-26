"""AI 任务接口。"""

from fastapi import APIRouter

from app.models.task import TaskCreateRequest, TaskCreateResponse
from app.services.task_service import task_service

router = APIRouter(prefix="/tasks", tags=["tasks"])


@router.post("", response_model=TaskCreateResponse)
def create_task(request: TaskCreateRequest) -> TaskCreateResponse:
    return task_service.create_task(request)


@router.post("/{task_id}/cancel")
def cancel_task(task_id: int) -> dict:
    """中断运行中的任务（供后端删除任务时调用）。"""
    cancelled = task_service.cancel_task(task_id)
    return {"taskId": task_id, "cancelled": cancelled}
