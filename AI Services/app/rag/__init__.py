"""RAG 系统：文件读取 → 文本切片 → Embedding → 向量存储 → 相似度检索。"""

from app.embeddings.embedding import EmbeddingProvider
from app.rag.loader import Document
from app.rag.pipeline import RAGPipeline
from app.rag.retriever import Retriever, SearchHit
from app.vectorstore.vectorstore import VectorStore

__all__ = [
    "RAGPipeline",
    "Retriever",
    "SearchHit",
    "VectorStore",
    "Document",
    "EmbeddingProvider",
    "CodeReviewContextBuilder",
    "build_rag_pipeline",
    "build_code_review_rag",
]


def build_rag_pipeline() -> RAGPipeline:
    """按配置构建独立 RAG Pipeline 单例。"""
    from app.config.config import get_settings
    from app.embeddings.embedding import create_embedding
    from app.rag.loader import FileLoader
    from app.rag.splitter import TextSplitter

    settings = get_settings()
    embedding = create_embedding(
        settings.embedding_provider,
        dim=settings.embedding_dim,
        model_name=settings.embedding_model_name,
    )
    loader = FileLoader()
    splitter = TextSplitter(
        chunk_size=settings.chunk_size,
        chunk_overlap=settings.chunk_overlap,
    )
    store = VectorStore()
    if settings.vectorstore_path:
        store.load(settings.vectorstore_path)
    return RAGPipeline(
        loader=loader,
        splitter=splitter,
        embedding=embedding,
        store=store,
        min_score=settings.rag_min_score,
    )


def build_code_review_rag() -> "CodeReviewContextBuilder":
    """构建 CODE_REVIEW 代码检索上下文构建器（独立向量库，含持久化）。"""
    from app.config.config import get_settings
    from app.rag.code_review import CodeReviewContextBuilder
    from app.embeddings.embedding import create_embedding
    from app.vectorstore.vectorstore import VectorStore

    settings = get_settings()
    embedding = create_embedding(
        settings.embedding_provider,
        dim=settings.embedding_dim,
        model_name=settings.embedding_model_name,
    )
    store = VectorStore()
    if settings.code_vectorstore_path:
        store.load(settings.code_vectorstore_path)
    return CodeReviewContextBuilder(
        embedding=embedding,
        store=store,
        store_path=settings.code_vectorstore_path,
        top_k=settings.rag_top_k,
        min_score=settings.rag_min_score,
    )
