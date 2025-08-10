-- API权限相关数据库表结构

-- API权限表
CREATE TABLE IF NOT EXISTS `api_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `permission_code` VARCHAR(100) NOT NULL COMMENT '权限代码',
    `permission_name` VARCHAR(200) NOT NULL COMMENT '权限名称',
    `permission_type` VARCHAR(50) DEFAULT 'API' COMMENT '权限类型',
    `description` TEXT DEFAULT NULL COMMENT '权限描述',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`permission_code`),
    KEY `idx_permission_type` (`permission_type`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API权限表';

-- API密钥权限关联表
CREATE TABLE IF NOT EXISTS `api_key_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `api_key_id` BIGINT NOT NULL COMMENT 'API密钥ID',
    `permission_id` BIGINT NOT NULL COMMENT '权限ID',
    `granted_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
    `granted_by` VARCHAR(100) DEFAULT NULL COMMENT '授权人',
    `expires_at` DATETIME DEFAULT NULL COMMENT '过期时间',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_api_key_permission` (`api_key_id`, `permission_id`),
    KEY `idx_api_key_id` (`api_key_id`),
    KEY `idx_permission_id` (`permission_id`),
    KEY `idx_status` (`status`),
    KEY `idx_expires_at` (`expires_at`),
    FOREIGN KEY (`api_key_id`) REFERENCES `api_key` (`id`) ON DELETE CASCADE,
    FOREIGN KEY (`permission_id`) REFERENCES `api_permission` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API密钥权限关联表';

-- 客户端权限关联表
CREATE TABLE IF NOT EXISTS `client_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `client_id` VARCHAR(100) NOT NULL COMMENT '客户端ID',
    `permission_id` BIGINT NOT NULL COMMENT '权限ID',
    `granted_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
    `granted_by` VARCHAR(100) DEFAULT NULL COMMENT '授权人',
    `expires_at` DATETIME DEFAULT NULL COMMENT '过期时间',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_client_permission` (`client_id`, `permission_id`),
    KEY `idx_client_id` (`client_id`),
    KEY `idx_permission_id` (`permission_id`),
    KEY `idx_status` (`status`),
    KEY `idx_expires_at` (`expires_at`),
    FOREIGN KEY (`permission_id`) REFERENCES `api_permission` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户端权限关联表';

-- 插入默认权限数据
INSERT INTO `api_permission` (`permission_code`, `permission_name`, `permission_type`, `description`, `status`) VALUES
('api:read', 'API读取权限', 'API', '允许读取API数据', 1),
('api:write', 'API写入权限', 'API', '允许写入API数据', 1),
('api:delete', 'API删除权限', 'API', '允许删除API数据', 1),
('api:admin', 'API管理权限', 'API', '允许管理API配置', 1),
('user:read', '用户读取权限', 'USER', '允许读取用户信息', 1),
('user:write', '用户写入权限', 'USER', '允许写入用户信息', 1),
('user:delete', '用户删除权限', 'USER', '允许删除用户信息', 1),
('user:admin', '用户管理权限', 'USER', '允许管理用户配置', 1),
('system:read', '系统读取权限', 'SYSTEM', '允许读取系统信息', 1),
('system:write', '系统写入权限', 'SYSTEM', '允许写入系统信息', 1),
('system:admin', '系统管理权限', 'SYSTEM', '允许管理系统配置', 1),
('auth:read', '认证读取权限', 'AUTH', '允许读取认证信息', 1),
('auth:write', '认证写入权限', 'AUTH', '允许写入认证信息', 1),
('auth:admin', '认证管理权限', 'AUTH', '允许管理认证配置', 1),
('log:read', '日志读取权限', 'LOG', '允许读取日志信息', 1),
('log:write', '日志写入权限', 'LOG', '允许写入日志信息', 1),
('log:admin', '日志管理权限', 'LOG', '允许管理日志配置', 1)
ON DUPLICATE KEY UPDATE `updated_at` = CURRENT_TIMESTAMP;
