# Seata 服务器配置修复

## 问题分析
Seata 服务器在 `8.133.246.113:8091` 上运行，但客户端无法连接，报错 `no available service endpoint found in cluster 'default'`。

## 根本原因
Seata 服务器没有正确配置集群名称，导致无法在 Nacos 中正确注册。

## 解决方案

### 1. 修改 Seata 服务器配置
在 Seata 服务器的 `conf/application.yml` 或 `conf/registry.conf` 中添加：

```yaml
seata:
  config:
    type: nacos
    nacos:
      server-addr: 8.133.246.113:8848
      namespace: d750d92e-152f-4055-a641-3bc9dda85a29
      group: DEFAULT_GROUP
      data-id: seataServer.properties
      username: ""
      password: ""
  
  registry:
    type: nacos
    nacos:
      application: seata-server
      server-addr: 8.133.246.113:8848
      namespace: d750d92e-152f-4055-a641-3bc9dda85a29
      group: DEFAULT_GROUP
      cluster: default  # 关键：设置集群名称为 default
      username: ""
      password: ""
```

### 2. 确保 seataServer.properties 配置正确
在 Nacos 中确保 `seataServer.properties` 包含：

```properties
# 事务组映射关系
service.vgroupMapping.bgai-service-tx-group=default
service.vgroupMapping.messages-service-tx-group=default
service.vgroupMapping.gateway-service-tx-group=default

# 事务组与集群的映射关系
service.default.grouplist=8.133.246.113:8091

# 存储模式
store.mode=db
store.db.datasource=druid
store.db.dbType=mysql
store.db.driverClassName=com.mysql.cj.jdbc.Driver
store.db.url=jdbc:mysql://8.133.246.113:3306/seata?useUnicode=true&characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true&useSSL=false&serverTimezone=GMT%2B8
store.db.user=bgtech
store.db.password=Zly689258..
store.db.minConn=5
store.db.maxConn=100
```

### 3. 重启 Seata 服务器
修改配置后，重启 Seata 服务器：

```bash
# 在 Seata 服务器上执行
docker restart seata
# 或者
systemctl restart seata
```

### 4. 验证配置
重启后检查：
- Nacos 中的服务注册情况
- 集群名称是否正确设置为 `default`
- 客户端连接是否正常

## 临时解决方案
如果暂时无法修改 Seata 服务器配置，可以在客户端配置中禁用 Seata 功能，避免连接错误。
