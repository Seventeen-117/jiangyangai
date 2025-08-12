# AI Agent Service 日志配置说明

## 概述

AI Agent Service 使用 Logback 作为日志框架，通过 `logback-spring.xml` 配置文件进行日志管理。支持多环境配置、日志分级、异步输出、AI聊天日志分离等功能。

## 配置文件

- **主配置文件**: `logback-spring.xml`
- **位置**: `src/main/resources/logback-spring.xml`

## 日志输出配置

### 1. 控制台输出
- **Appender**: `CONSOLE`
- **用途**: 开发环境下的实时日志查看
- **格式**: 包含时间戳、线程、级别、类名、消息

### 2. 文件输出
- **主日志文件**: `logs/aiAgent-service.log`
- **错误日志文件**: `logs/aiAgent-service-error.log`
- **AI聊天日志文件**: `logs/aiAgent-chat.log`
- **滚动策略**: 按日期和大小滚动，单文件最大100MB，保留30天

### 3. 异步输出
- **队列大小**: 512
- **丢弃阈值**: 0（不丢弃日志）
- **性能优化**: 提高日志写入性能

## 环境配置

### 开发环境 (dev)
```xml
<springProfile name="dev">
    <!-- 控制台 + 文件输出 -->
    <!-- 应用日志级别: DEBUG -->
    <!-- 框架日志级别: INFO -->
    <!-- AI服务日志级别: DEBUG -->
</springProfile>
```

### 生产环境 (prod)
```xml
<springProfile name="prod">
    <!-- 仅文件输出，无控制台 -->
    <!-- 应用日志级别: INFO -->
    <!-- 框架日志级别: WARN -->
    <!-- AI服务日志级别: INFO -->
</springProfile>
```

### 本地环境 (local)
```xml
<springProfile name="local">
    <!-- 控制台 + 文件输出 -->
    <!-- 所有日志级别: DEBUG -->
    <!-- AI服务日志级别: DEBUG -->
</springProfile>
```

## 日志级别配置

### 应用日志
- **包路径**: `com.yue.aiAgent`
- **开发环境**: DEBUG
- **生产环境**: INFO
- **本地环境**: DEBUG

### AI聊天日志
- **类路径**: `com.yue.aiAgent.service.impl.AiChatServiceImpl`
- **专用文件**: `logs/aiAgent-chat.log`
- **开发环境**: DEBUG
- **生产环境**: INFO
- **本地环境**: DEBUG

### 第三方框架日志
- **Dubbo**: INFO (dev) / WARN (prod)
- **Nacos**: INFO (dev) / WARN (prod)
- **Seata**: INFO (dev) / WARN (prod)
- **Spring Cloud**: INFO (dev) / WARN (prod)
- **MyBatis**: DEBUG (dev) / WARN (prod)

### AI服务日志
- **Spring AI**: DEBUG (dev) / INFO (prod)
- **OpenAI**: DEBUG (dev) / INFO (prod)
- **Azure OpenAI**: DEBUG (dev) / INFO (prod)
- **Ollama**: DEBUG (dev) / INFO (prod)

## 日志文件结构

```
logs/
├── aiAgent-service.log              # 主日志文件
├── aiAgent-service.2025-08-11.0.log # 按日期滚动的日志文件
├── aiAgent-service-error.log        # 错误日志文件
├── aiAgent-service-error.2025-08-11.0.log # 错误日志滚动文件
├── aiAgent-chat.log                 # AI聊天日志文件
└── aiAgent-chat.2025-08-11.0.log   # AI聊天日志滚动文件
```

## 日志格式

```
2025-08-11 23:45:12.345 [main] INFO  com.yue.aiAgent.AiAgentApplication - Started AiAgentApplication in 3.456 seconds
```

**格式说明**:
- `2025-08-11 23:45:12.345`: 时间戳
- `[main]`: 线程名
- `INFO`: 日志级别
- `com.yue.aiAgent.AiAgentApplication`: 类名
- `Started AiAgentApplication in 3.456 seconds`: 日志消息

## 特殊日志配置

### AI聊天日志分离
AI聊天相关的日志会单独输出到 `aiAgent-chat.log` 文件，便于：
- 监控AI服务调用情况
- 分析用户聊天行为
- 排查AI服务问题
- 统计AI服务性能

### 错误日志过滤
错误级别的日志会单独输出到 `aiAgent-service-error.log` 文件，便于：
- 快速定位错误
- 错误统计分析
- 告警系统集成

## 性能优化

### 异步输出
- 使用 `AsyncAppender` 提高日志写入性能
- 队列大小设置为512，避免内存溢出
- 丢弃阈值为0，确保不丢失日志

### 滚动策略
- 按日期滚动，便于日志管理
- 单文件大小限制为100MB，避免单个文件过大
- 保留30天的历史日志，平衡存储空间和日志完整性

## 环境变量配置

可以通过环境变量调整日志配置：

```bash
# 设置日志级别
export LOGGING_LEVEL_ROOT=INFO
export LOGGING_LEVEL_COM_YUE_AIAGENT=DEBUG

# 设置日志文件路径
export LOGGING_FILE_PATH=logs

# 设置AI聊天日志级别
export LOGGING_LEVEL_AI_CHAT=DEBUG
```

## 故障排查

### 1. 日志文件过大
- 检查滚动策略配置
- 调整 `maxFileSize` 和 `maxHistory` 参数
- 考虑增加日志压缩

### 2. 日志输出性能问题
- 检查异步配置
- 调整队列大小和丢弃阈值
- 监控日志写入延迟

### 3. 日志级别不生效
- 确认 `spring.profiles.active` 配置
- 检查 `logback-spring.xml` 文件路径
- 验证Spring Boot版本兼容性

### 4. AI聊天日志问题
- 检查AI聊天日志文件权限
- 验证AI服务连接状态
- 监控AI服务响应时间

## 最佳实践

### 1. 开发环境
- 启用控制台输出，便于调试
- 设置详细的日志级别
- 使用异步输出提高性能
- 监控AI服务调用情况

### 2. 生产环境
- 禁用控制台输出，减少性能影响
- 设置合适的日志级别，避免信息泄露
- 定期清理历史日志文件
- 监控AI服务性能和错误率

### 3. 日志内容
- 使用有意义的日志消息
- 包含必要的上下文信息
- 避免记录敏感信息（如API密钥）
- 记录AI服务调用的关键参数

### 4. AI服务日志
- 记录AI模型调用次数和响应时间
- 监控AI服务错误率和重试情况
- 分析用户聊天模式和偏好
- 优化AI服务配置参数

## 监控和告警

### 日志监控
- 监控日志文件大小和数量
- 检查错误日志频率
- 分析日志性能指标
- 监控AI服务调用情况

### 告警配置
- 错误日志数量阈值告警
- 日志文件大小告警
- 日志写入失败告警
- AI服务错误率告警
- AI服务响应时间告警

### 性能指标
- 日志写入延迟
- 异步队列使用率
- 日志文件滚动频率
- AI服务调用成功率

## 扩展配置

### 自定义日志格式
可以通过修改 `LOG_PATTERN` 属性来自定义日志格式：

```xml
<property name="LOG_PATTERN" value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%logger{50}] - %msg%n"/>
```

### 添加新的日志分类
可以为不同的业务模块添加专门的日志文件：

```xml
<appender name="BUSINESS_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <!-- 业务日志配置 -->
</appender>
```

### 集成ELK Stack
可以将日志输出配置为JSON格式，便于集成ELK Stack：

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
```
