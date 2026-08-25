"""Agent 基类。"""

import json
import re
from abc import ABC

from pydantic import BaseModel

from app.services.llm_service import LLMService


class AgentResult(BaseModel):
    """Agent 通用输出。"""

    content: str
    metadata: dict = {}


class BaseAgent(ABC):
    """所有 Agent 的基类。

    约束：Agent 只能通过 self.llm（LLMService）调用模型，禁止直接访问模型 API。
    """

    def __init__(self, llm_service: LLMService):
        self.llm = llm_service

    def _generate(self, prompt_name: str, variables: dict | None = None) -> str:
        return self.llm.generate(prompt_name, variables=variables).content

    @staticmethod
    def _extract_json(text: str):
        """从文本中提取 JSON（容忍 markdown 代码围栏）。"""
        text = re.sub(r"```(?:json)?", "", text).strip()
        match = re.search(r"\{.*\}|\[.*\]", text, re.DOTALL)
        if not match:
            return None
        try:
            return json.loads(match.group())
        except json.JSONDecodeError:
            return None
