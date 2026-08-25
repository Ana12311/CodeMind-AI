"""Worker Agent：执行具体子任务，支持 RAG 检索增强。"""

import logging

from app.agents.base import BaseAgent
from app.agents.models import Step, StepResult
from app.agents.tools import ToolRegistry
from app.rag.retriever import Retriever

logger = logging.getLogger(__name__)


class WorkerAgent(BaseAgent):
    """执行单个子步骤：优先匹配工具，否则走 LLM（带 RAG 上下文）。"""

    def __init__(
        self,
        llm_service,
        tools: ToolRegistry | None = None,
        retriever: Retriever | None = None,
        top_k: int = 3,
    ):
        super().__init__(llm_service)
        self.tools = tools or ToolRegistry()
        self.retriever = retriever
        self.top_k = top_k

    def execute(self, step: Step, task: str = "") -> StepResult:
        tool = self.tools.match(step.description)
        if tool:
            output = tool.run(step.description)
        else:
            context = self._retrieve_context(task, step.description)
            output = self._generate(
                "worker",
                {"task": task, "step": step.description, "context": context},
            )
        return StepResult(step=step, output=output.strip())

    def _retrieve_context(self, task: str, step: str) -> str:
        if self.retriever is None:
            return ""
        query = f"{task}\n{step}"
        try:
            hits = self.retriever.search(query, top_k=self.top_k)
        except Exception as exc:
            logger.warning("RAG 检索失败: %s", exc)
            return ""
        if not hits:
            return ""
        return "\n".join(f"- {h.text}" for h in hits)
