"""LLM 服务封装：加载 Prompt → 调用模型 → 格式化输出。"""

import logging

from app.services.llm_service.base import LLMProvider, LLMResult
from app.services.llm_service.prompt_loader import PromptLoader

logger = logging.getLogger(__name__)


class LLMService:
    """模型调用统一入口。

    上层模块（Agent 等）只能通过此类调用模型。
    """

    def __init__(self, provider: LLMProvider, prompt_loader: PromptLoader):
        self.provider = provider
        self.prompt_loader = prompt_loader

    def generate(self, prompt_name: str, variables: dict | None = None, **kwargs) -> LLMResult:
        """按 Prompt 名称生成。

        Args:
            prompt_name: prompts 目录下的模板文件名（不含扩展名）。
            variables: 模板变量。
            kwargs: 透传给 provider 的参数（temperature、max_tokens 等）。
        """
        prompt = self.prompt_loader.load(prompt_name, **(variables or {}))
        result = self.provider.complete(prompt, **kwargs)
        return self._format_output(result)

    def complete(self, prompt: str, **kwargs) -> LLMResult:
        """直接以完整文本调用模型（不经过模板）。"""
        result = self.provider.complete(prompt, **kwargs)
        return self._format_output(result)

    def _format_output(self, result: LLMResult) -> LLMResult:
        """输出格式化：去除首尾空白。"""
        content = result.content.strip() if result.content else ""
        return LLMResult(content=content, model=result.model, raw=result.raw)
