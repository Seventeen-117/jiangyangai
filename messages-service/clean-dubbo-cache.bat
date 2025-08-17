@echo off
echo 清理Dubbo缓存文件...

REM 删除Dubbo缓存目录
if exist "%USERPROFILE%\.dubbo" (
    echo 删除目录: %USERPROFILE%\.dubbo
    rmdir /s /q "%USERPROFILE%\.dubbo"
    echo Dubbo缓存已清理
) else (
    echo Dubbo缓存目录不存在
)

REM 删除临时文件
if exist "%TEMP%\dubbo" (
    echo 删除临时目录: %TEMP%\dubbo
    rmdir /s /q "%TEMP%\dubbo"
)

echo 清理完成！
pause
