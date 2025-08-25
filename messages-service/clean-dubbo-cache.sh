#!/bin/bash

echo "清理 Dubbo 缓存文件..."

# 清理用户主目录下的 Dubbo 缓存
echo "清理用户主目录下的 Dubbo 缓存..."
if [ -d "$HOME/.dubbo" ]; then
    echo "删除目录: $HOME/.dubbo"
    rm -rf "$HOME/.dubbo"
else
    echo "用户主目录下没有 Dubbo 缓存"
fi

# 清理临时目录下的 Dubbo 缓存
echo "清理临时目录下的 Dubbo 缓存..."
if [ -d "/tmp/dubbo" ]; then
    echo "删除目录: /tmp/dubbo"
    rm -rf "/tmp/dubbo"
else
    echo "临时目录下没有 Dubbo 缓存"
fi

# 清理可能存在的其他 Dubbo 缓存目录
echo "清理其他可能的 Dubbo 缓存目录..."
if [ -d "/var/tmp/dubbo" ]; then
    echo "删除目录: /var/tmp/dubbo"
    rm -rf "/var/tmp/dubbo"
fi

if [ -d "/opt/dubbo" ]; then
    echo "删除目录: /opt/dubbo"
    rm -rf "/opt/dubbo"
fi

echo "Dubbo 缓存清理完成！"
