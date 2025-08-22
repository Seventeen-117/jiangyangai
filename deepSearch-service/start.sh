#!/bin/bash

echo "Starting DeepSearch Service with Dubbo cache configuration..."

# 设置Dubbo缓存配置
export DUBBO_CACHE_PATH="$HOME/.dubbo/deepSearch-service"
export DUBBO_APP_NAME="deepSearch-service"

# 设置JVM参数
export JAVA_OPTS="-Ddubbo.cache.file.path=$DUBBO_CACHE_PATH \
-Ddubbo.metadata.cache.file.path=$DUBBO_CACHE_PATH/metadata \
-Ddubbo.service.discovery.cache.file.path=$DUBBO_CACHE_PATH/discovery \
-Ddubbo.config.cache.file.path=$DUBBO_CACHE_PATH/config \
-Ddubbo.registry.cache.file.path=$DUBBO_CACHE_PATH/registry \
-Ddubbo.mapping.cache.file.path=$DUBBO_CACHE_PATH/mapping \
-Ddubbo.application.cache.file.path=$DUBBO_CACHE_PATH/application \
-Ddubbo.service.name.mapping.cache.file.path=$DUBBO_CACHE_PATH/service-mapping \
-Ddubbo.metadata.service.name.mapping.cache.file.path=$DUBBO_CACHE_PATH/metadata-service-mapping \
-Ddubbo.cache.store.type=memory \
-Ddubbo.metadata.cache.store.type=memory \
-Ddubbo.service.discovery.cache.store.type=memory \
-Ddubbo.application.name=$DUBBO_APP_NAME \
-Ddubbo.application.qos-enable=false \
-Ddubbo.application.qos-port=22222 \
-Ddubbo.application.qos-accept-foreign-ip=false"

echo "Dubbo cache path: $DUBBO_CACHE_PATH"
echo "Dubbo app name: $DUBBO_APP_NAME"
echo "JVM options: $JAVA_OPTS"

# 启动应用
mvn spring-boot:run -Dspring-boot.run.jvmArguments="$JAVA_OPTS"
