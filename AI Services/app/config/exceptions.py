"""全局异常处理。"""

import logging

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

logger = logging.getLogger(__name__)


class AppException(Exception):
    """业务异常基类。"""

    def __init__(self, status_code: int, message: str):
        self.status_code = status_code
        self.message = message
        super().__init__(message)


def register_exception_handlers(app: FastAPI) -> None:
    """注册全局异常处理器。"""

    @app.exception_handler(AppException)
    async def app_exception_handler(request: Request, exc: AppException) -> JSONResponse:
        logger.warning("业务异常: %s", exc.message)
        return JSONResponse(status_code=exc.status_code, content={"detail": exc.message})

    @app.exception_handler(Exception)
    async def unhandled_exception_handler(request: Request, exc: Exception) -> JSONResponse:
        logger.exception("未处理异常: %s", exc)
        return JSONResponse(status_code=500, content={"detail": "Internal server error"})
