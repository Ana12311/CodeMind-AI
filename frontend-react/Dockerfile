# =============================================================
# CodeMind AI Frontend — 多阶段构建
# Stage 1: Node 构建静态产物（tsc + vite build → dist）
# Stage 2: Nginx 托管静态站点 + 反代 /api 到 backend
#
# 敏感配置不进镜像：前端无密钥，API 走同源 /api 反代。
# =============================================================

# ---------- Stage 1: 构建 ----------
FROM node:20-alpine AS builder
WORKDIR /app

# 先复制依赖清单，利用层缓存
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---------- Stage 2: 运行 ----------
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80
