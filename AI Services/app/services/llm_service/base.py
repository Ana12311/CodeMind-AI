"""模型调用抽象。"""

from abc import ABC, abstractmethod
from typing import Any

from pydantic import BaseModel


class LLMResult(BaseModel):
    """LLM 调用结果。"""

    content: str
    model: str = ""
    raw: Any = None


class LLMError(Exception):
    """LLM 调用异常。"""


class LLMProvider(ABC):
    """模型提供方抽象基类。

    未来新增模型只需继承此类并实现 complete，再注册到工厂即可。
    """

    @abstractmethod
    def complete(self, prompt: str, **kwargs) -> LLMResult:
        """调用模型并返回结构化结果。"""
        raise NotImplementedError
