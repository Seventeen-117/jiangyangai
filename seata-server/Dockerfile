# Seata Server Dockerfile
# 分布式事务协调器

FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 设置环境变量
ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC"
ENV TZ=Asia/Shanghai
ENV SEATA_PORT=8091
ENV SEATA_IP=0.0.0.0

# 安装必要的工具
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 复制Seata Server JAR文件
COPY seata-server.jar app.jar

# 复制配置文件
COPY conf/application.yml /app/conf/application.yml
COPY conf/registry.conf /app/conf/registry.conf

# 创建非root用户
RUN groupadd -r appuser && useradd -r -g appuser appuser
RUN chown -R appuser:appuser /app
USER appuser

# 暴露端口
EXPOSE 8091

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8091/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar -h $SEATA_IP -p $SEATA_PORT"]
