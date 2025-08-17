@echo off
echo 启动messages-service服务（带Dubbo缓存禁用属性）...

REM 清理Dubbo缓存
echo 清理Dubbo缓存文件...
if exist "%USERPROFILE%\.dubbo" (
    echo 删除目录: %USERPROFILE%\.dubbo
    rmdir /s /q "%USERPROFILE%\.dubbo"
    echo Dubbo缓存已清理
) else (
    echo Dubbo缓存目录不存在
)

REM 清理临时文件
if exist "%TEMP%\dubbo" (
    echo 删除临时目录: %TEMP%\dubbo
    rmdir /s /q "%TEMP%\dubbo"
)

echo 启动服务...
java -Ddubbo.cache.file.enabled=false ^
     -Ddubbo.metadata.cache.file.enabled=false ^
     -Ddubbo.registry.cache.file.enabled=false ^
     -Ddubbo.service.name.mapping.enabled=false ^
     -Ddubbo.mapping.cache.file.enabled=false ^
     -Ddubbo.metadata.mapping.cache.file.enabled=false ^
     -Ddubbo.registry.use-as-config-center=false ^
     -Ddubbo.registry.use-as-metadata-center=false ^
     -jar target/messages-service-1.0.0-Final.jar

pause
