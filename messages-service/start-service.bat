@echo off
echo 启动messages-service服务...

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
java -jar target/messages-service-1.0.0-Final.jar

pause
