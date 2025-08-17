# Elasticsearch索引映射修复指南

## 问题描述
当前ES中的 `messages` 和 `message_history` 索引将 `createTime` 和 `updateTime` 字段映射为 `date` 类型，但我们的代码现在发送的是字符串格式，导致 `document_parsing_exception` 错误。

## 解决方案

### 方案一：手动删除并重建索引（推荐）

1. **删除现有索引**
   ```bash
   # 删除messages索引
   curl -X DELETE "http://8.133.246.113:9200/messages"
   
   # 删除message_history索引
   curl -X DELETE "http://8.133.246.113:9200/message_history"
   ```

2. **重启messages-service**
   服务启动时会自动创建新的索引，使用正确的字段映射。

### 方案二：使用ES API更新映射

如果不想删除数据，可以更新现有索引的映射：

```bash
# 更新messages索引的createTime和updateTime字段映射
curl -X PUT "http://8.133.246.113:9200/messages/_mapping" -H "Content-Type: application/json" -d '{
  "properties": {
    "createTime": {
      "type": "keyword"
    },
    "updateTime": {
      "type": "keyword"
    }
  }
}'

# 更新message_history索引的createTime字段映射
curl -X PUT "http://8.133.246.113:9200/message_history/_mapping" -H "Content-Type: application/json" -d '{
  "properties": {
    "createTime": {
      "type": "keyword"
    }
  }
}'
```

### 方案三：使用Docker命令删除索引

如果ES运行在Docker中：

```bash
# 进入ES容器
docker exec -it elasticsearch bash

# 删除索引
curl -X DELETE "localhost:9200/messages"
curl -X DELETE "localhost:9200/message_history"
```

## 验证修复

修复后，可以通过以下命令验证索引映射：

```bash
# 查看messages索引映射
curl -X GET "http://8.133.246.113:9200/messages/_mapping"

# 查看message_history索引映射
curl -X GET "http://8.133.246.113:9200/message_history/_mapping"
```

正确的映射应该是：
- `createTime`: `"type": "keyword"`
- `updateTime`: `"type": "keyword"`

## 注意事项

1. 删除索引会丢失所有现有数据
2. 如果数据重要，请先备份
3. 建议在测试环境中先验证修复效果
4. 生产环境建议在维护窗口期间执行
