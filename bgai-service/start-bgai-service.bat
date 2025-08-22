@echo off
echo Starting bgai-service with Seata and Nacos configuration...

set JAVA_OPTS=-Ddubbo.cache.file.enabled=false -Ddubbo.metadata.cache.file.enabled=false -Ddubbo.cache.file.directory=%TEMP%\dubbo\bgai-service

cd /d "%~dp0"

if exist "target\bgai-service-1.0.0.jar" (
    echo Found JAR file, starting bgai-service...
    java %JAVA_OPTS% -jar target\bgai-service-1.0.0.jar --spring.profiles.active=dev
) else (
    echo JAR file not found, trying to build first...
    mvn clean package -DskipTests
    if exist "target\bgai-service-1.0.0.jar" (
        echo Build successful, starting bgai-service...
        java %JAVA_OPTS% -jar target\bgai-service-1.0.0.jar --spring.profiles.active=dev
    ) else (
        echo Build failed, please check the Maven build output.
        pause
    )
)

pause
