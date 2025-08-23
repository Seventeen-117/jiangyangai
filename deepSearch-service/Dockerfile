# DeepSearch Service Dockerfile
# 深度搜索服务

FROM openjdk:17-jre-slim

# 设置工作目录
WORKDIR /app

# 设置环境变量
ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:+UseStringDeduplication"
ENV SPRING_PROFILES_ACTIVE=dev
ENV TZ=Asia/Shanghai
ENV NACOS_HOST=8.133.246.113
ENV NACOS_PORT=8848
ENV SERVER_PORT=8085

# 安装必要的工具
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 复制JAR文件
COPY target/deepSearch-service-*.jar app.jar

# 创建非root用户并设置home目录
RUN groupadd -r appuser && useradd -r -g appuser -m -d /home/appuser appuser
RUN chown -R appuser:appuser /app /home/appuser
USER appuser

# 暴露端口
EXPOSE 8085

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8085/deepSearch/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
