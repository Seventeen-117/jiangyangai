# AI智能代理服务 (aiAgent-service)

## 服务概述

AI智能代理服务是一个基于Spring Boot的微服务，集成了多种AI模型（OpenAI、Azure OpenAI、Ollama），提供统一的AI聊天接口和代理管理功能。

## 技术架构

- **框架**: Spring Boot 3.2.5
- **微服务**: Spring Cloud 2023.0.1
- **服务发现**: Nacos
- **配置中心**: Nacos
- **RPC框架**: Dubbo 3.2.8
- **数据库**: MySQL + MyBatis-Plus
- **AI集成**: Spring AI 1.0.1
- **连接池**: HikariCP

## 服务端口

- **服务端口**: 8690
- **健康检查**: `/api/aiAgent/health`

## 主要功能

### 1. AI代理管理
- 支持多种AI类型：OpenAI、Azure OpenAI、Ollama
- 代理配置管理
- 连接状态监控
- 代理启用/禁用

### 2. AI聊天服务
- 统一聊天接口
- 支持流式响应
- 多模型切换
- 聊天记录存储

### 3. 微服务集成
- Nacos服务注册与发现
- Dubbo RPC服务
- 网关路由配置
- 断路器、重试、限流

## API接口

### AI代理管理接口

#### 查询AI代理列表
```
GET /api/aiAgent/list?current=1&size=10&name=&type=
```

#### 查询单个AI代理
```
GET /api/aiAgent/{id}
```

#### 新增AI代理
```
POST /api/aiAgent
Content-Type: application/json

{
  "name": "OpenAI GPT-4",
  "description": "OpenAI GPT-4模型",
  "type": "openai",
  "model": "gpt-4",
  "config": "{\"temperature\": 0.7, \"max_tokens\": 2000}",
  "status": 1
}
```

#### 更新AI代理
```
PUT /api/aiAgent/{id}
Content-Type: application/json

{
  "name": "OpenAI GPT-4",
  "description": "OpenAI GPT-4模型",
  "type": "openai",
  "model": "gpt-4",
  "config": "{\"temperature\": 0.8, \"max_tokens\": 2000}",
  "status": 1
}
```

#### 删除AI代理
```
DELETE /api/aiAgent/{id}
```

#### 测试AI代理连接
```
POST /api/aiAgent/{id}/test
```

### AI聊天接口

#### 发送聊天消息
```
POST /api/aiChat/chat
Content-Type: application/json

{
  "message": "你好，请介绍一下自己",
  "type": "openai"
}
```

#### 流式聊天
```
POST /api/aiChat/stream
Content-Type: application/json

{
  "message": "你好，请介绍一下自己",
  "type": "openai"
}
```

#### 快速聊天
```
GET /api/aiChat/quick?message=你好&type=openai
```

## 配置说明

### 环境变量配置

```bash
# Nacos配置
export NACOS_SERVER_ADDR=8.133.246.113:8848
export NACOS_GROUP=DEFAULT_GROUP
export NACOS_NAMESPACE=d750d92e-152f-4055-a641-3bc9dda85a29

# OpenAI配置
export OPENAI_API_KEY=your-openai-api-key
export OPENAI_BASE_URL=https://api.openai.com

# Azure OpenAI配置
export AZURE_OPENAI_API_KEY=your-azure-api-key
export AZURE_OPENAI_ENDPOINT=your-azure-endpoint
export AZURE_OPENAI_DEPLOYMENT=gpt-35-turbo

# Ollama配置
export OLLAMA_BASE_URL=http://localhost:11434
export OLLAMA_MODEL=llama2
```

### 数据库配置

服务使用MySQL数据库，需要创建`ai_agent`数据库，并执行`sql/ai_agent_tables.sql`脚本创建表结构。

## 部署说明

### 1. 环境准备
- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Nacos 2.x

### 2. 编译打包
```bash
cd aiAgent
mvn clean package -DskipTests
```

### 3. 启动服务
```bash
java -jar target/aiAgent-1.0.0-Final.jar
```

### 4. 验证服务
```bash
# 健康检查
curl http://localhost:8690/api/aiAgent/health

# 查询AI代理列表
curl http://localhost:8690/api/aiAgent/list
```

## 网关集成

服务已集成到网关路由中，通过以下路径访问：

- **AI代理管理**: `/api/aiAgent/**`
- **AI聊天服务**: `/api/aiChat/**`

网关配置已添加相应的断路器、重试、限流等配置。

## 监控与运维

### 健康检查
- 端点: `/api/aiAgent/health`
- 检查内容: 服务状态、数据库连接、AI服务连接

### 日志配置
- 日志文件: `logs/aiAgent-service.log`
- 日志级别: `com.yue.aiAgent: debug`
- 日志格式: 包含时间戳、线程、级别、类名、消息

### 性能指标
- 响应时间监控
- 错误率统计
- 并发量监控

## 故障排除

### 常见问题

1. **AI服务连接失败**
   - 检查API密钥配置
   - 验证网络连接
   - 确认服务地址正确

2. **数据库连接失败**
   - 检查数据库服务状态
   - 验证连接参数
   - 确认数据库权限

3. **服务注册失败**
   - 检查Nacos服务状态
   - 验证网络连接
   - 确认命名空间配置

### 日志分析
```bash
# 查看错误日志
grep "ERROR" logs/aiAgent-service.log

# 查看AI聊天日志
grep "AI聊天" logs/aiAgent-service.log

# 查看服务启动日志
grep "Started AiAgentApplication" logs/aiAgent-service.log
```

## 扩展开发

### 添加新的AI类型
1. 在`AiChatServiceImpl`中添加新的客户端注入
2. 在`getChatClient`方法中添加新的类型判断
3. 更新配置文件和文档

### 自定义聊天模板
1. 修改`PromptTemplate`中的提示词
2. 添加参数化配置
3. 支持多语言提示词

### 集成新的功能
1. 添加新的Controller和Service
2. 更新数据库表结构
3. 添加相应的API文档

## 版本历史

- **v1.0.0**: 初始版本，支持OpenAI、Azure OpenAI、Ollama
- 基础AI代理管理功能
- 统一聊天接口
- 微服务架构集成
