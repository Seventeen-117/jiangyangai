# 使用OpenJDK 17作为基础镜像
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 设置环境变量
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"
ENV SERVER_PORT=8690
ENV NACOS_SERVER_ADDR=8.133.246.113:8848
ENV NACOS_GROUP=DEFAULT_GROUP
ENV NACOS_NAMESPACE=d750d92e-152f-4055-a641-3bc9dda85a29

# 复制Maven配置文件
COPY pom.xml .
COPY src ./src

# 安装Maven并构建项目
RUN apt-get update && \
    apt-get install -y maven && \
    mvn clean package -DskipTests && \
    apt-get remove -y maven && \
    apt-get autoremove -y && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 复制构建好的jar文件
COPY target/aiAgent-1.0.0-Final.jar app.jar

# 创建日志目录
RUN mkdir -p /app/logs

# 暴露端口
EXPOSE 8690

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8690/api/aiAgent/health || exit 1

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
