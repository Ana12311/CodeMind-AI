"""RAG Pipeline：文件 → 切片 → Embedding → 向量库 → 检索。"""

from app.embeddings.embedding import EmbeddingProvider
from app.rag.loader import FileLoader
from app.rag.retriever import Retriever, SearchHit
from app.rag.splitter import TextSplitter
from app.vectorstore.vectorstore import VectorStore


class RAGPipeline:
    """独立 RAG 流水线，与 Agent 解耦。"""

    def __init__(
        self,
        loader: FileLoader,
        splitter: TextSplitter,
        embedding: EmbeddingProvider,
        store: VectorStore,
        min_score: float = 0.0,
    ):
        self.loader = loader
        self.splitter = splitter
        self.embedding = embedding
        self.store = store
        self.retriever = Retriever(embedding, store, min_score=min_score)

    def ingest_path(self, path: str) -> int:
        """读取文件/目录 → 切片 → 入库，返回入库块数。"""
        docs = self.loader.load(path)
        count = 0
        for doc in docs:
            chunks = self.splitter.split(doc.content)
            for chunk in chunks:
                vector = self.embedding.embed(chunk)
                self.store.add(chunk, vector, metadata=doc.metadata)
                count += 1
        return count

    def query(self, text: str, top_k: int = 5) -> list[SearchHit]:
        return self.retriever.search(text, top_k)
