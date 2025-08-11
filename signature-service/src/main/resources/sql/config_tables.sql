-- 配置分类表
CREATE TABLE config_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    category_code VARCHAR(50) NOT NULL UNIQUE COMMENT '分类编码',
    category_name VARCHAR(100) NOT NULL COMMENT '分类名称',
    description TEXT COMMENT '分类描述',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    sort_order INT DEFAULT 0 COMMENT '排序顺序',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category_code (category_code),
    INDEX idx_status (status)
) COMMENT '配置分类表';

-- 路径配置表
CREATE TABLE path_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    path_pattern VARCHAR(200) NOT NULL COMMENT '路径模式（支持Ant风格）',
    path_name VARCHAR(100) NOT NULL COMMENT '路径名称',
    path_type VARCHAR(20) NOT NULL COMMENT '路径类型：EXCLUDED-排除路径，STRICT-严格验证路径，INTERNAL-内部路径',
    http_methods VARCHAR(100) COMMENT 'HTTP方法（GET,POST,PUT,DELETE等，多个用逗号分隔）',
    description TEXT COMMENT '路径描述',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    sort_order INT DEFAULT 0 COMMENT '排序顺序',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (category_id) REFERENCES config_category(id),
    INDEX idx_category_id (category_id),
    INDEX idx_path_type (path_type),
    INDEX idx_status (status),
    INDEX idx_path_pattern (path_pattern)
) COMMENT '路径配置表';

-- 内部服务配置表
CREATE TABLE internal_service_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    service_code VARCHAR(50) NOT NULL UNIQUE COMMENT '服务编码',
    service_name VARCHAR(100) NOT NULL COMMENT '服务名称',
    service_type VARCHAR(20) NOT NULL COMMENT '服务类型：MICROSERVICE-微服务，GATEWAY-网关，EXTERNAL-外部服务',
    request_prefix VARCHAR(100) COMMENT '请求前缀',
    api_key VARCHAR(100) COMMENT 'API密钥',
    description TEXT COMMENT '服务描述',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    sort_order INT DEFAULT 0 COMMENT '排序顺序',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_service_code (service_code),
    INDEX idx_service_type (service_type),
    INDEX idx_status (status)
) COMMENT '内部服务配置表';

-- 验证规则配置表
CREATE TABLE validation_rule_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    rule_code VARCHAR(50) NOT NULL UNIQUE COMMENT '规则编码',
    rule_name VARCHAR(100) NOT NULL COMMENT '规则名称',
    rule_type VARCHAR(20) NOT NULL COMMENT '规则类型：SIGNATURE-签名验证，API_KEY-API密钥验证，AUTHENTICATION-认证验证',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
    strict_mode TINYINT DEFAULT 0 COMMENT '严格模式：0-非严格，1-严格',
    timeout_ms INT DEFAULT 5000 COMMENT '超时时间（毫秒）',
    retry_count INT DEFAULT 3 COMMENT '重试次数',
    retry_interval_ms INT DEFAULT 1000 COMMENT '重试间隔（毫秒）',
    description TEXT COMMENT '规则描述',
    config_json TEXT COMMENT '规则配置（JSON格式）',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    sort_order INT DEFAULT 0 COMMENT '排序顺序',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_rule_code (rule_code),
    INDEX idx_rule_type (rule_type),
    INDEX idx_status (status)
) COMMENT '验证规则配置表';

-- 缓存配置表
CREATE TABLE cache_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    cache_key VARCHAR(100) NOT NULL UNIQUE COMMENT '缓存键',
    cache_name VARCHAR(100) NOT NULL COMMENT '缓存名称',
    cache_type VARCHAR(20) NOT NULL COMMENT '缓存类型：REDIS-Redis缓存，LOCAL-本地缓存',
    expire_seconds INT DEFAULT 3600 COMMENT '过期时间（秒）',
    max_size INT DEFAULT 1000 COMMENT '最大缓存数量',
    description TEXT COMMENT '缓存描述',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_cache_key (cache_key),
    INDEX idx_cache_type (cache_type),
    INDEX idx_status (status)
) COMMENT '缓存配置表';

-- 初始化数据
INSERT INTO config_category (category_code, category_name, description, sort_order) VALUES
('API_KEY', 'API密钥配置', 'API密钥相关的配置分类', 1),
('SIGNATURE', '签名验证配置', '签名验证相关的配置分类', 2),
('AUTHENTICATION', '认证配置', '认证相关的配置分类', 3),
('PATH', '路径配置', '路径相关的配置分类', 4),
('CACHE', '缓存配置', '缓存相关的配置分类', 5);

-- 初始化路径配置
INSERT INTO path_config (category_id, path_pattern, path_name, path_type, http_methods, description, sort_order) VALUES
(4, '/actuator/**', 'Actuator端点', 'EXCLUDED', 'GET,POST', 'Spring Boot Actuator管理端点', 1),
(4, '/health/**', '健康检查', 'EXCLUDED', 'GET', '健康检查端点', 2),
(4, '/api/auth/**', '认证接口', 'EXCLUDED', 'GET,POST', '认证相关接口', 3),
(4, '/api/public/**', '公开接口', 'EXCLUDED', 'GET,POST', '公开访问接口', 4),
(4, '/api/sso/admin/**', 'SSO管理接口', 'EXCLUDED', 'GET,POST,PUT,DELETE', 'SSO管理相关接口，无需认证', 5),
(4, '/api/signature/**', '签名验证接口', 'STRICT', 'GET,POST', '需要严格验证的签名接口', 6),
(4, '/api/security/**', '安全接口', 'STRICT', 'GET,POST', '需要严格验证的安全接口', 7),
(4, '/api/keys/**', '密钥管理接口', 'STRICT', 'GET,POST,PUT,DELETE', '需要严格验证的密钥管理接口', 8),
(4, '/internal/**', '内部接口', 'INTERNAL', 'GET,POST', '内部服务调用接口', 9);

-- 初始化内部服务配置
INSERT INTO internal_service_config (service_code, service_name, service_type, request_prefix, api_key, description, sort_order) VALUES
('bgai-service', 'bgai服务', 'MICROSERVICE', '/api/bgai', 'test-api-key-123', '原始bgai服务', 1),
('gateway-service', '网关服务', 'GATEWAY', '/api/gateway', 'test-api-key-123', '网关服务', 2),
('signature-service', '签名服务', 'MICROSERVICE', '/api/signature', 'test-api-key-123', '签名验证服务', 3),
('bgtech-ai', 'bgtech-ai服务', 'MICROSERVICE', '/api/bgtech', 'test-api-key-123', 'bgtech-ai服务', 4);

-- 初始化验证规则配置
INSERT INTO validation_rule_config (rule_code, rule_name, rule_type, enabled, strict_mode, timeout_ms, retry_count, retry_interval_ms, description, config_json) VALUES
('API_KEY_VALIDATION', 'API密钥验证', 'API_KEY', 1, 1, 5000, 3, 1000, 'API密钥验证规则', '{"headerName":"X-API-Key","testKey":"test-api-key-123"}'),
('SIGNATURE_VALIDATION', '签名验证', 'SIGNATURE', 1, 1, 5000, 3, 1000, '签名验证规则', '{"algorithm":"SHA256","timestampExpireSeconds":300,"nonceCacheExpireSeconds":1800}'),
('AUTHENTICATION_VALIDATION', '认证验证', 'AUTHENTICATION', 1, 1, 5000, 3, 1000, '认证验证规则', '{"authorizationHeaderName":"Authorization","bearerPrefix":"Bearer "}');

-- 初始化缓存配置
INSERT INTO cache_config (cache_key, cache_name, cache_type, expire_seconds, max_size, description) VALUES
('app_secret_cache', '应用密钥缓存', 'REDIS', 3600, 1000, '应用密钥缓存配置'),
('nonce_cache', 'Nonce缓存', 'REDIS', 1800, 10000, '防重放攻击Nonce缓存'),
('api_key_cache', 'API密钥缓存', 'REDIS', 3600, 1000, 'API密钥缓存配置'),
('user_session_cache', '用户会话缓存', 'REDIS', 3600, 10000, '用户会话缓存配置');
