# Chat-Agent Service

AI智能代理服务，支持多种AI模型的聊天功能。

## 功能特性

- 支持 OpenAI GPT 模型
- 支持 Azure OpenAI 服务
- 支持 Ollama 本地模型
- 动态数据源配置
- 分布式事务支持（可选）

## 快速开始

### 1. 环境要求

- Java 17+
- Maven 3.6+
- MySQL 8.0+

### 2. 配置AI服务

#### OpenAI 配置

在 `application-dev.yml` 中配置：

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:your-actual-openai-api-key}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com}
      chat:
        options:
          model: gpt-3.5-turbo
          temperature: 0.7
          max-tokens: 1000
```

环境变量设置：
```bash
export OPENAI_API_KEY="your-actual-openai-api-key"
```

#### Azure OpenAI 配置

```yaml
spring:
  ai:
    azure:
      openai:
        api-key: ${AZURE_OPENAI_API_KEY:your-actual-azure-api-key}
        endpoint: ${AZURE_OPENAI_ENDPOINT:https://your-resource.openai.azure.com}
        chat:
          options:
            deployment-name: ${AZURE_OPENAI_DEPLOYMENT_NAME:gpt-35-turbo}
            temperature: 0.7
            max-tokens: 1000
```

环境变量设置：
```bash
export AZURE_OPENAI_API_KEY="your-actual-azure-api-key"
export AZURE_OPENAI_ENDPOINT="https://your-resource.openai.azure.com"
export AZURE_OPENAI_DEPLOYMENT_NAME="gpt-35-turbo"
```

#### Ollama 配置

```yaml
spring:
  ai:
    ollama:
      base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
      chat:
        options:
          model: ${OLLAMA_MODEL:llama2}
          temperature: 0.7
```

### 3. 数据库配置

确保MySQL服务运行，并创建数据库：

```sql
CREATE DATABASE chat_agent CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 4. 启动服务

```bash
# 开发环境
mvn spring-boot:run -Dspring.profiles.active=dev

# 或者设置环境变量后启动
export SPRING_PROFILES_ACTIVE=dev
mvn spring-boot:run
```

### 5. API 使用

#### 发送聊天消息

```bash
curl -X POST http://localhost:8080/api/aiChat/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "你好，请介绍一下自己",
    "type": "openai"
  }'
```

#### 快速聊天（GET方式）

```bash
curl "http://localhost:8080/api/aiChat/quick?message=你好&type=openai"
```

#### 流式聊天

```bash
curl -X POST http://localhost:8080/api/aiChat/stream \
  -H "Content-Type: application/json" \
  -d '{
    "message": "你好，请介绍一下自己",
    "type": "openai"
  }'
```

## 支持的AI类型

- `openai`: OpenAI GPT 模型
- `azure`: Azure OpenAI 服务
- `ollama`: Ollama 本地模型

## 开发模式

在开发模式下，如果没有配置有效的AI API密钥，服务会返回友好的错误信息，提示用户配置相应的API密钥。

## 故障排除

### 1. Seata 相关错误

如果遇到 Seata 相关错误，检查配置：

```yaml
spring:
  datasource:
    dynamic:
      seata: false  # 禁用Seata
```

### 2. AI 服务不可用

如果AI服务不可用，检查：

1. API密钥是否正确配置
2. 网络连接是否正常
3. 服务端点是否正确

### 3. 数据库连接失败

检查数据库配置：

```yaml
spring:
  datasource:
    dynamic:
      datasource:
        master:
          url: jdbc:mysql://localhost:3306/chat_agent
          username: root
          password: root
```

## 许可证

MIT License
