#!/bin/bash

echo "========================================"
echo "Messages Service 启动脚本（使用 JVM 参数）"
echo "========================================"

echo ""
echo "步骤 1: 清理 Dubbo 缓存..."
./clean-dubbo-cache.sh

echo ""
echo "步骤 2: 启动 Messages Service（使用 JVM 参数）..."
echo "正在启动应用，请稍候..."

# 读取 JVM 参数文件并启动应用
jvm_args=$(cat dubbo-jvm-options.txt | tr '\n' ' ')

# 启动应用
mvn spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.jvmArguments="$jvm_args"

echo ""
echo "应用启动完成！"
