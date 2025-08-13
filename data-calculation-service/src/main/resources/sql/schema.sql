-- 数据计算服务数据库表结构
-- 创建数据库
CREATE DATABASE IF NOT EXISTS `data_calculation_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `data_calculation_db`;

-- 图片上传记录表
CREATE TABLE `image_upload_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `request_id` varchar(64) NOT NULL COMMENT '请求ID',
  `business_type` varchar(50) NOT NULL COMMENT '业务类型',
  `user_id` varchar(64) NOT NULL COMMENT '用户ID',
  `source` varchar(50) DEFAULT NULL COMMENT '请求来源',
  `text_info` text COMMENT '文字信息',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态：PENDING-待处理，PROCESSING-处理中，COMPLETED-已完成，FAILED-失败',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `extra_params` json DEFAULT NULL COMMENT '扩展参数',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_request_id` (`request_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_business_type` (`business_type`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图片上传记录表';

-- 图片文件信息表
CREATE TABLE `image_file_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `request_id` varchar(64) NOT NULL COMMENT '请求ID',
  `image_id` varchar(64) NOT NULL COMMENT '图片ID',
  `image_name` varchar(255) NOT NULL COMMENT '图片名称',
  `image_type` varchar(50) DEFAULT NULL COMMENT '图片类型',
  `image_size` bigint(20) DEFAULT NULL COMMENT '图片大小（字节）',
  `image_url` varchar(500) DEFAULT NULL COMMENT '图片URL',
  `image_content` longtext COMMENT '图片内容（Base64编码）',
  `description` varchar(500) DEFAULT NULL COMMENT '图片描述',
  `order_num` int(11) DEFAULT NULL COMMENT '图片顺序',
  `status` varchar(20) NOT NULL DEFAULT 'UPLOADED' COMMENT '状态：UPLOADED-已上传，PROCESSING-处理中，RECOGNIZED-已识别，FAILED-失败',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_image_id` (`image_id`),
  KEY `idx_request_id` (`request_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图片文件信息表';

-- 图片识别结果表
CREATE TABLE `image_recognition_result` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `request_id` varchar(64) NOT NULL COMMENT '请求ID',
  `image_id` varchar(64) NOT NULL COMMENT '图片ID',
  `recognition_status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '识别状态：PENDING-待识别，PROCESSING-识别中，COMPLETED-已完成，FAILED-失败',
  `recognized_text` longtext COMMENT '识别出的文本内容',
  `table_data` json DEFAULT NULL COMMENT '识别出的表格数据',
  `confidence` decimal(5,4) DEFAULT NULL COMMENT '置信度',
  `error_message` text COMMENT '错误信息',
  `recognition_start_time` datetime DEFAULT NULL COMMENT '识别开始时间',
  `recognition_end_time` datetime DEFAULT NULL COMMENT '识别完成时间',
  `recognition_duration` bigint(20) DEFAULT NULL COMMENT '识别耗时（毫秒）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_request_image` (`request_id`, `image_id`),
  KEY `idx_request_id` (`request_id`),
  KEY `idx_image_id` (`image_id`),
  KEY `idx_recognition_status` (`recognition_status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图片识别结果表';

-- 识别字段信息表
CREATE TABLE `recognition_field_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `recognition_result_id` bigint(20) NOT NULL COMMENT '识别结果ID',
  `field_name` varchar(100) NOT NULL COMMENT '字段名称',
  `field_value` text COMMENT '字段值',
  `field_type` varchar(50) DEFAULT NULL COMMENT '字段类型',
  `description` varchar(500) DEFAULT NULL COMMENT '字段描述',
  `confidence` decimal(5,4) DEFAULT NULL COMMENT '置信度',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_recognition_result_id` (`recognition_result_id`),
  KEY `idx_field_name` (`field_name`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='识别字段信息表';

-- 图表信息表
CREATE TABLE `chart_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `recognition_result_id` bigint(20) NOT NULL COMMENT '识别结果ID',
  `chart_type` varchar(50) DEFAULT NULL COMMENT '图表类型',
  `title` varchar(255) DEFAULT NULL COMMENT '图表标题',
  `chart_data` json DEFAULT NULL COMMENT '图表数据',
  `axis_info` json DEFAULT NULL COMMENT '坐标轴信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_recognition_result_id` (`recognition_result_id`),
  KEY `idx_chart_type` (`chart_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图表信息表';

-- SQL语句表
CREATE TABLE `sql_statement` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `request_id` varchar(64) NOT NULL COMMENT '请求ID',
  `sql_id` varchar(64) NOT NULL COMMENT 'SQL ID',
  `sql_type` varchar(20) NOT NULL COMMENT 'SQL类型：SELECT, INSERT, UPDATE, DELETE等',
  `sql_content` text NOT NULL COMMENT 'SQL语句内容',
  `description` varchar(500) DEFAULT NULL COMMENT 'SQL描述',
  `target_table` varchar(100) DEFAULT NULL COMMENT '目标表名',
  `related_fields` json DEFAULT NULL COMMENT '相关字段',
  `priority` int(11) DEFAULT 0 COMMENT '执行优先级',
  `validated` tinyint(1) DEFAULT 0 COMMENT '是否已验证：0-未验证，1-已验证',
  `execution_status` varchar(20) DEFAULT 'PENDING' COMMENT '执行状态：PENDING-待执行，EXECUTING-执行中，COMPLETED-已完成，FAILED-失败',
  `execution_result` text COMMENT '执行结果',
  `execution_time` datetime DEFAULT NULL COMMENT '执行时间',
  `execution_duration` bigint(20) DEFAULT NULL COMMENT '执行耗时（毫秒）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sql_id` (`sql_id`),
  KEY `idx_request_id` (`request_id`),
  KEY `idx_sql_type` (`sql_type`),
  KEY `idx_target_table` (`target_table`),
  KEY `idx_execution_status` (`execution_status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SQL语句表';

-- 数据计算任务表
CREATE TABLE `calculation_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `request_id` varchar(64) NOT NULL COMMENT '请求ID',
  `task_id` varchar(64) NOT NULL COMMENT '任务ID',
  `business_type` varchar(50) NOT NULL COMMENT '业务类型',
  `user_id` varchar(64) NOT NULL COMMENT '用户ID',
  `task_status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：PENDING-待执行，EXECUTING-执行中，COMPLETED-已完成，FAILED-失败，CANCELLED-已取消',
  `task_type` varchar(20) NOT NULL DEFAULT 'SYNC' COMMENT '任务类型：SYNC-同步，ASYNC-异步',
  `ai_agent_response` json DEFAULT NULL COMMENT 'AI代理响应结果',
  `logic_flow_chart` longtext COMMENT '逻辑流程图',
  `logic_description` text COMMENT '逻辑思路描述',
  `calculation_result` json DEFAULT NULL COMMENT '计算结果',
  `error_message` text COMMENT '错误信息',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration` bigint(20) DEFAULT NULL COMMENT '执行耗时（毫秒）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_id` (`task_id`),
  KEY `idx_request_id` (`request_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_task_status` (`task_status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据计算任务表';

-- 计算日志表
CREATE TABLE `calculation_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` varchar(64) NOT NULL COMMENT '任务ID',
  `log_level` varchar(10) NOT NULL COMMENT '日志级别：DEBUG, INFO, WARN, ERROR',
  `step_name` varchar(100) DEFAULT NULL COMMENT '步骤名称',
  `log_message` text NOT NULL COMMENT '日志消息',
  `input_data` json DEFAULT NULL COMMENT '输入数据',
  `output_data` json DEFAULT NULL COMMENT '输出数据',
  `execution_time` bigint(20) DEFAULT NULL COMMENT '执行时间（毫秒）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_log_level` (`log_level`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计算日志表';

-- 创建索引
CREATE INDEX `idx_image_upload_record_composite` ON `image_upload_record` (`user_id`, `business_type`, `status`);
CREATE INDEX `idx_image_file_info_composite` ON `image_file_info` (`request_id`, `status`);
CREATE INDEX `idx_image_recognition_result_composite` ON `image_recognition_result` (`request_id`, `recognition_status`);
CREATE INDEX `idx_sql_statement_composite` ON `sql_statement` (`request_id`, `sql_type`, `execution_status`);
CREATE INDEX `idx_calculation_task_composite` ON `calculation_task` (`user_id`, `task_status`, `create_time`);
CREATE INDEX `idx_calculation_log_composite` ON `calculation_log` (`task_id`, `log_level`, `create_time`);
