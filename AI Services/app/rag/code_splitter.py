"""代码切片：按 class / function / method 结构切分，而非按字符。

每个 chunk 携带 file_name / class_name / method_name / language / line 元数据。
结构无法识别时回退到按长度切片。
"""

import logging
import re

from app.rag.loader import Document
from app.rag.splitter import TextSplitter

logger = logging.getLogger(__name__)

# 各语言顶层定义识别（class/interface/enum、函数、方法）
_DEF_PATTERNS = {
    "java": re.compile(
        r"^\s*(?:(?:public|private|protected)\s+)?(?:static\s+)?"
        r"(?:class|interface|enum)\s+([A-Za-z_][\w]*)|"
        r"^\s*(?:(?:public|private|protected)\s+)?(?:static\s+)?(?:final\s+)?"
        r"[\w<>\[\],\s]+\s+([A-Za-z_][\w]*)\s*\([^;]*\)\s*\{",
        re.MULTILINE,
    ),
    "python": re.compile(
        r"^class\s+([A-Za-z_][\w]*)|"
        r"^[ \t]+(?:async\s+)?def\s+([A-Za-z_][\w]*)|"
        r"^(?:async\s+)?def\s+([A-Za-z_][\w]*)",
        re.MULTILINE,
    ),
    "javascript": re.compile(
        r"^(?:export\s+)?(?:class|function)\s+([A-Za-z_$][\w$]*)|"
        r"^\s*(?:async\s+)?([A-Za-z_$][\w$]*)\s*\([^)]*\)\s*\{",
        re.MULTILINE,
    ),
    "typescript": re.compile(
        r"^(?:export\s+)?(?:class|interface|function)\s+([A-Za-z_$][\w$]*)|"
        r"^\s*(?:async\s+)?([A-Za-z_$][\w$]*)\s*\([^)]*\)\s*\{",
        re.MULTILINE,
    ),
}

_CLASS_KEYWORD = re.compile(r"\b(class|interface|enum)\b")


class CodeSplitter:
    """结构感知代码切片器。"""

    def __init__(self, max_chunk_size: int = 2000, chunk_overlap: int = 0):
        self.max_chunk_size = max_chunk_size
        self.fallback = TextSplitter(chunk_size=max_chunk_size, chunk_overlap=chunk_overlap)

    def split(self, doc: Document) -> list[Document]:
        language = doc.metadata.get("language", "text")
        pattern = _DEF_PATTERNS.get(language)
        lines = doc.content.splitlines()

        if not pattern:
            return self._fallback_chunks(doc)

        defs: list[tuple[int, str, str]] = []  # (line_no, kind, name)
        for m in pattern.finditer(doc.content):
            line_no = doc.content.count("\n", 0, m.start()) + 1
            matched = m.group(0)
            name = next((g for g in m.groups() if g), "")
            if not name:
                continue
            kind = "class" if _CLASS_KEYWORD.search(matched) else "method"
            defs.append((line_no, kind, name))

        if not defs:
            return self._fallback_chunks(doc)

        chunks: list[Document] = []
        current_class = ""
        for i, (start_line, kind, name) in enumerate(defs):
            end_line = defs[i + 1][0] - 1 if i + 1 < len(defs) else len(lines)
            block = "\n".join(lines[start_line - 1:end_line])
            if kind == "class":
                current_class = name
                method_name = ""
            else:
                method_name = name
            chunks.append(self._make_chunk(doc, block, start_line, current_class, method_name))

        # 定义之前的头部（package/import 等）单独成块
        if defs[0][0] > 1:
            head = "\n".join(lines[: defs[0][0] - 1]).strip()
            if head:
                chunks.insert(0, self._make_chunk(doc, head, 1, "", ""))

        # 超过阈值的块用长度切片兜底
        result: list[Document] = []
        for c in chunks:
            if len(c.content) > self.max_chunk_size:
                for sub in self.fallback.split(c.content):
                    result.append(
                        self._make_chunk(
                            doc, sub,
                            c.metadata.get("line", 1),
                            c.metadata.get("class_name", ""),
                            c.metadata.get("method_name", ""),
                        )
                    )
            else:
                result.append(c)
        logger.info("chunk generated: %d 个代码块", len(result))
        return result

    def _make_chunk(
        self,
        doc: Document,
        code: str,
        line: int,
        class_name: str,
        method_name: str,
    ) -> Document:
        header = (
            f"[文件: {doc.metadata.get('file_name', '')}]"
            f"[类: {class_name or '-'}][方法: {method_name or '-'}]"
            f"[起始行: {line}]"
        )
        return Document(
            content=f"{header}\n{code.strip()}",
            metadata={
                **doc.metadata,
                "line": line,
                "class_name": class_name,
                "method_name": method_name,
            },
        )

    def _fallback_chunks(self, doc: Document) -> list[Document]:
        return [self._make_chunk(doc, sub, 1, "", "") for sub in self.fallback.split(doc.content)]
