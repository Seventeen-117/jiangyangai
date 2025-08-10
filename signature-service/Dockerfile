# 使用 OpenJDK 17 作为基础镜像
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 复制 Maven 配置文件
COPY pom.xml .

# 复制源代码
COPY src ./src

# 设置环境变量
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SPRING_PROFILES_ACTIVE="prod"

# 暴露端口
EXPOSE 8689

# 创建非 root 用户
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 构建应用
RUN apt-get update && apt-get install -y maven && \
    mvn clean package -DskipTests && \
    mv target/signature-service-*.jar app.jar && \
    apt-get remove -y maven && \
    apt-get autoremove -y && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    chown appuser:appuser /app

# 切换到非 root 用户
USER appuser

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"] 