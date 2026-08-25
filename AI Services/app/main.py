"""FastAPI 应用入口。"""

from fastapi import FastAPI

from app.api.task import router as task_router
from app.config.config import get_settings
from app.config.exceptions import register_exception_handlers
from app.config.logging import setup_logging

settings = get_settings()
setup_logging(settings.log_level)

app = FastAPI(title=settings.app_name)

register_exception_handlers(app)
app.include_router(task_router, prefix=settings.api_prefix)


@app.get("/health")
def health():
    return {"status": "ok"}
