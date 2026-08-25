"""工具抽象与注册表。

Worker Agent 通过 ToolRegistry 匹配并执行工具。
具体工具（code_analyzer / file_reader / git_tool）后续在此注册。
"""

from abc import ABC, abstractmethod


class Tool(ABC):
    """工具基类。"""

    name: str = ""
    description: str = ""

    @abstractmethod
    def run(self, *args, **kwargs) -> str:
        raise NotImplementedError


class ToolRegistry:
    """按名称关键词匹配工具。"""

    def __init__(self):
        self._tools: dict[str, Tool] = {}

    def register(self, tool: Tool) -> None:
        self._tools[tool.name] = tool

    def match(self, text: str) -> Tool | None:
        lowered = text.lower()
        for name, tool in self._tools.items():
            if name.lower() in lowered:
                return tool
        return None
