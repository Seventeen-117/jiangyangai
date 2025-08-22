# Docker 构建指南

本文档介绍如何使用更新后的构建脚本为 jiangyangAI 项目构建 Docker 镜像。

## 更新内容

### 1. 修复了镜像标签问题
- 现在按照你的示例方式进行镜像构建和标签
- 支持本地构建、远程标签和推送三种模式

### 2. 镜像命名规范
```bash
# 本地镜像名称
<service-name>:latest

# 远程镜像名称  
registry.cn-shanghai.aliyuncs.com/bg-boot/<service-name>:latest
```

### 3. 构建流程
1. **本地构建**: `docker build -t <service-name>:latest .`
2. **远程标签**: `docker tag <service-name>:latest registry.cn-shanghai.aliyuncs.com/bg-boot/<service-name>:latest`
3. **推送镜像**: `docker push registry.cn-shanghai.aliyuncs.com/bg-boot/<service-name>:latest`

## 使用方法

### Linux/Mac 系统

```bash
# 给脚本添加执行权限
chmod +x build-docker.sh

# 1. 只构建本地镜像
./build-docker.sh build

# 2. 构建并标签到远程仓库（不推送）
./build-docker.sh tag

# 3. 构建、标签并推送到远程仓库
./build-docker.sh push

# 4. 清理本地镜像
./build-docker.sh clean

# 5. 显示帮助信息
./build-docker.sh help
```

### Windows 系统

```cmd
# 1. 只构建本地镜像
build-docker.bat build

# 2. 构建并标签到远程仓库（不推送）
build-docker.bat tag

# 3. 构建、标签并推送到远程仓库
build-docker.bat push

# 4. 清理本地镜像
build-docker.bat clean

# 5. 显示帮助信息
build-docker.bat help
```

## 支持的服务

脚本会自动构建以下服务的 Docker 镜像：

| 服务名称 | 描述 |
|---------|------|
| base-service | 基础服务（动态数据源） |
| bgai-service | BGAI 服务 |
| chat-agent | 聊天代理服务 |
| deepSearch-service | 深度搜索服务 |
| gateway-service | 网关服务 |
| messages-service | 消息服务 |
| signature-service | 签名服务 |
| dubbo-api | Dubbo API 接口 |
| seata-server | 分布式事务协调器 |

## 构建示例

### 示例1：只构建本地镜像进行测试
```bash
./build-docker.sh build
```

输出示例：
```
构建 base-service...
  编译 base-service...
  构建Docker镜像 base-service...
base-service 处理完成

构建 bgai-service...
  编译 bgai-service...
  构建Docker镜像 bgai-service...
bgai-service 处理完成

...

本地镜像列表：
base-service           latest    abc123    2 minutes ago    500MB
bgai-service          latest    def456    1 minute ago     600MB
```

### 示例2：构建并标签（准备推送）
```bash
./build-docker.sh tag
```

输出示例：
```
构建 base-service...
  编译 base-service...
  构建Docker镜像 base-service...
  标签 base-service 到远程仓库...
base-service 处理完成

...

远程标签镜像列表：
registry.cn-shanghai.aliyuncs.com/bg-boot/base-service    latest    abc123    2 minutes ago    500MB
registry.cn-shanghai.aliyuncs.com/bg-boot/bgai-service   latest    def456    1 minute ago     600MB
```

### 示例3：构建、标签并推送到生产环境
```bash
./build-docker.sh push
```

输出示例：
```
构建 base-service...
  编译 base-service...
  构建Docker镜像 base-service...
  标签 base-service 到远程仓库...
  推送 base-service 到远程仓库...
base-service 处理完成

...

所有Docker镜像处理完成！
```

## 手动构建单个服务

如果你只想构建某个特定服务，可以手动执行：

```bash
# 进入服务目录
cd bgai-service

# 编译项目
mvn clean package -DskipTests

# 构建本地镜像
docker build -t bgai-service:latest .

# 标签到远程仓库
docker tag bgai-service:latest registry.cn-shanghai.aliyuncs.com/bg-boot/bgai-service:latest

# 推送到远程仓库
docker push registry.cn-shanghai.aliyuncs.com/bg-boot/bgai-service:latest
```

## 前置要求

### 1. 环境要求
- Java 17+
- Maven 3.6+
- Docker 20.10+

### 2. 阿里云镜像仓库认证
```bash
# 登录阿里云镜像仓库
docker login registry.cn-shanghai.aliyuncs.com
```

### 3. 网络要求
- 能够访问阿里云镜像仓库
- Maven 能够下载依赖

## 故障排除

### 1. 编译失败
```bash
# 检查 Java 版本
java -version

# 检查 Maven 版本
mvn -version

# 清理 Maven 缓存
mvn clean
```

### 2. Docker 构建失败
```bash
# 检查 Docker 服务状态
docker version

# 查看构建日志
docker build -t <service-name>:latest . --no-cache
```

### 3. 推送失败
```bash
# 重新登录阿里云镜像仓库
docker login registry.cn-shanghai.aliyuncs.com

# 检查网络连接
ping registry.cn-shanghai.aliyuncs.com
```

### 4. 镜像过大
```bash
# 查看镜像大小
docker images | grep <service-name>

# 优化 Dockerfile（添加 .dockerignore）
# 使用多阶段构建
# 清理不必要的文件
```

## 最佳实践

### 1. 构建顺序
建议按以下顺序构建：
1. `base-service` - 其他服务依赖的基础服务
2. 其他业务服务
3. `seata-server` - 分布式事务协调器

### 2. 版本管理
- 开发环境使用 `latest` 标签
- 生产环境使用具体版本号，如 `v1.0.0`

### 3. 镜像优化
- 使用 `.dockerignore` 排除不必要的文件
- 使用多阶段构建减小镜像大小
- 定期清理未使用的镜像

### 4. 安全考虑
- 不要在镜像中包含敏感信息
- 使用非 root 用户运行容器
- 定期更新基础镜像

## 与 Kubernetes 集成

构建完成的镜像可以直接用于 Kubernetes 部署：

```bash
# 构建并推送镜像
./build-docker.sh push

# 部署到 Kubernetes
./quick-deploy.sh
```

镜像名称已经在 Kubernetes 配置文件中更新为正确的远程仓库地址。
