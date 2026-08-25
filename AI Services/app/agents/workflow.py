"""Agent 工作流：明确线性流程，无自主循环。"""

from app.agents.models import WorkflowResult
from app.agents.planner import PlannerAgent
from app.agents.reviewer import ReviewerAgent
from app.agents.worker import WorkerAgent


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
    ) -> WorkflowResult:
        plan = self.planner.plan(task, task_type)
        step_results = [self.worker.execute(step, task=task) for step in plan.steps]
        review = self.reviewer.review(task, step_results, context)
        status = "completed" if review.approved else "rejected"
        return WorkflowResult(
            task_id=task_id,
            status=status,
            plan=plan,
            step_results=step_results,
            review=review,
        )
