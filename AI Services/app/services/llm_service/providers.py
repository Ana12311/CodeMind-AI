"""模型提供方实现与工厂。"""

import logging

from openai import OpenAI

from app.services.llm_service.base import LLMError, LLMProvider, LLMResult

logger = logging.getLogger(__name__)


class MockProvider(LLMProvider):
    """占位提供方，不调用真实模型，仅回显 Prompt。用于本地联调。"""

    def __init__(self, **kwargs):
        # Mock 无需真实配置，忽略工厂传入的全部 settings 字段。
        pass

    def complete(self, prompt: str, **kwargs) -> LLMResult:
        logger.info("MockProvider 调用，prompt 长度=%d", len(prompt))
        return LLMResult(content=f"[mock] {prompt}", model="mock")


class OpenAICompatibleProvider(LLMProvider):
    """OpenAI 兼容接口提供方，覆盖 DeepSeek、通义千问、Moonshot、Ollama 等。"""

    def __init__(self, api_key: str, base_url: str, model: str):
        if not api_key:
            raise LLMError("缺少 api_key，请在 .env 中配置")
        self.model = model
        self.client = OpenAI(api_key=api_key, base_url=base_url)

    def complete(self, prompt: str, **kwargs) -> LLMResult:
        try:
            response = self.client.chat.completions.create(
                model=self.model,
                messages=[{"role": "user", "content": prompt}],
                **kwargs,
            )
        except Exception as exc:
            raise LLMError(f"模型调用失败: {exc}") from exc

        content = response.choices[0].message.content or ""
        return LLMResult(content=content, model=self.model, raw=response)


def create_provider(provider_type: str, **config) -> LLMProvider:
    """根据配置创建模型提供方。

    未来新增模型：在此增加分支，无需改动上层。
    """
    provider_type = (provider_type or "mock").lower()

    if provider_type == "mock":
        return MockProvider(**config)

    if provider_type == "deepseek":
        return OpenAICompatibleProvider(
            api_key=config.get("deepseek_api_key", ""),
            base_url=config.get("deepseek_base_url", "https://api.deepseek.com"),
            model=config.get("deepseek_model", "deepseek-chat"),
        )

    raise ValueError(f"不支持的模型提供方: {provider_type}")
