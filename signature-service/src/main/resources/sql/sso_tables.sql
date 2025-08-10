-- SSO相关数据库表结构

-- 授权码表
CREATE TABLE IF NOT EXISTS `authorization_code` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code` VARCHAR(255) NOT NULL COMMENT '授权码',
    `client_id` VARCHAR(255) NOT NULL COMMENT '客户端ID',
    `user_id` VARCHAR(255) NOT NULL COMMENT '用户ID',
    `redirect_uri` VARCHAR(500) NOT NULL COMMENT '重定向URI',
    `scope` VARCHAR(500) DEFAULT NULL COMMENT '权限范围',
    `state` VARCHAR(255) DEFAULT NULL COMMENT '状态参数',
    `expires_at` DATETIME NOT NULL COMMENT '过期时间',
    `used` BOOLEAN DEFAULT FALSE COMMENT '是否已使用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_client_id` (`client_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='授权码表';

-- OAuth客户端表
CREATE TABLE IF NOT EXISTS `oauth_client` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `client_id` VARCHAR(255) NOT NULL COMMENT '客户端ID',
    `client_secret` VARCHAR(500) NOT NULL COMMENT '客户端密钥',
    `client_name` VARCHAR(255) NOT NULL COMMENT '客户端名称',
    `redirect_uri` VARCHAR(500) NOT NULL COMMENT '重定向URI',
    `scope` VARCHAR(500) DEFAULT NULL COMMENT '权限范围',
    `grant_types` VARCHAR(500) DEFAULT NULL COMMENT '授权类型',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_client_id` (`client_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth客户端表';

-- SSO用户表
CREATE TABLE IF NOT EXISTS `sso_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` VARCHAR(255) NOT NULL COMMENT '用户ID',
    `username` VARCHAR(255) NOT NULL COMMENT '用户名',
    `password` VARCHAR(500) NOT NULL COMMENT '密码',
    `email` VARCHAR(255) DEFAULT NULL COMMENT '邮箱',
    `nickname` VARCHAR(255) DEFAULT NULL COMMENT '昵称',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像',
    `role` VARCHAR(100) DEFAULT 'USER' COMMENT '角色',
    `department` VARCHAR(255) DEFAULT NULL COMMENT '部门',
    `position` VARCHAR(255) DEFAULT NULL COMMENT '职位',
    `phone` VARCHAR(50) DEFAULT NULL COMMENT '手机号',
    `gender` VARCHAR(20) DEFAULT NULL COMMENT '性别',
    `status` VARCHAR(50) DEFAULT 'ACTIVE' COMMENT '状态',
    `enabled` BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    `locked` BOOLEAN DEFAULT FALSE COMMENT '是否锁定',
    `last_login_at` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(100) DEFAULT NULL COMMENT '最后登录IP',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_status` (`status`),
    KEY `idx_enabled` (`enabled`),
    KEY `idx_locked` (`locked`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SSO用户表';

-- 插入默认数据

-- 插入默认OAuth客户端
INSERT INTO `oauth_client` (`client_id`, `client_secret`, `client_name`, `redirect_uri`, `scope`, `grant_types`, `status`) VALUES
('default-client', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '默认客户端', 'http://localhost:8080/api/sso/callback', 'read write', 'authorization_code refresh_token password', 1)
ON DUPLICATE KEY UPDATE `updated_at` = CURRENT_TIMESTAMP;

-- 插入默认用户（密码：123456）
INSERT INTO `sso_user` (`user_id`, `username`, `password`, `email`, `nickname`, `role`, `status`, `enabled`, `locked`) VALUES
('user-001', 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'admin@example.com', '管理员', 'ADMIN', 'ACTIVE', TRUE, FALSE),
('user-002', 'testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'test@example.com', '测试用户', 'USER', 'ACTIVE', TRUE, FALSE)
ON DUPLICATE KEY UPDATE `updated_at` = CURRENT_TIMESTAMP;
