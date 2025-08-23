# Docker镜像问题修复说明

## 问题原因

你遇到的问题是脚本中使用的镜像名称与你手动拉取的镜像名称不一致：

- **脚本中使用**：`openjdk:17-jre-slim`（JRE版本）
- **你手动拉取**：`openjdk:17-jdk-slim`（JDK版本）

## 镜像区别

| 镜像名称 | 大小 | 包含内容 | 用途 |
|---------|------|----------|------|
| `openjdk:17-jdk-slim` | ~400MB | JDK + JRE | 开发环境 |
| `openjdk:17-jre-slim` | ~200MB | 仅JRE | 生产环境 |

## 已修复的内容

1. **脚本配置**：
   - 将 `build-docker-optimized.sh` 中的基础镜像改为 `openjdk:17-jdk-slim`
   - 更新所有备用镜像源为JDK版本
   - 修改本地镜像检查逻辑

2. **Dockerfile更新**：
   - `bgai-service/Dockerfile`
   - `chat-agent/Dockerfile`
   - `deepSearch-service/Dockerfile`
   - 其他服务的Dockerfile已经是正确的

3. **快速修复脚本**：
   - 更新 `quick-fix.sh` 使用正确的镜像名称

## 使用方法

现在你可以正常使用构建脚本了：

```bash
# 检查网络连接
./build-docker-optimized.sh network

# 构建镜像
./build-docker-optimized.sh tag

# 如果网络有问题，使用离线模式
./build-docker-optimized.sh offline-tag
```

## 注意事项

1. **镜像大小**：使用JDK版本会比JRE版本大约200MB，但功能更完整
2. **兼容性**：JDK版本包含JRE，所以不会有兼容性问题
3. **性能**：在生产环境中，JRE版本启动更快，但JDK版本功能更全

## 如果仍然有问题

如果仍然遇到网络问题，可以：

1. **手动拉取镜像**：
   ```bash
   docker pull openjdk:17-jdk-slim
   ```

2. **使用离线模式**：
   ```bash
   ./build-docker-optimized.sh offline-tag
   ```

3. **配置Docker镜像源**：
   在 `/etc/docker/daemon.json` 中添加：
   ```json
   {
     "registry-mirrors": [
       "https://registry.cn-hangzhou.aliyuncs.com",
       "https://docker.mirrors.ustc.edu.cn"
     ]
   }
   ```

现在脚本应该可以正常工作了！
