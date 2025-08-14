@echo off
echo ========================================
echo 启动AI智能代理服务 (aiAgent-service)
echo ========================================

REM 设置Java环境变量
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%

REM 设置服务端口
set SERVER_PORT=8690

REM 设置Nacos配置
set NACOS_SERVER_ADDR=8.133.246.113:8848
set NACOS_GROUP=DEFAULT_GROUP
set NACOS_NAMESPACE=d750d92e-152f-4055-a641-3bc9dda85a29

REM 设置AI服务配置
set OPENAI_API_KEY=your-openai-api-key
set OPENAI_BASE_URL=https://api.openai.com
set AZURE_OPENAI_API_KEY=your-azure-api-key
set AZURE_OPENAI_ENDPOINT=your-azure-endpoint
set AZURE_OPENAI_DEPLOYMENT=gpt-35-turbo
set OLLAMA_BASE_URL=http://localhost:11434
set OLLAMA_MODEL=llama2

echo 当前配置:
echo 服务端口: %SERVER_PORT%
echo Nacos地址: %NACOS_SERVER_ADDR%
echo Nacos分组: %NACOS_GROUP%
echo Nacos命名空间: %NACOS_NAMESPACE%
echo.

REM 检查Java版本
java -version
if %errorlevel% neq 0 (
    echo 错误: 未找到Java环境，请检查JAVA_HOME配置
    pause
    exit /b 1
)

REM 检查Maven
mvn -version
if %errorlevel% neq 0 (
    echo 错误: 未找到Maven环境，请检查Maven配置
    pause
    exit /b 1
)

echo.
echo 开始编译项目...
mvn clean package -DskipTests

if %errorlevel% neq 0 (
    echo 错误: 项目编译失败
    pause
    exit /b 1
)

echo.
echo 项目编译成功，开始启动服务...
echo 服务将在 http://localhost:%SERVER_PORT% 启动
echo 健康检查: http://localhost:%SERVER_PORT%/api/aiAgent/health
echo.

REM 启动服务
java -jar target/aiAgent-1.0.0-Final.jar

pause
