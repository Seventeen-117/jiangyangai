# Messages Service 配置文件说明

## 配置文件概述

本目录包含了 `messages-service` 服务的所有配置文件，支持本地配置和Nacos配置中心两种方式。

## 配置文件说明

### 1. application.yml
- **用途**: 主配置文件，包含所有环境通用的配置
- **加载顺序**: 在 `bootstrap.yml` 之后加载
- **特点**: 包含完整的消息服务配置，适用于本地开发

### 2. application-dev.yml
- **用途**: 开发环境专用配置
- **激活方式**: `spring.profiles.active=dev`
- **特点**: 
  - 启用调试日志
  - 使用本地消息中间件
  - 较短的超时时间和重试次数

### 3. application-prod.yml
- **用途**: 生产环境专用配置
- **激活方式**: `spring.profiles.active=prod`
- **特点**:
  - 使用生产环境消息中间件地址
  - 较长的超时时间和重试次数
  - 启用性能监控和健康检查

### 4. bootstrap.yml
- **用途**: Spring Cloud启动配置
- **加载顺序**: 最先加载
- **特点**: 配置Nacos配置中心和注册中心

### 5. nacos-config.yml
- **用途**: 专门用于导入Nacos配置中心的配置文件
- **导入方式**: 手动导入到Nacos
- **特点**: 包含环境变量占位符，支持动态配置

## 导入Nacos配置中心

### 方法1: 通过Nacos控制台导入

1. 登录Nacos控制台
2. 进入"配置管理" -> "配置列表"
3. 点击"+"号创建配置
4. 填写配置信息：
   - **Data ID**: `messages-service.yml`
   - **Group**: `JIANGYANG`
   - **格式**: YAML
   - **配置内容**: 复制 `nacos-config.yml` 文件内容

### 方法2: 通过Nacos API导入

```bash
curl -X POST 'http://localhost:8848/nacos/v1/cs/configs' \
  -d 'dataId=messages-service.yml' \
  -d 'group=JIANGYANG' \
  -d 'content=配置文件内容' \
  -d 'type=yaml'
```

### 方法3: 通过Nacos客户端工具导入

使用Nacos提供的配置导入工具，将 `nacos-config.yml` 文件内容导入。

## 环境变量配置

支持通过环境变量覆盖配置：

### RocketMQ配置
- `ROCKETMQ_NAMESRV_ADDR`: RocketMQ Name Server地址

### Kafka配置
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka服务器地址

### RabbitMQ配置
- `RABBITMQ_HOST`: RabbitMQ主机地址
- `RABBITMQ_PORT`: RabbitMQ端口
- `RABBITMQ_USERNAME`: RabbitMQ用户名
- `RABBITMQ_PASSWORD`: RabbitMQ密码
- `RABBITMQ_VIRTUAL_HOST`: RabbitMQ虚拟主机

### Nacos配置
- `NACOS_SERVER_ADDR`: Nacos服务器地址
- `NACOS_CONFIG_GROUP`: 配置分组
- `NACOS_DISCOVERY_GROUP`: 服务发现分组
- `NACOS_NAMESPACE`: 命名空间

## 配置优先级

配置加载优先级（从高到低）：
1. 环境变量
2. Nacos配置中心
3. 本地配置文件
4. 默认值

## 配置热更新

启用Nacos配置热更新后，修改配置中心中的配置会自动刷新到应用，无需重启服务。

## 注意事项

1. **生产环境**: 建议使用Nacos配置中心，避免配置文件泄露
2. **敏感信息**: 密码等敏感信息建议使用环境变量或Nacos加密配置
3. **配置验证**: 导入配置后，建议验证配置的正确性
4. **备份配置**: 重要配置建议在本地备份

## 故障排查

### 配置加载失败
- 检查Nacos服务是否正常
- 验证配置格式是否正确
- 检查网络连接

### 配置不生效
- 确认配置已正确导入Nacos
- 检查应用是否正确连接到Nacos
- 验证配置分组和命名空间是否正确

### 配置热更新不工作
- 确认启用了配置监听
- 检查应用是否有配置变更监听器
- 验证Nacos配置中心设置
