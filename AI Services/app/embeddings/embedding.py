"""Embedding：文本转向量。

默认使用本地 Hashing 实现（无外部依赖、离线可用）。
真实语义 Embedding（OpenAI 兼容 / sentence-transformers）后续在此接入。
"""

import hashlib
import logging
import math
import re
from abc import ABC, abstractmethod

logger = logging.getLogger(__name__)

_CJK_RE = re.compile(r"[一-鿿]")
_WORD_RE = re.compile(r"[a-z0-9_]+")


class EmbeddingProvider(ABC):
    """Embedding 提供方抽象基类。"""

    @abstractmethod
    def embed(self, text: str) -> list[float]:
        """单条文本转向量。"""
        raise NotImplementedError

    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        return [self.embed(t) for t in texts]

    def embed_query(self, text: str) -> list[float]:
        """查询向量。默认与文档同编码，语义模型可覆写为独立查询编码。"""
        return self.embed(text)


class HashingEmbeddingProvider(EmbeddingProvider):
    """特征哈希 Embedding，词袋 + 符号哈希 + L2 归一化。"""

    def __init__(self, dim: int = 256):
        self.dim = dim

    def embed(self, text: str) -> list[float]:
        vec = [0.0] * self.dim
        for token in self._tokens(text):
            digest = hashlib.md5(token.encode("utf-8")).digest()
            idx = int.from_bytes(digest[:4], "big") % self.dim
            sign = 1.0 if digest[4] % 2 == 0 else -1.0
            vec[idx] += sign
        norm = math.sqrt(sum(x * x for x in vec)) or 1.0
        return [x / norm for x in vec]

    @staticmethod
    def _tokens(text: str) -> list[str]:
        text = text.lower()
        tokens = _WORD_RE.findall(text)
        tokens += _CJK_RE.findall(text)
        return tokens or [text.strip()]


class SentenceTransformerEmbeddingProvider(EmbeddingProvider):
    """语义 Embedding（sentence-transformers / bge-m3），支持中文需求 ↔ 英文代码跨语言检索。"""

    def __init__(self, model_name: str = "BAAI/bge-m3", **kwargs):
        from sentence_transformers import SentenceTransformer

        self.model_name = model_name
        self._model = SentenceTransformer(model_name)
        logger.info("embedding model loaded: %s", model_name)

    def embed(self, text: str) -> list[float]:
        return self.embed_documents([text])[0]

    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        if not texts:
            return []
        vecs = self._model.encode(
            texts,
            normalize_embeddings=True,
            show_progress_bar=False,
            batch_size=16,
            convert_to_numpy=True,
        )
        return [v.tolist() for v in vecs]

    def embed_query(self, text: str) -> list[float]:
        return self.embed_documents([text])[0]


_EMBEDDING_CACHE: dict[tuple, EmbeddingProvider] = {}


def create_embedding(provider_type: str, **config) -> EmbeddingProvider:
    """根据配置创建 Embedding 提供方（同 provider+模型 单例，避免重复加载模型）。"""
    provider_type = (provider_type or "hashing").lower()
    model_name = config.get("model_name") or config.get("embedding_model_name") or "BAAI/bge-m3"
    key = (provider_type, model_name, config.get("dim"))
    if key in _EMBEDDING_CACHE:
        return _EMBEDDING_CACHE[key]
    if provider_type == "hashing":
        provider: EmbeddingProvider = HashingEmbeddingProvider(dim=config.get("dim", 256))
    elif provider_type in ("bge-m3", "sentence-transformers", "semantic"):
        provider = SentenceTransformerEmbeddingProvider(model_name=model_name)
    else:
        raise ValueError(f"不支持的 Embedding 提供方: {provider_type}")
    _EMBEDDING_CACHE[key] = provider
    return provider
