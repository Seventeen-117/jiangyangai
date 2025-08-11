-- ================================================
-- 动态配置表结构和初始数据
-- 用于替代signature-service中的硬编码验证逻辑
-- 执行前请确保数据库连接正常
-- ================================================

-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ================================================
-- 1. 创建配置分类表
-- ================================================
CREATE TABLE IF NOT EXISTS `config_category` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `category_code` VARCHAR(50) NOT NULL UNIQUE COMMENT '分类编码',
    `category_name` VARCHAR(100) NOT NULL COMMENT '分类名称',
    `description` TEXT COMMENT '分类描述',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `sort_order` INT DEFAULT 0 COMMENT '排序顺序',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_category_code` (`category_code`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置分类表';

-- ================================================
-- 2. 创建路径配置表
-- ================================================
CREATE TABLE IF NOT EXISTS `path_config` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `category_id` BIGINT NOT NULL COMMENT '分类ID',
    `path_pattern` VARCHAR(200) NOT NULL COMMENT '路径模式（支持Ant风格）',
    `path_name` VARCHAR(100) NOT NULL COMMENT '路径名称',
    `path_type` VARCHAR(20) NOT NULL COMMENT '路径类型：EXCLUDED-排除路径，STRICT-严格验证路径，INTERNAL-内部路径',
    `http_methods` VARCHAR(100) COMMENT 'HTTP方法（GET,POST,PUT,DELETE等，多个用逗号分隔）',
    `description` TEXT COMMENT '路径描述',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `sort_order` INT DEFAULT 0 COMMENT '排序顺序',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_category_id` (`category_id`),
    INDEX `idx_path_type` (`path_type`),
    INDEX `idx_status` (`status`),
    INDEX `idx_path_pattern` (`path_pattern`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='路径配置表';

-- ================================================
-- 3. 创建内部服务配置表
-- ================================================
CREATE TABLE IF NOT EXISTS `internal_service_config` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `service_code` VARCHAR(50) NOT NULL UNIQUE COMMENT '服务编码',
    `service_name` VARCHAR(100) NOT NULL COMMENT '服务名称',
    `service_type` VARCHAR(20) NOT NULL COMMENT '服务类型：MICROSERVICE-微服务，GATEWAY-网关，EXTERNAL-外部服务',
    `request_prefix` VARCHAR(100) COMMENT '请求前缀',
    `api_key` VARCHAR(100) COMMENT 'API密钥',
    `description` TEXT COMMENT '服务描述',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `sort_order` INT DEFAULT 0 COMMENT '排序顺序',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_service_code` (`service_code`),
    INDEX `idx_service_type` (`service_type`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内部服务配置表';

-- ================================================
-- 4. 创建验证规则配置表
-- ================================================
CREATE TABLE IF NOT EXISTS `validation_rule_config` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `rule_code` VARCHAR(50) NOT NULL UNIQUE COMMENT '规则编码',
    `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
    `rule_type` VARCHAR(20) NOT NULL COMMENT '规则类型：SIGNATURE-签名验证，API_KEY-API密钥验证，AUTHENTICATION-认证验证',
    `enabled` TINYINT DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
    `strict_mode` TINYINT DEFAULT 0 COMMENT '严格模式：0-非严格，1-严格',
    `timeout_ms` INT DEFAULT 5000 COMMENT '超时时间（毫秒）',
    `retry_count` INT DEFAULT 3 COMMENT '重试次数',
    `retry_interval_ms` INT DEFAULT 1000 COMMENT '重试间隔（毫秒）',
    `description` TEXT COMMENT '规则描述',
    `config_json` TEXT COMMENT '规则配置（JSON格式）',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `sort_order` INT DEFAULT 0 COMMENT '排序顺序',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_rule_code` (`rule_code`),
    INDEX `idx_rule_type` (`rule_type`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='验证规则配置表';

-- ================================================
-- 5. 创建缓存配置表
-- ================================================
CREATE TABLE IF NOT EXISTS `cache_config` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `cache_key` VARCHAR(100) NOT NULL UNIQUE COMMENT '缓存键',
    `cache_name` VARCHAR(100) NOT NULL COMMENT '缓存名称',
    `cache_type` VARCHAR(20) NOT NULL COMMENT '缓存类型：REDIS-Redis缓存，LOCAL-本地缓存',
    `expire_seconds` INT DEFAULT 3600 COMMENT '过期时间（秒）',
    `max_size` INT DEFAULT 1000 COMMENT '最大缓存数量',
    `description` TEXT COMMENT '缓存描述',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_cache_key` (`cache_key`),
    INDEX `idx_cache_type` (`cache_type`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缓存配置表';

-- ================================================
-- 6. 添加外键约束
-- ================================================
ALTER TABLE `path_config` 
ADD CONSTRAINT `fk_path_config_category` 
FOREIGN KEY (`category_id`) REFERENCES `config_category`(`id`) 
ON DELETE RESTRICT ON UPDATE CASCADE;

-- ================================================
-- 7. 插入配置分类数据
-- ================================================
INSERT INTO `config_category` (`category_code`, `category_name`, `description`, `sort_order`) VALUES
('API_KEY', 'API密钥配置', 'API密钥相关的配置分类', 1),
('SIGNATURE', '签名验证配置', '签名验证相关的配置分类', 2),
('AUTHENTICATION', '认证配置', '认证相关的配置分类', 3),
('PATH', '路径配置', '路径相关的配置分类', 4),
('CACHE', '缓存配置', '缓存相关的配置分类', 5)
ON DUPLICATE KEY UPDATE `updated_time` = CURRENT_TIMESTAMP;

-- ================================================
-- 8. 插入路径配置数据（替代硬编码的排除路径）
-- ================================================
INSERT INTO `path_config` (`category_id`, `path_pattern`, `path_name`, `path_type`, `http_methods`, `description`, `sort_order`) VALUES
-- 排除路径（EXCLUDED）- 这些路径不需要任何验证
(4, '/actuator/**', 'Actuator端点', 'EXCLUDED', 'GET,POST', 'Spring Boot Actuator管理端点', 1),
(4, '/health/**', '健康检查', 'EXCLUDED', 'GET', '健康检查端点', 2),
(4, '/api/auth/**', '认证接口', 'EXCLUDED', 'GET,POST', '认证相关接口', 3),
(4, '/api/public/**', '公开接口', 'EXCLUDED', 'GET,POST', '公开访问接口', 4),
(4, '/api/sso/admin/**', 'SSO管理接口', 'EXCLUDED', 'GET,POST,PUT,DELETE', 'SSO管理相关接口，无需认证', 5),
(4, '/api/signature/**', '签名验证接口', 'EXCLUDED', 'GET,POST', '签名验证相关接口，无需签名认证', 6),
(4, '/api/token/**', 'Token管理接口', 'EXCLUDED', 'GET,POST', 'Token管理相关接口', 7),
(4, '/api/keys/**', '密钥管理接口', 'EXCLUDED', 'GET,POST,PUT,DELETE', '密钥管理相关接口', 8),
(4, '/api/validation/**', '验证接口', 'EXCLUDED', 'GET,POST', '验证相关接口', 9),
(4, '/api/app-secret/**', '应用密钥接口', 'EXCLUDED', 'GET,POST,PUT,DELETE', '应用密钥管理接口', 10),
(4, '/api/metrics/**', '监控指标接口', 'EXCLUDED', 'GET', '监控指标相关接口', 11),
(4, '/swagger-ui/**', 'Swagger UI', 'EXCLUDED', 'GET', 'Swagger API文档界面', 12),
(4, '/v3/api-docs/**', 'API文档', 'EXCLUDED', 'GET', 'OpenAPI 3.0文档接口', 13),

-- API Key认证排除路径（API_KEY_EXCLUDED）- 这些路径不需要API Key认证验证
(4, '/actuator/**', 'Actuator端点', 'API_KEY_EXCLUDED', 'GET,POST', 'Spring Boot Actuator管理端点', 20),
(4, '/health/**', '健康检查', 'API_KEY_EXCLUDED', 'GET', '健康检查端点', 21),
(4, '/api/public/**', '公开接口', 'API_KEY_EXCLUDED', 'GET,POST', '公开访问接口', 22),
(4, '/api/validation/**', '验证接口', 'API_KEY_EXCLUDED', 'GET,POST', '验证相关接口', 23),
(4, '/api/signature/**', '签名验证接口', 'API_KEY_EXCLUDED', 'GET,POST', '签名验证相关接口', 24),
(4, '/api/keys/**', '密钥管理接口', 'API_KEY_EXCLUDED', 'GET,POST,PUT,DELETE', '密钥管理相关接口', 25),
(4, '/api/token/**', 'Token管理接口', 'API_KEY_EXCLUDED', 'GET,POST', 'Token管理相关接口', 26),
(4, '/swagger-ui/**', 'Swagger UI', 'API_KEY_EXCLUDED', 'GET', 'Swagger API文档界面', 27),
(4, '/v3/api-docs/**', 'API文档', 'API_KEY_EXCLUDED', 'GET', 'OpenAPI 3.0文档接口', 28),

-- JWT认证排除路径（JWT_EXCLUDED）- 这些路径不需要JWT验证
(4, '/actuator/**', 'Actuator端点', 'JWT_EXCLUDED', 'GET,POST', 'Spring Boot Actuator管理端点', 30),
(4, '/health/**', '健康检查', 'JWT_EXCLUDED', 'GET', '健康检查端点', 31),
(4, '/api/public/**', '公开接口', 'JWT_EXCLUDED', 'GET,POST', '公开访问接口', 32),
(4, '/api/validation/**', '验证接口', 'JWT_EXCLUDED', 'GET,POST', '验证相关接口', 33),
(4, '/api/metrics/**', '监控指标接口', 'JWT_EXCLUDED', 'GET', '监控指标相关接口', 34),
(4, '/api/token/**', 'Token管理接口', 'JWT_EXCLUDED', 'GET,POST', 'Token管理相关接口（包括Token生成）', 35),
(4, '/api/keys/**', '密钥管理接口', 'JWT_EXCLUDED', 'GET,POST,PUT,DELETE', '密钥管理相关接口', 36),
(4, '/api/signature/**', '签名验证接口', 'JWT_EXCLUDED', 'GET,POST', '签名验证相关接口', 37),
(4, '/swagger-ui/**', 'Swagger UI', 'JWT_EXCLUDED', 'GET', 'Swagger API文档界面', 38),
(4, '/v3/api-docs/**', 'API文档', 'JWT_EXCLUDED', 'GET', 'OpenAPI 3.0文档接口', 39),

-- 权限验证排除路径（PERMISSION_EXCLUDED）- 这些路径不需要权限验证
(4, '/actuator/**', 'Actuator端点', 'PERMISSION_EXCLUDED', 'GET,POST', 'Spring Boot Actuator管理端点', 40),
(4, '/health/**', '健康检查', 'PERMISSION_EXCLUDED', 'GET', '健康检查端点', 41),
(4, '/api/public/**', '公开接口', 'PERMISSION_EXCLUDED', 'GET,POST', '公开访问接口', 42),
(4, '/api/validation/**', '验证接口', 'PERMISSION_EXCLUDED', 'GET,POST', '验证相关接口', 43),
(4, '/api/metrics/**', '监控指标接口', 'PERMISSION_EXCLUDED', 'GET', '监控指标相关接口', 44),
(4, '/api/token/**', 'Token管理接口', 'PERMISSION_EXCLUDED', 'GET,POST', 'Token管理相关接口', 45),
(4, '/api/keys/**', '密钥管理接口', 'PERMISSION_EXCLUDED', 'GET,POST,PUT,DELETE', '密钥管理相关接口', 46),
(4, '/api/signature/**', '签名验证接口', 'PERMISSION_EXCLUDED', 'GET,POST', '签名验证相关接口', 47),
(4, '/swagger-ui/**', 'Swagger UI', 'PERMISSION_EXCLUDED', 'GET', 'Swagger API文档界面', 48),
(4, '/v3/api-docs/**', 'API文档', 'PERMISSION_EXCLUDED', 'GET', 'OpenAPI 3.0文档接口', 49),

-- 严格验证路径（STRICT）- 这些路径需要严格的API Key验证
(4, '/api/security/**', '安全接口', 'STRICT', 'GET,POST', '需要严格验证的安全接口', 50),
(4, '/api/admin/**', '管理接口', 'STRICT', 'GET,POST,PUT,DELETE', '需要严格验证的管理接口', 51),

-- 内部路径（INTERNAL）- 这些路径用于内部服务调用
(4, '/internal/**', '内部接口', 'INTERNAL', 'GET,POST', '内部服务调用接口', 60)
ON DUPLICATE KEY UPDATE `updated_time` = CURRENT_TIMESTAMP;

-- ================================================
-- 9. 插入内部服务配置数据（替代硬编码的内部服务判断）
-- ================================================
INSERT INTO `internal_service_config` (`service_code`, `service_name`, `service_type`, `request_prefix`, `api_key`, `description`, `sort_order`) VALUES
('bgai-service', 'bgai服务', 'MICROSERVICE', '/api/bgai', 'test-api-key-123', '原始bgai服务', 1),
('gateway-service', '网关服务', 'GATEWAY', '/api/gateway', 'test-api-key-123', '网关服务', 2),
('signature-service', '签名服务', 'MICROSERVICE', '/api/signature', 'test-api-key-123', '签名验证服务', 3),
('bgtech-ai', 'bgtech-ai服务', 'MICROSERVICE', '/api/bgtech', 'test-api-key-123', 'bgtech-ai服务', 4)
ON DUPLICATE KEY UPDATE `updated_time` = CURRENT_TIMESTAMP;

-- ================================================
-- 10. 插入验证规则配置数据（替代硬编码的验证开关）
-- ================================================
INSERT INTO `validation_rule_config` (`rule_code`, `rule_name`, `rule_type`, `enabled`, `strict_mode`, `timeout_ms`, `retry_count`, `retry_interval_ms`, `description`, `config_json`) VALUES
-- API密钥验证规则（默认禁用，开发环境）
('API_KEY_VALIDATION', 'API密钥验证', 'API_KEY', 0, 1, 5000, 3, 1000, 'API密钥验证规则（开发环境默认禁用）', '{"headerName":"X-API-Key","testKey":"test-api-key-123","allowInternalServiceSkip":true}'),
-- 签名验证规则（默认禁用，开发环境）
('SIGNATURE_VALIDATION', '签名验证', 'SIGNATURE', 0, 1, 5000, 3, 1000, '签名验证规则（开发环境默认禁用）', '{"algorithm":"SHA256","timestampExpireSeconds":300,"nonceCacheExpireSeconds":1800}'),
-- 认证验证规则（启用）
('AUTHENTICATION_VALIDATION', '认证验证', 'AUTHENTICATION', 1, 1, 5000, 3, 1000, '认证验证规则', '{"authorizationHeaderName":"Authorization","bearerPrefix":"Bearer "}'),
-- API Key认证验证规则（默认禁用，开发环境）
('API_KEY_AUTH_VALIDATION', 'API Key认证验证', 'API_KEY', 0, 1, 5000, 3, 1000, 'API Key认证验证规则（开发环境默认禁用）', '{"headerName":"X-API-Key","authorizationHeaderName":"Authorization","bearerPrefix":"Bearer ","queryParamName":"apiKey"}'),
-- JWT验证规则（默认禁用，开发环境）
('JWT_VALIDATION', 'JWT验证', 'JWT', 0, 1, 5000, 3, 1000, 'JWT验证规则（开发环境默认禁用）', '{"authorizationHeaderName":"Authorization","bearerPrefix":"Bearer ","jwtHeaderName":"X-JWT-Token","queryParamName":"token"}'),
-- 权限验证规则（默认禁用，开发环境）
('PERMISSION_VALIDATION', '权限验证', 'PERMISSION', 0, 1, 5000, 3, 1000, '权限验证规则（开发环境默认禁用）', '{"requireUserId":true,"allowAnonymousAccess":false,"defaultRole":"user"}')
ON DUPLICATE KEY UPDATE `updated_time` = CURRENT_TIMESTAMP;

-- ================================================
-- 11. 插入缓存配置数据
-- ================================================
INSERT INTO `cache_config` (`cache_key`, `cache_name`, `cache_type`, `expire_seconds`, `max_size`, `description`) VALUES
('app_secret_cache', '应用密钥缓存', 'REDIS', 3600, 1000, '应用密钥缓存配置'),
('nonce_cache', 'Nonce缓存', 'REDIS', 1800, 10000, '防重放攻击Nonce缓存'),
('api_key_cache', 'API密钥缓存', 'REDIS', 3600, 1000, 'API密钥缓存配置'),
('user_session_cache', '用户会话缓存', 'REDIS', 3600, 10000, '用户会话缓存配置'),
('path_config_cache', '路径配置缓存', 'LOCAL', 1800, 500, '路径配置缓存'),
('validation_rule_cache', '验证规则缓存', 'LOCAL', 1800, 100, '验证规则缓存'),
('internal_service_cache', '内部服务缓存', 'LOCAL', 3600, 100, '内部服务配置缓存')
ON DUPLICATE KEY UPDATE `updated_time` = CURRENT_TIMESTAMP;

-- ================================================
-- 12. 设置外键检查
-- ================================================
SET FOREIGN_KEY_CHECKS = 1;

-- ================================================
-- 13. 验证数据插入结果
-- ================================================
SELECT '配置分类表数据' AS info;
SELECT category_code, category_name, status FROM config_category ORDER BY sort_order;

SELECT '路径配置表数据' AS info;
SELECT path_pattern, path_name, path_type, http_methods, status FROM path_config ORDER BY sort_order;

SELECT '内部服务配置表数据' AS info;
SELECT service_code, service_name, service_type, status FROM internal_service_config ORDER BY sort_order;

SELECT '验证规则配置表数据' AS info;
SELECT rule_code, rule_name, rule_type, enabled, status FROM validation_rule_config ORDER BY sort_order;

SELECT '缓存配置表数据' AS info;
SELECT cache_key, cache_name, cache_type, status FROM cache_config;

-- ================================================
-- SQL脚本执行完成
-- ================================================
