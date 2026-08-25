# syntax=docker/dockerfile:1

# 基础镜像：官方 Python 3.12 slim（Debian），体积小、依赖精简，适合生产部署
FROM python:3.12-slim

# 运行环境变量（非机密）：关闭字节码写入/缓冲，加速日志与依赖安装
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

# 工作目录
WORKDIR /app

# 先装 torch（CPU 构建），避免 PyPI 默认 CUDA 版拉取多 GB nvidia_* 依赖
# 默认从官方 CPU 索引装（全球通用，torch==2.13.0+cpu）。
# 国内加速：--build-arg PIP_TORCH_WHEEL=<阿里云 wheel 直链> 直接装指定 wheel，绕开慢速 R2。
ARG PIP_TORCH_INDEX_URL=https://download.pytorch.org/whl/cpu
ARG TORCH_VERSION=2.13.0+cpu
ARG PIP_TORCH_WHEEL=""
RUN if [ -n "${PIP_TORCH_WHEEL}" ]; then \
        pip install --no-cache-dir --default-timeout=300 --retries 5 "${PIP_TORCH_WHEEL}"; \
    else \
        pip install --no-cache-dir --default-timeout=300 --retries 5 \
            --index-url "${PIP_TORCH_INDEX_URL}" "torch==${TORCH_VERSION}"; \
    fi

# 再装其余依赖（torch 已就位，sentence-transformers 不会再拉 CUDA torch）
# 索引源可用 --build-arg PIP_INDEX_URL 覆盖
COPY requirements.txt .
ARG PIP_INDEX_URL=https://pypi.org/simple
RUN pip install --no-cache-dir --default-timeout=300 --retries 5 \
      -i ${PIP_INDEX_URL} -r requirements.txt

# 复制应用代码（app 内含 api/services/agents/rag/embeddings/vectorstore/prompts/config）
COPY app ./app

# 非 root 运行（生产最佳实践）
RUN useradd --create-home --uid 10001 --shell /usr/sbin/nologin appuser
USER appuser

# 服务端口
EXPOSE 8000

# 启动：uvicorn，入口 app.main:app
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
