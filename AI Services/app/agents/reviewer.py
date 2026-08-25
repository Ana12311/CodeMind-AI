"""Reviewer Agent：结果审核。"""

import logging

from app.agents.base import BaseAgent
from app.agents.models import Issue, ReviewResult, StepResult

logger = logging.getLogger(__name__)


class ReviewerAgent(BaseAgent):
    """审核执行结果，判定通过与否。"""

    def review(self, task: str, step_results: list[StepResult], context: str = "") -> ReviewResult:
        results_text = "\n".join(
            f"{r.step.id}. {r.step.description}\n  结果: {r.output}" for r in step_results
        )
        if context:
            logger.info("reviewer context injected: %d 字符", len(context))
        raw = self._generate(
            "reviewer",
            {"task": task, "results": results_text, "context": context},
        )
        logger.info("LLM completed: reviewer 审核完成")
        return self._parse(raw)

    def _parse(self, raw: str) -> ReviewResult:
        data = self._extract_json(raw)
        if isinstance(data, dict):
            approved = bool(data.get("approved", False))
            summary = str(data.get("summary", raw.strip()))
            issues: list[Issue] = []
            for item in data.get("issues", []):
                if isinstance(item, dict):
                    try:
                        issues.append(Issue(**item))
                    except Exception:
                        pass
            return ReviewResult(approved=approved, summary=summary, issues=issues)
        approved = "通过" in raw or "approved" in raw.lower()
        return ReviewResult(approved=approved, summary=raw.strip(), issues=[])
