"""文本切片。"""


class TextSplitter:
    """按长度切片，带重叠，尽量在换行处断开。"""

    def __init__(self, chunk_size: int = 500, chunk_overlap: int = 50):
        if chunk_overlap >= chunk_size:
            raise ValueError("chunk_overlap 必须小于 chunk_size")
        self.chunk_size = chunk_size
        self.chunk_overlap = chunk_overlap

    def split(self, text: str) -> list[str]:
        text = text.strip()
        if not text:
            return []
        if len(text) <= self.chunk_size:
            return [text]

        chunks: list[str] = []
        start = 0
        while start < len(text):
            end = start + self.chunk_size
            chunk = text[start:end]
            # 未到末尾时，优先在换行处截断，避免切断行
            if end < len(text):
                nl = chunk.rfind("\n")
                if nl > self.chunk_size * 0.5:
                    end = start + nl + 1
                    chunk = text[start:end]
            chunks.append(chunk.strip())
            start = end - self.chunk_overlap
        return [c for c in chunks if c]
