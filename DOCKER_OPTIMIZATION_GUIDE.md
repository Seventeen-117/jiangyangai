# Docker镜像优化指南

## 问题分析

你的Docker镜像之所以这么大（2.9GB），主要原因包括：

1. **JAR文件巨大**：每个服务的JAR文件都在1.2GB左右
2. **基础镜像**：使用`openjdk:17-jdk-slim`而不是更小的JRE版本
3. **构建上下文**：没有使用`.dockerignore`文件来减少构建上下文
4. **依赖冗余**：可能包含不必要的依赖

## 已实施的优化

### 1. 基础镜像优化
- 将所有服务的Dockerfile从 `openjdk:17-jdk-slim` 改为 `openjdk:17-jre-slim`
- JRE版本比JDK版本小约200-300MB

### 2. 添加.dockerignore文件
为所有主要服务添加了`.dockerignore`文件：
- `bgai-service`
- `chat-agent`
- `deepSearch-service`
- `gateway-service`
- `messages-service`
- `signature-service`

这些文件排除了不必要的文件，减少构建上下文大小。

### 3. 修复Nacos权限问题
**重要修复**：解决了Nacos客户端无法创建缓存目录的问题：
- 修改用户创建命令，确保appuser有正确的home目录
- 使用 `useradd -m -d /home/appuser` 创建用户并设置home目录
- 确保appuser对 `/home/appuser` 目录有写权限

### 4. 优化构建脚本
创建了 `build-docker-optimized.sh` 脚本，包含以下优化：
- 使用Docker BuildKit
- 显示JAR文件大小信息
- 更好的错误处理

## 使用方法

### 使用优化后的构建脚本
```bash
# 给脚本执行权限
chmod +x build-docker-optimized.sh

# 构建优化版本
./build-docker-optimized.sh build

# 构建并标签
./build-docker-optimized.sh tag

# 构建、标签并推送
./build-docker-optimized.sh push
```

### 分析镜像大小
```bash
# 给分析脚本执行权限
chmod +x analyze-image-size.sh

# 分析特定镜像
./analyze-image-size.sh bgai-service latest

# 分析所有镜像
./analyze-image-size.sh
```

## 修复的关键问题

### Nacos缓存目录权限问题
**错误信息**：
```
java.lang.IllegalStateException: failed to create cache dir: /home/appuser/nacos/naming/d750d92e-152f-4055-a641-3bc9dda85a29
```

**解决方案**：
1. 修改用户创建命令：
   ```dockerfile
   # 修复前
   RUN groupadd -r appuser && useradd -r -g appuser appuser
   RUN chown -R appuser:appuser /app
   
   # 修复后
   RUN groupadd -r appuser && useradd -r -g appuser -m -d /home/appuser appuser
   RUN chown -R appuser:appuser /app /home/appuser
   ```

2. 确保用户有正确的home目录和权限

## 进一步优化建议

### 1. 多阶段构建（推荐）
创建多阶段Dockerfile来进一步减少镜像大小：

```dockerfile
# 第一阶段：构建
FROM maven:3.9.6-openjdk-17-slim AS builder
WORKDIR /build
COPY . .
RUN mvn clean package -DskipTests -q -f service-name/pom.xml

# 第二阶段：运行
FROM openjdk:17-jre-slim
WORKDIR /app
COPY --from=builder /build/service-name/target/*.jar app.jar
# ... 其他配置
```

### 2. 依赖优化
检查Maven依赖，移除不必要的依赖：
- 使用 `mvn dependency:analyze` 分析依赖
- 将测试依赖设置为 `scope=test`
- 使用 `provided` scope 标记容器中不需要的依赖

### 3. JAR文件优化
- 使用 `spring-boot-maven-plugin` 的 `excludes` 配置排除不必要的依赖
- 考虑使用 `spring-boot-thin-launcher` 来创建更小的JAR文件

### 4. 使用更小的基础镜像
考虑使用以下基础镜像：
- `eclipse-temurin:17-jre-alpine` (更小)
- `amazoncorretto:17-alpine` (Amazon版本)
- `adoptopenjdk:17-jre-hotspot` (AdoptOpenJDK版本)

## 预期效果

实施这些优化后，预期镜像大小可以减少：
- 基础镜像优化：减少200-300MB
- .dockerignore优化：减少构建时间
- 多阶段构建：减少500MB-1GB
- 依赖优化：减少100-500MB

总体预期可以减少30-50%的镜像大小。

## 监控和验证

使用以下命令监控优化效果：

```bash
# 查看镜像大小
docker images

# 分析镜像层
docker history image-name:tag

# 比较优化前后的大小
docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"
```

## 注意事项

1. **重要**：确保优化后的镜像功能正常，特别是Nacos连接
2. 测试所有服务的启动和运行
3. 验证健康检查是否正常工作
4. 在生产环境部署前充分测试
5. 确保Nacos缓存目录权限正确

## 故障排除

如果仍然遇到Nacos权限问题：

1. 检查容器内用户权限：
   ```bash
   docker exec -it container-name ls -la /home/appuser
   ```

2. 手动创建目录：
   ```bash
   docker exec -it container-name mkdir -p /home/appuser/nacos/naming
   ```

3. 检查Nacos配置：
   ```yaml
   spring:
     cloud:
       nacos:
         discovery:
           cache-dir: /tmp/nacos/cache  # 使用临时目录
   ```
