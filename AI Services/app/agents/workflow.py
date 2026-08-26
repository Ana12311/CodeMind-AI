"""Agent 工作流：明确线性流程，无自主循环。"""

from typing import Callable

from app.agents.models import WorkflowResult
from app.agents.planner import PlannerAgent
from app.agents.reviewer import ReviewerAgent
from app.agents.worker import WorkerAgent


class TaskCancelled(Exception):
    """任务在运行中被取消，用于中断工作流。"""


class AgentWorkflow:
    """固定三步：Planner → Worker(逐个) → Reviewer。可控，不循环。"""

    def __init__(self, planner: PlannerAgent, worker: WorkerAgent, reviewer: ReviewerAgent):
        self.planner = planner
        self.worker = worker
        self.reviewer = reviewer

    def run(
        self,
        task_id: int,
        task: str,
        task_type: str = "general",
        context: str = "",
        should_stop: Callable[[], bool] | None = None,
    ) -> WorkflowResult:
        if should_stop and should_stop():
            raise TaskCancelled("任务已取消")
        plan = self.planner.plan(task, task_type)

        step_results = []
        for step in plan.steps:
            if should_stop and should_stop():
                raise TaskCancelled("任务已取消")
            step_results.append(self.worker.execute(step, task=task))

        if should_stop and should_stop():
            raise TaskCancelled("任务已取消")
        review = self.reviewer.review(task, step_results, context)
        status = "completed" if review.approved else "rejected"
        return WorkflowResult(
            task_id=task_id,
            status=status,
            plan=plan,
            step_results=step_results,
            review=review,
        )
