# =============================================================
# CodeMind AI Backend — 多阶段构建
# Stage 1: Gradle 构建（基于 Eclipse Temurin JDK 的 Gradle 官方镜像）
# Stage 2: JRE 运行环境（eclipse-temurin）
#
# 敏感配置一律通过 docker run -e 环境变量注入，镜像内不写死任何密码/secret。
# =============================================================

# ---------- Stage 1: 构建 ----------
FROM gradle:9.5.1-jdk17 AS builder
WORKDIR /app

# 先复制构建脚本（利用 Docker 层缓存，依赖未变时不重复下载）
COPY build.gradle settings.gradle ./
COPY src ./src

RUN gradle bootJar --no-daemon

# ---------- Stage 2: 运行 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# 非 root 用户
RUN groupadd -r app && useradd -r -g app app

# 复制可执行 fat jar（bootJar 产物唯一）
COPY --from=builder /app/build/libs/*.jar app.jar

# 端口
EXPOSE 8080

# 运行用户
USER app

# 容器化 JVM：按容器内存上限自动伸缩堆
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]

# 所需环境变量（通过 docker run -e / compose environment 注入）：
#   DB_HOST / DB_PORT / DB_NAME / DB_USERNAME / DB_PASSWORD
#   REDIS_HOST / REDIS_PORT / REDIS_DATABASE / REDIS_PASSWORD
#   JWT_SECRET
#   INTERNAL_SECRET
#   AI_SERVICE_URL
