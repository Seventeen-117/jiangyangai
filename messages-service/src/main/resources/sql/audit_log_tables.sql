-- 审计日志数据库表结构
-- 用于存储分布式事务的完整生命周期和业务处理轨迹

-- 事务审计日志表
CREATE TABLE `transaction_audit_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `global_transaction_id` varchar(128) DEFAULT NULL COMMENT '全局事务ID (Seata XID)',
  `branch_transaction_id` varchar(128) DEFAULT NULL COMMENT '分支事务ID',
  `business_transaction_id` varchar(128) DEFAULT NULL COMMENT '业务事务ID (业务方传入)',
  `service_name` varchar(64) DEFAULT NULL COMMENT '服务名称',
  `operation_type` varchar(32) DEFAULT NULL COMMENT '操作类型 (SEND, CONSUME, COMPENSATE, etc.)',
  `message_type` varchar(16) DEFAULT NULL COMMENT '消息类型 (ROCKETMQ, KAFKA, RABBITMQ)',
  `topic` varchar(128) DEFAULT NULL COMMENT '消息主题',
  `tags` varchar(128) DEFAULT NULL COMMENT '消息标签',
  `message_key` varchar(128) DEFAULT NULL COMMENT '消息键',
  `message_id` varchar(128) DEFAULT NULL COMMENT '消息ID',
  `transaction_status` varchar(16) DEFAULT NULL COMMENT '事务状态 (BEGIN, SUCCESS, FAILED, COMPENSATED)',
  `operation_status` varchar(16) DEFAULT NULL COMMENT '操作状态 (PENDING, PROCESSING, SUCCESS, FAILED)',
  `request_params` text COMMENT '请求参数 (JSON格式)',
  `response_result` text COMMENT '响应结果 (JSON格式)',
  `error_message` text COMMENT '错误信息',
  `retry_count` int(11) DEFAULT '0' COMMENT '重试次数',
  `max_retry_count` int(11) DEFAULT '16' COMMENT '最大重试次数',
  `execution_time` bigint(20) DEFAULT NULL COMMENT '执行耗时 (毫秒)',
  `parent_operation_id` varchar(128) DEFAULT NULL COMMENT '父操作ID (用于构建操作链)',
  `operation_chain_id` varchar(128) DEFAULT NULL COMMENT '操作链ID (用于关联同一业务链的所有操作)',
  `operation_order` int(11) DEFAULT NULL COMMENT '操作顺序 (在同一链中的顺序)',
  `business_context` text COMMENT '业务上下文 (JSON格式，存储业务相关上下文信息)',
  `extended_fields` text COMMENT '扩展字段 (JSON格式)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除标识 (0-未删除, 1-已删除)',
  PRIMARY KEY (`id`),
  KEY `idx_global_transaction_id` (`global_transaction_id`),
  KEY `idx_business_transaction_id` (`business_transaction_id`),
  KEY `idx_operation_chain_id` (`operation_chain_id`),
  KEY `idx_message_id` (`message_id`),
  KEY `idx_service_name` (`service_name`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_transaction_status` (`transaction_status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事务审计日志表';

-- 消息生命周期日志表
CREATE TABLE `message_lifecycle_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `message_id` varchar(128) DEFAULT NULL COMMENT '消息ID (消息中间件生成的唯一ID)',
  `business_message_id` varchar(128) DEFAULT NULL COMMENT '业务消息ID (业务方传入的消息ID)',
  `message_type` varchar(16) DEFAULT NULL COMMENT '消息类型 (ROCKETMQ, KAFKA, RABBITMQ)',
  `topic` varchar(128) DEFAULT NULL COMMENT '消息主题',
  `tags` varchar(128) DEFAULT NULL COMMENT '消息标签',
  `message_key` varchar(128) DEFAULT NULL COMMENT '消息键',
  `queue_name` varchar(128) DEFAULT NULL COMMENT '消息队列',
  `partition_id` int(11) DEFAULT NULL COMMENT '分区ID (Kafka专用)',
  `offset` bigint(20) DEFAULT NULL COMMENT '偏移量 (Kafka专用)',
  `lifecycle_stage` varchar(32) DEFAULT NULL COMMENT '生命周期阶段 (PRODUCE, SEND, STORE, CONSUME, ACK, FAIL)',
  `stage_status` varchar(16) DEFAULT NULL COMMENT '阶段状态 (PENDING, PROCESSING, SUCCESS, FAILED, TIMEOUT)',
  `producer_service` varchar(64) DEFAULT NULL COMMENT '生产者服务名',
  `consumer_service` varchar(64) DEFAULT NULL COMMENT '消费者服务名',
  `consumer_group` varchar(64) DEFAULT NULL COMMENT '消费者组',
  `message_size` bigint(20) DEFAULT NULL COMMENT '消息大小 (字节)',
  `message_content` text COMMENT '消息内容 (可选，大消息可能只存储摘要)',
  `message_digest` varchar(32) DEFAULT NULL COMMENT '消息摘要 (消息内容的MD5值)',
  `message_properties` text COMMENT '消息属性 (JSON格式)',
  `delay_level` int(11) DEFAULT NULL COMMENT '延迟级别 (RocketMQ专用)',
  `delay_time` bigint(20) DEFAULT NULL COMMENT '延迟时间 (毫秒)',
  `retry_count` int(11) DEFAULT '0' COMMENT '重试次数',
  `max_retry_count` int(11) DEFAULT '16' COMMENT '最大重试次数',
  `dead_letter_flag` tinyint(1) DEFAULT '0' COMMENT '死信队列标识 (0-正常, 1-死信)',
  `dead_letter_reason` varchar(512) DEFAULT NULL COMMENT '死信原因',
  `processing_time` bigint(20) DEFAULT NULL COMMENT '处理耗时 (毫秒)',
  `error_message` text COMMENT '错误信息',
  `error_stack_trace` text COMMENT '错误堆栈',
  `business_context` text COMMENT '业务上下文 (JSON格式)',
  `extended_fields` text COMMENT '扩展字段 (JSON格式)',
  `stage_start_time` datetime DEFAULT NULL COMMENT '阶段开始时间',
  `stage_end_time` datetime DEFAULT NULL COMMENT '阶段结束时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除标识 (0-未删除, 1-已删除)',
  PRIMARY KEY (`id`),
  KEY `idx_message_id` (`message_id`),
  KEY `idx_business_message_id` (`business_message_id`),
  KEY `idx_message_type` (`message_type`),
  KEY `idx_topic` (`topic`),
  KEY `idx_lifecycle_stage` (`lifecycle_stage`),
  KEY `idx_stage_status` (`stage_status`),
  KEY `idx_producer_service` (`producer_service`),
  KEY `idx_consumer_service` (`consumer_service`),
  KEY `idx_consumer_group` (`consumer_group`),
  KEY `idx_dead_letter_flag` (`dead_letter_flag`),
  KEY `idx_stage_start_time` (`stage_start_time`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息生命周期日志表';

-- 业务轨迹日志表
CREATE TABLE `business_trace_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `global_transaction_id` varchar(128) DEFAULT NULL COMMENT '全局事务ID (Seata XID)',
  `business_transaction_id` varchar(128) DEFAULT NULL COMMENT '业务事务ID (业务方传入)',
  `trace_id` varchar(128) DEFAULT NULL COMMENT '调用链ID (用于关联同一调用链的所有节点)',
  `parent_span_id` varchar(128) DEFAULT NULL COMMENT '父节点ID (用于构建调用树)',
  `span_id` varchar(128) DEFAULT NULL COMMENT '当前节点ID',
  `service_name` varchar(64) DEFAULT NULL COMMENT '服务名称',
  `service_instance_id` varchar(128) DEFAULT NULL COMMENT '服务实例ID',
  `operation_name` varchar(128) DEFAULT NULL COMMENT '操作名称',
  `operation_type` varchar(32) DEFAULT NULL COMMENT '操作类型 (HTTP, RPC, MQ, DB, etc.)',
  `call_direction` varchar(16) DEFAULT NULL COMMENT '调用方向 (INBOUND, OUTBOUND, INTERNAL)',
  `target_service` varchar(64) DEFAULT NULL COMMENT '目标服务名',
  `target_method` varchar(128) DEFAULT NULL COMMENT '目标方法',
  `request_url` varchar(512) DEFAULT NULL COMMENT '请求URL (HTTP调用)',
  `request_method` varchar(16) DEFAULT NULL COMMENT '请求方法 (GET, POST, etc.)',
  `request_params` text COMMENT '请求参数 (JSON格式)',
  `request_headers` text COMMENT '请求头 (JSON格式)',
  `response_result` text COMMENT '响应结果 (JSON格式)',
  `response_status` int(11) DEFAULT NULL COMMENT '响应状态码',
  `call_status` varchar(16) DEFAULT NULL COMMENT '调用状态 (PENDING, PROCESSING, SUCCESS, FAILED, TIMEOUT)',
  `error_message` text COMMENT '错误信息',
  `error_stack_trace` text COMMENT '错误堆栈',
  `call_duration` bigint(20) DEFAULT NULL COMMENT '调用耗时 (毫秒)',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `business_context` text COMMENT '业务上下文 (JSON格式)',
  `user_id` varchar(64) DEFAULT NULL COMMENT '用户ID',
  `session_id` varchar(128) DEFAULT NULL COMMENT '用户会话ID',
  `client_ip` varchar(64) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(512) DEFAULT NULL COMMENT '用户代理',
  `extended_fields` text COMMENT '扩展字段 (JSON格式)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除标识 (0-未删除, 1-已删除)',
  PRIMARY KEY (`id`),
  KEY `idx_global_transaction_id` (`global_transaction_id`),
  KEY `idx_business_transaction_id` (`business_transaction_id`),
  KEY `idx_trace_id` (`trace_id`),
  KEY `idx_span_id` (`span_id`),
  KEY `idx_parent_span_id` (`parent_span_id`),
  KEY `idx_service_name` (`service_name`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_call_direction` (`call_direction`),
  KEY `idx_target_service` (`target_service`),
  KEY `idx_call_status` (`call_status`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务轨迹日志表';

-- 创建索引优化查询性能
CREATE INDEX `idx_transaction_audit_log_composite` ON `transaction_audit_log` (`global_transaction_id`, `create_time`);
CREATE INDEX `idx_message_lifecycle_log_composite` ON `message_lifecycle_log` (`message_id`, `lifecycle_stage`, `create_time`);
CREATE INDEX `idx_business_trace_log_composite` ON `business_trace_log` (`trace_id`, `span_id`, `create_time`);

-- 创建分区表（可选，用于大数据量场景）
-- ALTER TABLE transaction_audit_log PARTITION BY RANGE (TO_DAYS(create_time)) (
--     PARTITION p202401 VALUES LESS THAN (TO_DAYS('2024-02-01')),
--     PARTITION p202402 VALUES LESS THAN (TO_DAYS('2024-03-01')),
--     PARTITION p202403 VALUES LESS THAN (TO_DAYS('2024-04-01')),
--     PARTITION p_future VALUES LESS THAN MAXVALUE
-- );

-- 创建视图，用于简化查询
CREATE VIEW `v_transaction_summary` AS
SELECT 
    global_transaction_id,
    business_transaction_id,
    service_name,
    operation_type,
    message_type,
    transaction_status,
    operation_status,
    MIN(create_time) as start_time,
    MAX(create_time) as end_time,
    COUNT(*) as operation_count,
    SUM(execution_time) as total_execution_time,
    MAX(CASE WHEN error_message IS NOT NULL THEN 1 ELSE 0 END) as has_error
FROM transaction_audit_log 
WHERE deleted = 0
GROUP BY global_transaction_id, business_transaction_id, service_name, operation_type, message_type, transaction_status, operation_status;

CREATE VIEW `v_message_lifecycle_summary` AS
SELECT 
    message_id,
    business_message_id,
    message_type,
    topic,
    lifecycle_stage,
    stage_status,
    producer_service,
    consumer_service,
    consumer_group,
    MIN(stage_start_time) as first_stage_time,
    MAX(stage_end_time) as last_stage_time,
    COUNT(*) as stage_count,
    SUM(processing_time) as total_processing_time,
    MAX(CASE WHEN error_message IS NOT NULL THEN 1 ELSE 0 END) as has_error
FROM message_lifecycle_log 
WHERE deleted = 0
GROUP BY message_id, business_message_id, message_type, topic, lifecycle_stage, stage_status, producer_service, consumer_service, consumer_group;

CREATE VIEW `v_business_trace_summary` AS
SELECT 
    trace_id,
    global_transaction_id,
    business_transaction_id,
    service_name,
    operation_type,
    call_direction,
    target_service,
    call_status,
    MIN(start_time) as trace_start_time,
    MAX(end_time) as trace_end_time,
    COUNT(*) as span_count,
    SUM(call_duration) as total_duration,
    MAX(CASE WHEN error_message IS NOT NULL THEN 1 ELSE 0 END) as has_error
FROM business_trace_log 
WHERE deleted = 0
GROUP BY trace_id, global_transaction_id, business_transaction_id, service_name, operation_type, call_direction, target_service, call_status;
