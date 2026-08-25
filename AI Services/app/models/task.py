"""任务相关请求/响应模型。"""

from pydantic import BaseModel, Field


class TaskCreateRequest(BaseModel):
    """创建 AI 任务请求。"""

    taskId: int = Field(..., description="任务ID")
    taskType: str = Field(..., description="任务类型")
    projectId: int = Field(..., description="项目ID")
    content: str = Field(default="", description="任务内容")


class TaskCreateResponse(BaseModel):
    """创建 AI 任务响应。"""

    taskId: int
    status: str
    message: str
