"""CODE_REVIEW 代码上下文构建：代码加载 → 结构切片 → Embedding → 向量库 → 检索。"""

import logging

from app.rag.code_loader import CodeLoader
from app.rag.code_splitter import CodeSplitter
from app.embeddings.embedding import EmbeddingProvider
from app.rag.retriever import Retriever
from app.vectorstore.vectorstore import VectorStore

logger = logging.getLogger(__name__)


class CodeReviewContextBuilder:
    """为 CODE_REVIEW 构建代码检索上下文，独立于通用文档 RAG。"""

    def __init__(
        self,
        embedding: EmbeddingProvider,
        store: VectorStore,
        store_path: str = "",
        top_k: int = 5,
        min_score: float = 0.0,
    ):
        self.code_loader = CodeLoader()
        self.code_splitter = CodeSplitter()
        self.embedding = embedding
        self.store = store
        self.store_path = store_path
        self.top_k = top_k
        self.min_score = min_score
        self.retriever = Retriever(embedding, store, min_score=min_score)

    def ingest(self, path: str) -> int:
        """加载代码 → 切片 → 向量化 → 入库，返回入库块数。"""
        self.store.clear()  # 全量重建，避免与持久化旧数据重复
        docs = self.code_loader.load(path)
        chunks: list = []
        for doc in docs:
            chunks.extend(self.code_splitter.split(doc))
        vectors = self.embedding.embed_documents([c.content for c in chunks])
        for chunk, vector in zip(chunks, vectors):
            self.store.add(chunk.content, vector, metadata=chunk.metadata)
        count = len(chunks)
        logger.info("embedding success: %d 个向量入库", count)
        if self.store_path:
            self.store.save(self.store_path)
            logger.info("代码向量库已持久化到 %s", self.store_path)
        return count

    def retrieve(self, query: str) -> str:
        """按查询检索代码块，返回拼好的上下文文本。"""
        hits = self.retriever.search(query, top_k=self.top_k)
        logger.info("retrieval success: 命中 %d 个代码块", len(hits))
        if not hits:
            return ""
        return "\n\n".join(
            f"--- {h.metadata.get('file_path', h.metadata.get('file_name', ''))} ---\n{h.text}"
            for h in hits
        )
