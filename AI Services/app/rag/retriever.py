"""相似度检索。"""

from app.embeddings.embedding import EmbeddingProvider
from app.vectorstore.vectorstore import SearchHit, VectorStore


class Retriever:
    """查询文本 → Embedding → 向量库相似度检索。"""

    def __init__(self, embedding: EmbeddingProvider, store: VectorStore, min_score: float = 0.0):
        self.embedding = embedding
        self.store = store
        self.min_score = min_score

    def add_texts(self, texts: list[str], metadata: dict | None = None) -> int:
        """文本切片 → Embedding → 入向量库。"""
        vectors = self.embedding.embed_documents(texts)
        for text, vec in zip(texts, vectors):
            self.store.add(text, vec, metadata)
        return len(texts)

    def search(self, query: str, top_k: int = 5) -> list[SearchHit]:
        query_vector = self.embedding.embed_query(query)
        hits = self.store.search(query_vector, top_k)
        return [h for h in hits if h.score >= self.min_score]
