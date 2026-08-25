"""回调服务：将任务结果回传 Spring Boot。"""

import logging
import time

import httpx

logger = logging.getLogger(__name__)


class CallbackService:
    """执行完成后回调 Spring Boot 保存结果。"""

    def __init__(
        self,
        url: str = "",
        timeout: float = 10.0,
        retries: int = 3,
        retry_delay: float = 1.0,
    ):
        self.url = url
        self.timeout = timeout
        self.retries = max(0, retries)
        self.retry_delay = retry_delay

    def send(self, task_id: int, status: str, result: dict | None = None) -> bool:
        """回调结果。

        载荷：{taskId, status, result}

        返回：True = 无需回调（未配置 url）或回调成功；False = 回调失败（重试耗尽）。
        """
        if not self.url:
            logger.info("未配置 callback_url，跳过回调 taskId=%s status=%s", task_id, status)
            return True

        payload: dict = {"taskId": task_id, "status": status}
        if result is not None:
            payload["result"] = result

        for attempt in range(self.retries + 1):
            try:
                resp = httpx.post(self.url, json=payload, timeout=self.timeout)
                resp.raise_for_status()
                logger.info("回调成功 taskId=%s status=%s", task_id, status)
                return True
            except Exception as exc:
                if attempt < self.retries:
                    logger.warning("回调失败 taskId=%s 第 %d 次: %s", task_id, attempt + 1, exc)
                    time.sleep(self.retry_delay)
                else:
                    logger.error("回调最终失败 taskId=%s: %s", task_id, exc)
        return False
