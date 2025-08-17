# Dubbo缓存冲突解决方案总结

## 问题描述
当多个Dubbo服务实例在同一台机器上运行时，会出现文件锁冲突错误：
```
org.apache.dubbo.common.cache.FileCacheStoreFactory$PathNotExclusiveException: 
C:\Users\Administrator\.dubbo\.mapping.bgai-service.dubbo.cache is not exclusive. 
Maybe multiple Dubbo instances are using the same folder.
```

## 已实施的解决方案

### 1. 配置文件修改
在 `application-dev.yml` 中添加了以下配置：

```yaml
dubbo:
  application:
    name: messages-service
    qos-enable: false
    metadata-type: local
    cache-file: false
    environment: develop
  protocol:
    name: dubbo
    port: -1
    cache-file: false
  registry:
    address: nacos://8.133.246.113:8848
    timeout: 10000
    cache-file: false
    file: false
    use-as-config-center: false
    use-as-metadata-center: false
  consumer:
    timeout: 5000
    retries: 2
    check: false
    cache-file: false
    metadata-type: local
  provider:
    timeout: 5000
    retries: 2
    cache-file: false
    metadata-type: local
  config-center:
    timeout: 3000
    file: false
  metadata-report:
    timeout: 3000
    file: false
  cache:
    file:
      enabled: false
  service-name-mapping:
    enabled: false
    cache:
      file:
        enabled: false
```

### 2. Java代码修改
创建了 `DubboCacheConfig.java` 类：

- 在静态代码块中设置系统属性
- 在启动时清理冲突的缓存文件
- 禁用所有Dubbo文件缓存机制

### 3. 启动脚本
创建了多个启动脚本：

- `clean-dubbo-cache.bat` - 清理Dubbo缓存
- `start-service.bat` - 启动服务（自动清理缓存）
- `start-service-with-props.bat` - 启动服务（带JVM参数）

### 4. JVM启动参数
在启动脚本中设置了以下JVM参数：

```bash
java -Ddubbo.cache.file.enabled=false \
     -Ddubbo.metadata.cache.file.enabled=false \
     -Ddubbo.registry.cache.file.enabled=false \
     -Ddubbo.service.name.mapping.enabled=false \
     -Ddubbo.mapping.cache.file.enabled=false \
     -Ddubbo.metadata.mapping.cache.file.enabled=false \
     -Ddubbo.registry.use-as-config-center=false \
     -Ddubbo.registry.use-as-metadata-center=false \
     -jar target/messages-service-1.0.0-Final.jar
```

## 使用方法

### 推荐启动方式
使用带JVM参数的启动脚本：
```bash
start-service-with-props.bat
```

### 手动清理缓存
如果仍有问题，可以手动清理：
```bash
# Windows
rmdir /s /q "%USERPROFILE%\.dubbo"

# Linux/Mac
rm -rf ~/.dubbo
```

## 验证修复
启动服务后，检查日志中是否有以下信息：
```
已设置Dubbo文件缓存禁用属性
Dubbo缓存清理完成
```

如果没有出现文件锁冲突错误，说明修复成功。

## 注意事项

1. **环境配置**：确保使用正确的环境值（develop/test/product）
2. **服务隔离**：避免同时运行多个相同服务实例
3. **缓存清理**：定期清理Dubbo缓存目录
4. **生产环境**：建议使用容器化部署避免冲突

## 故障排除

如果问题仍然存在：

1. 检查是否有其他服务正在使用相同的缓存目录
2. 确保所有相关服务都已停止
3. 手动删除 `.dubbo` 目录
4. 使用不同的用户账户运行服务
5. 考虑使用容器化部署

## 相关文件

- `application-dev.yml` - 开发环境配置
- `application-test.yml` - 测试环境配置  
- `application-prod.yml` - 生产环境配置
- `DubboCacheConfig.java` - Dubbo缓存配置类
- `start-service-with-props.bat` - 推荐启动脚本
- `dubbo-cache-fix.md` - 详细解决方案文档
