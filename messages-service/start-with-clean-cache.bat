@echo off
echo ========================================
echo Messages Service 启动脚本（带缓存清理）
echo ========================================

echo.
echo 步骤 1: 清理 Dubbo 缓存...
call clean-dubbo-cache.bat

echo.
echo 步骤 2: 启动 Messages Service...
echo 正在启动应用，请稍候...

REM 启动应用
mvn spring-boot:run -Dspring-boot.run.profiles=dev

echo.
echo 应用启动完成！
pause
