@echo off
echo ========================================
echo Messages Service 启动脚本（使用 JVM 参数）
echo ========================================

echo.
echo 步骤 1: 清理 Dubbo 缓存...
call clean-dubbo-cache.bat

echo.
echo 步骤 2: 启动 Messages Service（使用 JVM 参数）...
echo 正在启动应用，请稍候...

REM 读取 JVM 参数文件并启动应用
for /f "tokens=*" %%i in (dubbo-jvm-options.txt) do (
    set "jvm_args=!jvm_args! %%i"
)

REM 启动应用
mvn spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.jvmArguments="%jvm_args%"

echo.
echo 应用启动完成！
pause
