"""LLM 服务：模型调用统一入口。

Agent 等上层模块只能通过 LLMService 调用模型，禁止直接实例化 Provider。
"""

from app.services.llm_service.service import LLMService

__all__ = ["LLMService", "get_llm_service"]


_llm_service: LLMService | None = None


def get_llm_service() -> LLMService:
    """获取全局 LLMService 单例，由配置决定使用哪个模型提供方。"""
    global _llm_service
    if _llm_service is None:
        from app.config.config import get_settings
        from app.services.llm_service.prompt_loader import PromptLoader
        from app.services.llm_service.providers import create_provider

        settings = get_settings()
        provider = create_provider(settings.llm_provider, **settings.model_dump())
        prompt_loader = PromptLoader(settings.prompts_dir)
        _llm_service = LLMService(provider=provider, prompt_loader=prompt_loader)
    return _llm_service
