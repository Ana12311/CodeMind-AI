"""Prompt 模板加载。"""

from pathlib import Path

import yaml

PROJECT_ROOT = Path(__file__).resolve().parents[3]
DEFAULT_PROMPTS_DIR = PROJECT_ROOT / "app" / "prompts"


class _SafeDict(dict):
    """缺失变量占位保留为 {key}，不抛异常。"""

    def __missing__(self, key: str) -> str:
        return "{" + key + "}"


class PromptLoader:
    """从 prompts 目录加载 Prompt 模板并填充变量。

    支持 .txt / .md 纯文本模板，以及 .yaml（读取其中的 prompt 字段）。
    """

    def __init__(self, prompts_dir: str | Path | None = None):
        self.prompts_dir = Path(prompts_dir) if prompts_dir else DEFAULT_PROMPTS_DIR

    def load(self, name: str, **variables: str) -> str:
        path = self._resolve(name)
        template = self._read_template(path)
        return template.format_map(_SafeDict(**variables))

    def _resolve(self, name: str) -> Path:
        for ext in (".yaml", ".yml", ".txt", ".md"):
            candidate = self.prompts_dir / f"{name}{ext}"
            if candidate.exists():
                return candidate
        raise FileNotFoundError(f"未找到 Prompt: {name}（目录 {self.prompts_dir}）")

    def _read_template(self, path: Path) -> str:
        if path.suffix in (".yaml", ".yml"):
            data = yaml.safe_load(path.read_text(encoding="utf-8"))
            if isinstance(data, dict):
                return str(data.get("prompt", ""))
            return str(data)
        return path.read_text(encoding="utf-8")
