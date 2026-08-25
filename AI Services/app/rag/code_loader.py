"""代码文件加载：读取项目代码目录，产出带语言/路径元数据的 Document。

支持 .java / .py / .js / .ts（可扩展），跳过二进制与常见构建目录。
"""

import logging
from pathlib import Path

from app.rag.loader import Document

logger = logging.getLogger(__name__)

SUPPORTED_EXTENSIONS = {
    ".java", ".py", ".js", ".ts",
    ".kt", ".go", ".cs", ".cpp", ".c", ".h",
}

LANGUAGE_BY_EXT = {
    ".java": "java",
    ".py": "python",
    ".js": "javascript",
    ".ts": "typescript",
    ".kt": "kotlin",
    ".go": "go",
    ".cs": "csharp",
    ".cpp": "cpp",
    ".c": "c",
    ".h": "c",
}

SKIP_DIRS = {
    ".git", "node_modules", "__pycache__", ".venv", "venv",
    "target", "build", "dist", ".idea", ".mvn", "out",
}


def language_from_path(path: str | Path) -> str:
    """根据扩展名返回语言标识。"""
    return LANGUAGE_BY_EXT.get(Path(path).suffix.lower(), "text")


class CodeLoader:
    """读取项目代码目录，返回 Document 列表。

    Document.metadata 含 file_name / file_path / language。
    """

    def load(self, path: str | Path) -> list[Document]:
        p = Path(path)
        if p.is_file():
            return [self._read(p)] if p.suffix.lower() in SUPPORTED_EXTENSIONS else []
        if p.is_dir():
            docs = []
            for f in sorted(p.rglob("*")):
                if not f.is_file():
                    continue
                if f.suffix.lower() not in SUPPORTED_EXTENSIONS:
                    continue
                if any(part in SKIP_DIRS for part in f.parts):
                    continue
                docs.append(self._read(f))
            logger.info("CodeLoader success: 加载 %d 个代码文件", len(docs))
            return docs
        raise FileNotFoundError(f"路径不存在: {path}")

    def _read(self, path: Path) -> Document:
        content = path.read_text(encoding="utf-8", errors="ignore")
        return Document(
            content=content,
            metadata={
                "file_name": path.name,
                "file_path": str(path),
                "language": language_from_path(path),
            },
        )
