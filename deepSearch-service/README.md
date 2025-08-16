# 数据计算服务 (Data Calculation Service)

## 概述

数据计算服务是一个基于Spring Boot的微服务，主要负责处理图片上传、识别和数据分析计算。该服务与网关、认证服务、AI代理服务、BGAI服务等协同工作，实现完整的业务流程。

## 核心功能

### 1. 图片上传和识别处理
- 接收多图片上传请求
- 异步发送图片识别请求给BGAI服务
- 存储图片信息和识别结果到MySQL数据库

### 2. AI逻辑判断
- 调用AI代理服务进行业务逻辑分析
- 生成逻辑流程图和计算步骤
- 输出完整的逻辑思路描述

### 3. 数据计算执行
- 将逻辑流程图和文字提交给BGAI服务
- 根据BGAI服务返回的计算规则执行具体计算
- 支持同步和异步计算模式

### 4. 服务间通信
- 使用Dubbo进行服务间RPC通信
- 集成messages-service进行消息发送和消费
- 支持异步任务处理

## 技术架构

### 技术栈
- **Spring Boot 3.2.5**: 主框架
- **Spring Cloud**: 微服务框架
- **Dubbo 3.2.8**: RPC通信框架
- **MyBatis Plus**: 数据访问层
- **MySQL 8.0**: 数据存储
- **Nacos**: 服务发现和配置中心
- **OpenFeign**: HTTP客户端（用于AI代理服务）

### 服务架构
```
网关服务 → 认证服务 → 数据计算服务
                ↓
         AI代理服务 ← → BGAI服务
                ↓
         messages-service
```

## 数据库设计

### 主要表结构

#### 1. 图片上传记录表 (image_upload_record)
- 存储图片上传的基本信息
- 包含业务类型、用户ID、处理状态等

#### 2. 图片文件信息表 (image_file_info)
- 存储图片文件的详细信息
- 包含图片内容、大小、类型等

#### 3. 图片识别结果表 (image_recognition_result)
- 存储图片识别的结果
- 包含识别文本、表格数据、字段信息等

#### 4. SQL语句表 (sql_statement)
- 存储从图片中识别出的SQL语句
- 包含SQL类型、内容、目标表等

#### 5. 数据计算任务表 (calculation_task)
- 存储计算任务的执行状态
- 包含AI响应、逻辑流程图、计算结果等

#### 6. 计算日志表 (calculation_log)
- 存储计算过程的详细日志
- 包含步骤名称、输入输出数据等

## API接口

### 1. 图片上传接口
```
POST /api/calculation/upload-images
```
- 支持多图片上传
- 异步处理图片识别
- 返回请求ID用于状态查询

### 2. 数据计算接口
```
POST /api/calculation/execute
```
- 同步执行数据计算
- 返回完整的计算结果

### 3. 异步计算接口
```
POST /api/calculation/execute-async
```
- 异步执行数据计算
- 返回任务ID用于状态查询

### 4. 状态查询接口
```
GET /api/calculation/status/{requestId}
```
- 查询计算任务状态
- 返回任务执行进度和结果

## 业务流程

### 1. 图片上传和识别流程
```
1. 接收图片上传请求
2. 保存图片信息到数据库
3. 异步发送消息给BGAI服务
4. BGAI服务识别图片内容，生成SQL语句
5. 将识别结果存储到数据库
```

### 2. 数据计算流程
```
1. 接收计算请求
2. 调用AI代理服务进行逻辑分析
3. 生成逻辑流程图和计算步骤
4. 将逻辑信息提交给BGAI服务
5. 根据BGAI服务返回的规则执行计算
6. 返回计算结果
```

## 配置说明

### 环境配置
- `application.yml`: 主配置文件
- `application-dev.yml`: 开发环境配置
- `application-test.yml`: 测试环境配置

### 服务配置
```yaml
service:
  ai-agent:
    base-url: http://localhost:8080
    timeout: 30000
  bgai:
    base-url: http://localhost:8081
    timeout: 30000
```

### Dubbo配置
```yaml
dubbo:
  application:
    name: deepSearch-service
  protocol:
    name: dubbo
    port: 20885
  registry:
    address: nacos://127.0.0.1:8848
```

## 部署说明

### 1. 环境要求
- JDK 17+
- MySQL 8.0+
- Nacos 2.0+
- Redis 6.0+

### 2. 数据库初始化
```sql
-- 执行 schema.sql 创建数据库表结构
mysql -u root -p < src/main/resources/sql/schema.sql
```

### 3. 启动服务
```bash
# 开发环境
mvn spring-boot:run -Dspring.profiles.active=dev

# 生产环境
java -jar deepSearch-service.jar --spring.profiles.active=prod
```

### 4. 健康检查
```
GET /data-calculation/api/calculation/health
```

## 监控和日志

### 日志配置
- 使用SLF4J + Logback
- 支持结构化日志输出
- 可配置日志级别和输出格式

### 性能监控
- 集成Micrometer指标收集
- 支持Prometheus监控
- 提供任务执行时间统计

## 错误处理

### 异常分类
- **业务异常**: 计算逻辑错误、参数验证失败等
- **系统异常**: 服务调用失败、数据库连接异常等
- **网络异常**: 超时、连接中断等

### 重试机制
- 支持配置重试次数和间隔
- 指数退避算法
- 熔断器保护

## 扩展性设计

### 1. 插件化计算引擎
- 支持多种计算算法
- 可插拔的计算规则
- 动态加载计算模块

### 2. 分布式计算支持
- 支持任务分片
- 多节点并行计算
- 负载均衡策略

### 3. 缓存策略
- Redis缓存热点数据
- 本地缓存计算结果
- 多级缓存架构

## 安全考虑

### 1. 认证授权
- 集成网关认证服务
- 支持JWT Token验证
- 基于角色的权限控制

### 2. 数据安全
- 敏感数据加密存储
- 传输数据签名验证
- 访问日志审计

## 故障排查

### 常见问题
1. **服务启动失败**: 检查数据库连接和Nacos配置
2. **图片识别超时**: 检查BGAI服务状态和网络连接
3. **计算任务卡死**: 检查线程池配置和资源使用情况

### 调试工具
- 集成Swagger API文档
- 提供详细的错误堆栈信息
- 支持远程调试

## 版本历史

### v1.0.0 (2024-01-01)
- 初始版本发布
- 支持基本的图片识别和计算功能
- 集成Dubbo和messages-service

## 联系方式

- 作者: jiangyang
- 邮箱: jiangyang@example.com
- 项目地址: https://github.com/jiangyang/deepSearch-service

## 许可证

本项目采用 MIT 许可证，详见 LICENSE 文件。
