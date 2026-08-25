"""文件读取。"""

from dataclasses import dataclass, field
from pathlib import Path

SUPPORTED_EXTENSIONS = {
    ".txt", ".md", ".rst",
    ".py", ".java", ".js", ".ts", ".go", ".kt", ".cs", ".cpp", ".c", ".h",
    ".json", ".yaml", ".yml", ".xml", ".html", ".css", ".sql",
}


@dataclass
class Document:
    """文档单元。"""

    content: str
    metadata: dict = field(default_factory=dict)


class FileLoader:
    """读取文件或目录，返回 Document 列表。"""

    def load(self, path: str | Path) -> list[Document]:
        p = Path(path)
        if p.is_file():
            return [self._read(p)]
        if p.is_dir():
            docs = []
            for f in sorted(p.rglob("*")):
                if f.is_file() and f.suffix.lower() in SUPPORTED_EXTENSIONS:
                    docs.append(self._read(f))
            return docs
        raise FileNotFoundError(f"路径不存在: {path}")

    def _read(self, path: Path) -> Document:
        content = path.read_text(encoding="utf-8", errors="ignore")
        return Document(content=content, metadata={"source": str(path)})
