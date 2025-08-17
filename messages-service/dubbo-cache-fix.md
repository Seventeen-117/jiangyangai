# Dubbo缓存冲突解决方案

## 问题描述
当多个Dubbo服务实例在同一台机器上运行时，可能会出现文件锁冲突错误：
```
org.apache.dubbo.common.cache.FileCacheStoreFactory$PathNotExclusiveException: 
C:\Users\Administrator\.dubbo\.metadata.bgai-service.nacos.8.133.246.113%003a8848.dubbo.cache is not exclusive. 
Maybe multiple Dubbo instances are using the same folder.
```

## 解决方案

### 方案一：自动清理缓存（推荐）
服务启动时会自动清理冲突的Dubbo缓存文件。

### 方案二：手动清理缓存
如果自动清理失败，可以手动执行以下步骤：

1. **停止所有相关服务**
   ```bash
   # 停止messages-service
   # 停止bgai-service
   # 停止其他使用Dubbo的服务
   ```

2. **清理Dubbo缓存目录**
   ```bash
   # Windows
   rmdir /s /q "%USERPROFILE%\.dubbo"
   
   # Linux/Mac
   rm -rf ~/.dubbo
   ```

3. **重新启动服务**
   ```bash
   # 启动messages-service
   java -jar target/messages-service-1.0.0-Final.jar
   ```

### 方案三：使用批处理脚本
运行提供的批处理脚本：
```bash
# Windows - 清理缓存
clean-dubbo-cache.bat

# Windows - 启动服务（自动清理缓存）
start-service.bat

# Windows - 启动服务（带JVM参数禁用缓存）
start-service-with-props.bat
```

## 配置说明

### 已应用的配置修改

1. **禁用文件缓存**
   ```yaml
   dubbo:
     application:
       cache-file: false
     registry:
       cache-file: false
     consumer:
       cache-file: false
     provider:
       cache-file: false
   ```

2. **使用本地元数据**
   ```yaml
   dubbo:
     application:
       metadata-type: local
     consumer:
       metadata-type: local
     provider:
       metadata-type: local
   ```

3. **配置环境标识**
   ```yaml
   dubbo:
     application:
       environment: develop  # 支持的值: develop, test, product
   ```

## 预防措施

1. **避免同时运行多个相同服务实例**
2. **使用不同的应用名称或实例ID**
3. **定期清理Dubbo缓存目录**
4. **使用容器化部署隔离环境**

## 验证修复

启动服务后，检查日志中是否有以下信息：
```
Dubbo缓存清理完成
```

如果没有出现文件锁冲突错误，说明修复成功。

## 注意事项

1. 清理缓存会丢失服务发现信息，需要重新注册
2. 生产环境建议使用容器化部署避免冲突
3. 如果问题持续存在，考虑使用不同的用户账户运行服务
