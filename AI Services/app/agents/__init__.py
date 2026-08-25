"""Agent 系统：Planner / Worker / Reviewer 三个 Agent + 明确工作流。"""

import logging

from app.agents.models import (
    Plan,
    ReviewResult,
    Step,
    StepResult,
    WorkflowResult,
)
from app.agents.planner import PlannerAgent
from app.agents.reviewer import ReviewerAgent
from app.agents.worker import WorkerAgent
from app.agents.workflow import AgentWorkflow

logger = logging.getLogger(__name__)

__all__ = [
    "AgentWorkflow",
    "PlannerAgent",
    "WorkerAgent",
    "ReviewerAgent",
    "Step",
    "Plan",
    "StepResult",
    "ReviewResult",
    "WorkflowResult",
    "build_agent_workflow",
]


def build_agent_workflow() -> AgentWorkflow:
    """按配置构建工作流，三个 Agent 共享同一个 LLMService，Worker 接入 RAG。"""
    from app.config.config import get_settings
    from app.rag import build_rag_pipeline
    from app.services.llm_service import get_llm_service

    settings = get_settings()
    llm = get_llm_service()

    rag = build_rag_pipeline()
    if settings.rag_docs_dir:
        try:
            count = rag.ingest_path(settings.rag_docs_dir)
            logger.info("RAG 入库 %s -> %d 块", settings.rag_docs_dir, count)
        except Exception as exc:
            logger.warning("RAG 入库失败，跳过增强，任务继续执行: %s", exc)

    return AgentWorkflow(
        planner=PlannerAgent(llm),
        worker=WorkerAgent(llm, retriever=rag.retriever, top_k=settings.rag_top_k),
        reviewer=ReviewerAgent(llm),
    )
