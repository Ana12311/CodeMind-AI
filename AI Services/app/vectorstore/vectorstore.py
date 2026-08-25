"""向量存储：内存实现 + JSON 持久化。"""

import json
import math
import uuid
from dataclasses import dataclass, field, asdict
from pathlib import Path


@dataclass
class VectorRecord:
    """一条向量记录。"""

    id: str
    text: str
    vector: list[float]
    metadata: dict = field(default_factory=dict)


@dataclass
class SearchHit:
    """检索命中。"""

    text: str
    score: float
    metadata: dict = field(default_factory=dict)


class VectorStore:
    """简单内存向量库，余弦相似度检索。"""

    def __init__(self):
        self._records: list[VectorRecord] = []

    def add(self, text: str, vector: list[float], metadata: dict | None = None) -> str:
        record = VectorRecord(
            id=uuid.uuid4().hex,
            text=text,
            vector=vector,
            metadata=metadata or {},
        )
        self._records.append(record)
        return record.id

    def search(self, query_vector: list[float], top_k: int = 5) -> list[SearchHit]:
        if not self._records:
            return []
        scored = []
        for r in self._records:
            score = self._cosine(query_vector, r.vector)
            scored.append(SearchHit(text=r.text, score=score, metadata=r.metadata))
        scored.sort(key=lambda h: h.score, reverse=True)
        return scored[:top_k]

    def clear(self) -> None:
        self._records.clear()

    def __len__(self) -> int:
        return len(self._records)

    @staticmethod
    def _cosine(a: list[float], b: list[float]) -> float:
        return sum(x * y for x, y in zip(a, b))

    def save(self, path: str | Path) -> None:
        path = Path(path)
        path.parent.mkdir(parents=True, exist_ok=True)
        data = [asdict(r) for r in self._records]
        path.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")

    def load(self, path: str | Path) -> None:
        path = Path(path)
        if not path.exists():
            return
        data = json.loads(path.read_text(encoding="utf-8"))
        self._records = [VectorRecord(**item) for item in data]
