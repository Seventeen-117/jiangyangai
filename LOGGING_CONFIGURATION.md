# 主项目日志配置说明

## 问题说明

之前项目中出现了以下问题：
1. **主项目根目录下出现了 `logs/` 目录**：这是不应该的，日志应该在各服务自己的目录下
2. **主项目根目录下出现了 `logPath_IS_UNDEFINED/` 目录**：这表明有环境变量未正确设置
3. **各服务的日志路径配置不一致**：有些使用相对路径，有些使用绝对路径

## 问题原因

### 1. 硬编码的服务名路径
之前的配置中使用了类似这样的路径：
```xml
<file>bgai-service/logs/${springAppName}.log</file>
<file>gateway-service/logs/${springAppName}.log</file>
<file>signature-service/logs/${springAppName}.log</file>
```

这会导致：
- 日志文件被创建在错误的位置
- 路径解析问题
- 主项目根目录下出现不必要的目录

### 2. 环境变量未正确设置
某些配置可能依赖环境变量，当环境变量未设置时，会出现 `logPath_IS_UNDEFINED` 这样的路径。

### 3. 相对路径与绝对路径混用
不同服务使用了不同的路径配置方式，导致日志文件分散在不同位置。

## 修复方案

### 1. 统一使用相对路径
所有服务的logback配置都改为使用相对路径：
```xml
<!-- 修复前 -->
<file>bgai-service/logs/${springAppName}.log</file>

<!-- 修复后 -->
<file>logs/${springAppName}.log</file>
```

### 2. 日志目录结构
每个服务启动后，会在自己的工作目录下创建 `logs/` 目录：
```
项目根目录/
├── bgai-service/
│   ├── logs/                    # bgai-service的日志目录
│   │   ├── bgai-service.log
│   │   └── bgai-service.2025-08-11.log
│   └── ...
├── gateway-service/
│   ├── logs/                    # gateway-service的日志目录
│   │   ├── gateway-service.log
│   │   ├── gateway-service-error.log
│   │   └── gateway-service-route.log
│   └── ...
├── signature-service/
│   ├── logs/                    # signature-service的日志目录
│   │   ├── signature-service.log
│   │   ├── signature-service-error.log
│   │   └── signature-service-signature.log
│   └── ...
├── messages-service/
│   ├── logs/                    # messages-service的日志目录
│   │   ├── messages-service.log
│   │   └── messages-service-error.log
│   └── ...
└── aiAgent/
    ├── logs/                    # aiAgent的日志目录
    │   ├── aiAgent-service.log
    │   ├── aiAgent-service-error.log
    │   └── aiAgent-chat.log
    └── ...
```

### 3. 已修复的服务
- ✅ **bgai-service**: 使用相对路径 `logs/${springAppName}.log`
- ✅ **gateway-service**: 使用相对路径 `logs/${springAppName}.log`
- ✅ **signature-service**: 使用相对路径 `logs/${springAppName}.log`
- ✅ **messages-service**: 使用相对路径 `logs/${springAppName}.log`
- ✅ **aiAgent**: 使用相对路径 `logs/${springAppName}.log`

## 配置最佳实践

### 1. 路径配置原则
- **使用相对路径**: `logs/${springAppName}.log`
- **避免硬编码服务名**: 不要使用 `bgai-service/logs/` 这样的路径
- **使用Spring属性**: 通过 `${springAppName}` 获取服务名

### 2. 日志文件命名
- **主日志**: `${springAppName}.log`
- **错误日志**: `${springAppName}-error.log`
- **业务日志**: `${springAppName}-{业务类型}.log`

### 3. 滚动策略
- **按日期滚动**: `%d{yyyy-MM-dd}`
- **按大小滚动**: 单文件最大100MB
- **历史保留**: 保留30天

## 环境变量配置

### 1. 日志级别环境变量
```bash
# 设置根日志级别
export LOGGING_LEVEL_ROOT=INFO

# 设置特定包的日志级别
export LOGGING_LEVEL_COM_JIANGYANG_MESSAGES=DEBUG
export LOGGING_LEVEL_COM_YUE_AIAGENT=DEBUG

# 设置日志文件路径（可选）
export LOGGING_FILE_PATH=logs
```

### 2. 服务启动环境变量
```bash
# Nacos配置
export NACOS_SERVER_ADDR=8.133.246.113:8848
export NACOS_GROUP=DEFAULT_GROUP
export NACOS_NAMESPACE=d750d92e-152f-4055-a641-3bc9dda85a29

# 服务端口
export SERVER_PORT=8688  # 根据服务调整
```

## 故障排查

### 1. 日志文件位置错误
**症状**: 日志文件出现在主项目根目录下
**原因**: 使用了绝对路径或错误的相对路径
**解决**: 检查logback配置，确保使用正确的相对路径

### 2. 路径包含服务名
**症状**: 日志路径包含 `bgai-service/logs/` 这样的服务名
**原因**: 硬编码了服务名在路径中
**解决**: 改为使用 `logs/` 相对路径

### 3. 环境变量未设置
**症状**: 出现 `logPath_IS_UNDEFINED` 这样的路径
**原因**: 依赖的环境变量未设置
**解决**: 设置必要的环境变量，或使用默认值

## 监控和维护

### 1. 日志目录监控
- 监控各服务的日志目录大小
- 定期清理历史日志文件
- 检查日志文件权限

### 2. 日志内容监控
- 监控错误日志数量
- 分析日志性能指标
- 设置日志告警规则

### 3. 定期检查
- 检查日志文件位置是否正确
- 验证日志滚动策略是否生效
- 确认日志级别配置是否合适

## 总结

通过统一使用相对路径配置，避免了日志文件出现在错误位置的问题。现在每个服务都会在自己的工作目录下创建 `logs/` 目录，日志文件管理更加清晰和规范。

**关键要点**:
1. 使用相对路径 `logs/${springAppName}.log`
2. 避免硬编码服务名在路径中
3. 统一各服务的日志配置方式
4. 定期检查和维护日志配置
