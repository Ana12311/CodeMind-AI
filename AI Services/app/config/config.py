"""应用配置管理。"""

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """应用配置，支持从环境变量与 .env 文件读取。"""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    app_name: str = "AI Execution Service"
    app_env: str = "development"
    log_level: str = "INFO"
    api_prefix: str = "/api"

    # LLM
    llm_provider: str = "mock"  # mock | deepseek | ...
    prompts_dir: str = ""  # 为空时使用项目根目录 /prompts

    # DeepSeek（OpenAI 兼容）
    deepseek_api_key: str = ""
    deepseek_base_url: str = "https://api.deepseek.com"
    deepseek_model: str = "deepseek-chat"

    # RAG
    embedding_provider: str = "hashing"  # hashing | bge-m3
    embedding_model_name: str = "BAAI/bge-m3"  # bge-m3 语义模型，禁止硬编码到代码
    embedding_dim: int = 256  # 仅 hashing 使用；语义模型维度由模型决定
    chunk_size: int = 500
    chunk_overlap: int = 50
    vectorstore_path: str = ""  # 为空时纯内存，不持久化
    rag_docs_dir: str = "docs"  # 启动时自动入库的文档目录，空=不入库
    rag_top_k: int = 3  # Worker 检索返回条数
    rag_min_score: float = 0.5  # 检索命中最低相似度阈值，低于则视为无匹配（bge-m3 跨语言余弦经验区间 0.5~0.7）

    # CODE_REVIEW 代码 RAG
    code_review_dir: str = ""  # 项目代码目录，空=不启用代码入库
    code_vectorstore_path: str = ""  # 代码向量库持久化路径，空=纯内存
    code_review_query: str = "分析代码中的安全问题、设计问题、性能问题"
    # 代码检索最低相似度阈值：跨语言（中文查询 vs 英文代码）通用查询实测 0.3~0.45，
    # 低于通用文档检索阈值（0.5），否则代码检索恒为空。
    code_review_min_score: float = 0.3

    # 回调（Spring Boot）
    callback_url: str = ""  # 结果回调地址，空=不回调
    callback_timeout: float = 10.0
    callback_retries: int = 3  # 回调失败重试次数
    callback_retry_delay: float = 1.0  # 重试间隔（秒）

    # 内部服务 HMAC 密钥（回调签名，与后端 InternalAuthFilter 一致）
    internal_secret: str = ""  # 空=回调不加签名（仅本地调试）

    @property
    def is_production(self) -> bool:
        return self.app_env.lower() == "production"


@lru_cache
def get_settings() -> Settings:
    """获取全局唯一配置实例。"""
    return Settings()
