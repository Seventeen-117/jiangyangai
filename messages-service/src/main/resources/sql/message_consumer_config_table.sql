-- 消息消费配置表
CREATE TABLE `message_consumer_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `service_name` varchar(100) NOT NULL COMMENT '服务名称',
  `instance_id` varchar(100) DEFAULT NULL COMMENT '服务实例ID',
  `message_queue_type` varchar(20) NOT NULL COMMENT '消息中间件类型：ROCKETMQ、KAFKA、RABBITMQ',
  `consume_mode` varchar(20) NOT NULL COMMENT '消费模式：PUSH（推模式）、PULL（拉模式）',
  `consume_type` varchar(20) NOT NULL COMMENT '消费类型：CLUSTERING（集群消费）、BROADCASTING（广播消费）',
  `consume_order` varchar(20) NOT NULL COMMENT '顺序性：CONCURRENT（并发消费）、ORDERLY（顺序消费）',
  `topic` varchar(200) NOT NULL COMMENT '主题/队列名称',
  `tag` varchar(100) DEFAULT NULL COMMENT '标签（RocketMQ专用）',
  `consumer_group` varchar(100) NOT NULL COMMENT '消费组名称',
  `partition_key` varchar(100) DEFAULT NULL COMMENT '分区键（Kafka专用）',
  `exchange` varchar(100) DEFAULT NULL COMMENT '交换机名称（RabbitMQ专用）',
  `routing_key` varchar(100) DEFAULT NULL COMMENT '路由键（RabbitMQ专用）',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `consume_interval` bigint(20) DEFAULT '1000' COMMENT '消费间隔（毫秒，拉模式专用）',
  `batch_size` int(11) DEFAULT '1' COMMENT '批量消费大小',
  `max_retry_times` int(11) DEFAULT '3' COMMENT '最大重试次数',
  `timeout` bigint(20) DEFAULT '30000' COMMENT '超时时间（毫秒）',
  `description` varchar(500) DEFAULT NULL COMMENT '配置描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(100) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(100) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_service_topic_group` (`service_name`, `topic`, `consumer_group`),
  KEY `idx_service_name` (`service_name`),
  KEY `idx_message_queue_type` (`message_queue_type`),
  KEY `idx_enabled` (`enabled`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息消费配置表';

-- 插入示例数据
INSERT INTO `message_consumer_config` (
  `service_name`, `instance_id`, `message_queue_type`, `consume_mode`, `consume_type`, `consume_order`,
  `topic`, `tag`, `consumer_group`, `exchange`, `routing_key`, `enabled`, `description`
) VALUES 
-- RocketMQ 推模式 + 集群消费 + 并发消费
('order-service', 'order-001', 'ROCKETMQ', 'PUSH', 'CLUSTERING', 'CONCURRENT',
 'ORDER_TOPIC', 'ORDER_CREATE', 'order-consumer-group', NULL, NULL, 1, '订单服务消费订单创建消息'),

-- RocketMQ 推模式 + 集群消费 + 顺序消费
('payment-service', 'payment-001', 'ROCKETMQ', 'PUSH', 'CLUSTERING', 'ORDERLY',
 'PAYMENT_TOPIC', 'PAYMENT_SUCCESS', 'payment-consumer-group', NULL, NULL, 1, '支付服务消费支付成功消息，保证顺序'),

-- RocketMQ 推模式 + 广播消费 + 并发消费
('notification-service', 'notification-001', 'ROCKETMQ', 'PUSH', 'BROADCASTING', 'CONCURRENT',
 'SYSTEM_TOPIC', 'SYSTEM_MAINTENANCE', 'notification-consumer-group', NULL, NULL, 1, '通知服务消费系统维护消息，广播模式'),

-- Kafka 拉模式 + 集群消费 + 并发消费
('log-service', 'log-001', 'KAFKA', 'PULL', 'CLUSTERING', 'CONCURRENT',
 'LOG_TOPIC', NULL, 'log-consumer-group', NULL, NULL, 1, '日志服务消费日志消息，拉模式'),

-- RabbitMQ 推模式 + 集群消费 + 并发消费
('inventory-service', 'inventory-001', 'RABBITMQ', 'PUSH', 'CLUSTERING', 'CONCURRENT',
 'inventory_queue', NULL, 'inventory-consumer-group', 'inventory_exchange', 'inventory.update', 1, '库存服务消费库存更新消息');
INSERT INTO message_consumer_config (
    service_name,
    instance_id,
    message_queue_type,
    consume_mode,
    consume_type,
    consume_order,
    topic,
    tag,
    consumer_group,
    enabled,
    batch_size,
    max_retry_times,
    timeout,
    description,
    create_time,
    update_time,
    create_by,
    update_by
) VALUES (
             'bgai-service',                    -- 服务名称
             'bgai-instance-001',              -- 服务实例ID
             'ROCKETMQ',                       -- 消息中间件类型
             'PUSH',                           -- 消费模式：推模式
             'CLUSTERING',                     -- 消费类型：集群消费
             'CONCURRENT',                     -- 顺序性：并发消费
             'bgai-events',                    -- 主题名称
             'user-action',                    -- 标签
             'bgai-consumer-group',            -- 消费组名称
             true,                             -- 是否启用
             10,                               -- 批量消费大小
             3,                                -- 最大重试次数
             5000,                             -- 超时时间（毫秒）
             'bgai-service用户行为事件消费配置', -- 配置描述
             NOW(),                            -- 创建时间
             NOW(),                            -- 更新时间
             'system',                         -- 创建人
             'system'                          -- 更新人
         );