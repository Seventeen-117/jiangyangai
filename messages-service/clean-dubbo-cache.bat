@echo off
echo 清理 Dubbo 缓存文件...

REM 清理用户主目录下的 Dubbo 缓存
echo 清理用户主目录下的 Dubbo 缓存...
if exist "%USERPROFILE%\.dubbo" (
    echo 删除目录: %USERPROFILE%\.dubbo
    rmdir /s /q "%USERPROFILE%\.dubbo"
) else (
    echo 用户主目录下没有 Dubbo 缓存
)

REM 清理临时目录下的 Dubbo 缓存
echo 清理临时目录下的 Dubbo 缓存...
if exist "%TEMP%\dubbo" (
    echo 删除目录: %TEMP%\dubbo
    rmdir /s /q "%TEMP%\dubbo"
) else (
    echo 临时目录下没有 Dubbo 缓存
)

REM 清理可能存在的其他 Dubbo 缓存目录
echo 清理其他可能的 Dubbo 缓存目录...
if exist "C:\dubbo" (
    echo 删除目录: C:\dubbo
    rmdir /s /q "C:\dubbo"
)

if exist "C:\temp\dubbo" (
    echo 删除目录: C:\temp\dubbo
    rmdir /s /q "C:\temp\dubbo"
)

echo Dubbo 缓存清理完成！
pause
