"""Agent 数据模型。"""

from pydantic import BaseModel


class Step(BaseModel):
    """一个可执行子步骤。"""

    id: int
    description: str


class Plan(BaseModel):
    """任务拆解结果。"""

    steps: list[Step]


class StepResult(BaseModel):
    """子步骤执行结果。"""

    step: Step
    output: str


class Issue(BaseModel):
    """代码审查问题项。"""

    file: str = ""
    line: str = ""
    level: str = "P2"  # P0 | P1 | P2
    problem: str = ""
    suggestion: str = ""


class ReviewResult(BaseModel):
    """审核结论。"""

    approved: bool
    summary: str = ""
    issues: list[Issue] = []


class WorkflowResult(BaseModel):
    """整个工作流最终结果。"""

    task_id: int
    status: str  # completed | rejected
    plan: Plan
    step_results: list[StepResult]
    review: ReviewResult
