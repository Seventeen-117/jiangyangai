#!/bin/bash

echo "========================================"
echo "启动AI智能代理服务 (aiAgent-service)"
echo "========================================"

# 设置Java环境变量
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH=$JAVA_HOME/bin:$PATH

# 设置服务端口
export SERVER_PORT=8690

# 设置Nacos配置
export NACOS_SERVER_ADDR=8.133.246.113:8848
export NACOS_GROUP=DEFAULT_GROUP
export NACOS_NAMESPACE=d750d92e-152f-4055-a641-3bc9dda85a29

# 设置AI服务配置
export OPENAI_API_KEY=your-openai-api-key
export OPENAI_BASE_URL=https://api.openai.com
export AZURE_OPENAI_API_KEY=your-azure-api-key
export AZURE_OPENAI_ENDPOINT=your-azure-endpoint
export AZURE_OPENAI_DEPLOYMENT=gpt-35-turbo
export OLLAMA_BASE_URL=http://localhost:11434
export OLLAMA_MODEL=llama2

echo "当前配置:"
echo "服务端口: $SERVER_PORT"
echo "Nacos地址: $NACOS_SERVER_ADDR"
echo "Nacos分组: $NACOS_GROUP"
echo "Nacos命名空间: $NACOS_NAMESPACE"
echo

# 检查Java版本
if ! command -v java &> /dev/null; then
    echo "错误: 未找到Java环境，请检查JAVA_HOME配置"
    exit 1
fi

java -version
if [ $? -ne 0 ]; then
    echo "错误: Java环境检查失败"
    exit 1
fi

# 检查Maven
if ! command -v mvn &> /dev/null; then
    echo "错误: 未找到Maven环境，请检查Maven配置"
    exit 1
fi

mvn -version
if [ $? -ne 0 ]; then
    echo "错误: Maven环境检查失败"
    exit 1
fi

echo
echo "开始编译项目..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "错误: 项目编译失败"
    exit 1
fi

echo
echo "项目编译成功，开始启动服务..."
echo "服务将在 http://localhost:$SERVER_PORT 启动"
echo "健康检查: http://localhost:$SERVER_PORT/api/aiAgent/health"
echo

# 启动服务
java -jar target/aiAgent-1.0.0-Final.jar
