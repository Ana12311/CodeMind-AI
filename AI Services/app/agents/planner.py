"""Planner Agent：任务拆解。"""

import re

from app.agents.base import BaseAgent
from app.agents.models import Plan, Step


class PlannerAgent(BaseAgent):
    """将任务拆解为有序子步骤。"""

    def plan(self, task: str, task_type: str = "general") -> Plan:
        raw = self._generate("planner", {"task": task, "task_type": task_type})
        return self._parse(raw, task)

    def _parse(self, raw: str, task: str) -> Plan:
        data = self._extract_json(raw)
        steps: list[Step] = []

        if isinstance(data, dict) and isinstance(data.get("steps"), list):
            for i, item in enumerate(data["steps"], start=1):
                if isinstance(item, str):
                    steps.append(Step(id=i, description=item))
                elif isinstance(item, dict):
                    steps.append(
                        Step(id=int(item.get("id", i)), description=str(item.get("description", "")))
                    )

        if not steps:
            steps = self._parse_lines(raw)

        if not steps:
            steps = [Step(id=1, description=task)]

        return Plan(steps=[s for s in steps if s.description])

    @staticmethod
    def _parse_lines(raw: str) -> list[Step]:
        steps: list[Step] = []
        for line in raw.splitlines():
            m = re.match(r"^(?:\d+[.、)]|[-*])\s*(.+)", line.strip())
            if m and m.group(1):
                steps.append(Step(id=len(steps) + 1, description=m.group(1)))
        return steps
