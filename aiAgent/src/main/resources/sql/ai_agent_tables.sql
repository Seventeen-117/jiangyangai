-- AI代理服务数据库表结构
-- 创建时间: 2025-08-11
-- 数据库: ai_agent

-- 创建AI代理表
CREATE TABLE IF NOT EXISTS `ai_agent` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '代理名称',
  `description` text COMMENT '代理描述',
  `type` varchar(50) NOT NULL COMMENT '代理类型(openai, azure, ollama)',
  `model` varchar(100) NOT NULL COMMENT '模型名称',
  `config` longtext COMMENT '配置参数(JSON格式)',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态(0:禁用, 1:启用)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_type` (`type`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI代理表';

-- 创建AI聊天记录表
CREATE TABLE IF NOT EXISTS `ai_chat_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` varchar(100) NOT NULL COMMENT '会话ID',
  `user_id` varchar(100) DEFAULT NULL COMMENT '用户ID',
  `agent_id` bigint(20) NOT NULL COMMENT 'AI代理ID',
  `message` text NOT NULL COMMENT '用户消息',
  `response` longtext NOT NULL COMMENT 'AI响应',
  `type` varchar(50) NOT NULL COMMENT 'AI类型',
  `model` varchar(100) NOT NULL COMMENT '使用的模型',
  `tokens_used` int(11) DEFAULT NULL COMMENT '使用的token数量',
  `cost` decimal(10,6) DEFAULT NULL COMMENT '费用',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态(0:失败, 1:成功)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_agent_id` (`agent_id`),
  KEY `idx_type` (`type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI聊天记录表';

-- 插入示例数据
INSERT INTO `ai_agent` (`name`, `description`, `type`, `model`, `config`, `status`, `create_by`) VALUES
('OpenAI GPT-3.5', 'OpenAI GPT-3.5 Turbo模型', 'openai', 'gpt-3.5-turbo', '{"temperature": 0.7, "max_tokens": 1000}', 1, 'system'),
('Azure OpenAI', 'Azure OpenAI服务', 'azure', 'gpt-35-turbo', '{"temperature": 0.7, "max_tokens": 1000}', 1, 'system'),
('Ollama Llama2', '本地Ollama Llama2模型', 'ollama', 'llama2', '{"temperature": 0.7, "max_tokens": 1000}', 1, 'system');

-- 创建索引
CREATE INDEX IF NOT EXISTS `idx_ai_agent_type_status` ON `ai_agent` (`type`, `status`);
CREATE INDEX IF NOT EXISTS `idx_ai_chat_record_user_time` ON `ai_chat_record` (`user_id`, `create_time`);
