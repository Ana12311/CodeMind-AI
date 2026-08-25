"""AI 任务接口。"""

from fastapi import APIRouter

from app.models.task import TaskCreateRequest, TaskCreateResponse
from app.services.task_service import task_service

router = APIRouter(prefix="/tasks", tags=["tasks"])


@router.post("", response_model=TaskCreateResponse)
def create_task(request: TaskCreateRequest) -> TaskCreateResponse:
    return task_service.create_task(request)
