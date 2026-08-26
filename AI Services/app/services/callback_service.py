"""回调服务：将任务结果回传 Spring Boot（含 HMAC 内部鉴权签名）。"""

import hashlib
import hmac
import json
import logging
import time
from urllib.parse import urlparse

import httpx

logger = logging.getLogger(__name__)


class CallbackService:
    """执行完成后回调 Spring Boot 保存结果。

    回调需携带 HMAC-SHA256 签名，与后端 InternalAuthFilter 鉴权规则一致：

        canonical = METHOD + "\\n" + (path + query) + "\\n" + timestamp + "\\n" + body
        signature = lowercaseHex(HmacSHA256(internal_secret, canonical))

    请求头：X-Timestamp（epoch 毫秒）、X-Signature（hex 小写）。
    """

    def __init__(
        self,
        url: str = "",
        timeout: float = 10.0,
        retries: int = 3,
        retry_delay: float = 1.0,
        internal_secret: str = "",
    ):
        self.url = url
        self.timeout = timeout
        self.retries = max(0, retries)
        self.retry_delay = retry_delay
        self.internal_secret = internal_secret

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
            # 后端 AiTaskCallbackRequest.result 为 String，对象需序列化为 JSON 字符串
            if isinstance(result, (dict, list)):
                result = json.dumps(result, ensure_ascii=False, separators=(",", ":"))
            payload["result"] = result

        # 序列化一次，签名与请求体用完全相同的字符串，保证签名与后端收到的 body 逐字节一致
        body_str = json.dumps(payload, ensure_ascii=False, separators=(",", ":"))

        for attempt in range(self.retries + 1):
            try:
                resp = httpx.post(
                    self.url,
                    content=body_str.encode("utf-8"),
                    headers=self._build_headers(body_str),
                    timeout=self.timeout,
                )
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

    def _build_headers(self, body_str: str) -> dict[str, str]:
        """构造回调请求头（含 HMAC 签名）。未配置 internal_secret 时不加签名。"""
        headers = {"Content-Type": "application/json"}
        if not self.internal_secret:
            return headers

        parsed = urlparse(self.url)
        path_with_query = parsed.path
        if parsed.query:
            path_with_query += "?" + parsed.query

        timestamp = str(int(time.time() * 1000))
        canonical = f"POST\n{path_with_query}\n{timestamp}\n{body_str}"
        signature = hmac.new(
            self.internal_secret.encode("utf-8"),
            canonical.encode("utf-8"),
            hashlib.sha256,
        ).hexdigest()

        headers["X-Timestamp"] = timestamp
        headers["X-Signature"] = signature
        return headers
