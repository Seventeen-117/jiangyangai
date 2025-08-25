@echo off
REM messages-service 启动脚本
REM 添加Dubbo缓存控制系统属性，避免缓存冲突

echo 启动 messages-service 服务...

REM 设置Dubbo缓存控制系统属性
set DUBBO_CACHE_FILE=false
set DUBBO_METADATA_CACHE_FILE=false
set DUBBO_REGISTRY_CACHE_FILE=false
set DUBBO_CONSUMER_CACHE_FILE=false
set DUBBO_PROVIDER_CACHE_FILE=false
set DUBBO_CACHE_TYPE=memory
set DUBBO_CACHE_DIRECTORY=%USERPROFILE%\.dubbo\messages-service

REM 设置Dubbo元数据类型
set DUBBO_METADATA_TYPE=local

REM 设置Dubbo应用环境
set DUBBO_APPLICATION_ENVIRONMENT=develop

REM 启动服务
mvn spring-boot:run -Dspring-boot.run.profiles=dev ^
    -Ddubbo.cache.file=false ^
    -Ddubbo.metadata.cache.file=false ^
    -Ddubbo.registry.cache.file=false ^
    -Ddubbo.consumer.cache.file=false ^
    -Ddubbo.provider.cache.file=false ^
    -Ddubbo.cache.type=memory ^
    -Ddubbo.cache.directory=%USERPROFILE%\.dubbo\messages-service ^
    -Ddubbo.metadata.type=local ^
    -Ddubbo.application.environment=develop

pause
