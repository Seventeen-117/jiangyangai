-- ================================================
-- 针对ApiKeyAuthenticationFilter、JwtAuthenticationFilter、PermissionAuthorizationFilter
-- 的数据库配置INSERT语句
-- ================================================

-- 这些INSERT语句用于替代以下过滤器中的硬编码逻辑：
-- 1. ApiKeyAuthenticationFilter.isSkipApiKeyValidation()
-- 2. JwtAuthenticationFilter.isSkipJwtValidation()  
-- 3. PermissionAuthorizationFilter.isSkipPermissionValidation()

-- ================================================
-- API Key认证排除路径配置
-- ================================================
INSERT INTO `path_config` (`category_id`, `path_pattern`, `path_name`, `path_type`, `http_methods`, `description`, `sort_order`) VALUES
-- API Key认证排除路径（API_KEY_EXCLUDED）- 这些路径不需要API Key认证验证
(4, '/actuator/**', 'Actuator端点', 'API_KEY_EXCLUDED', 'GET,POST', 'Spring Boot Actuator管理端点', 20),
(4, '/health/**', '健康检查', 'API_KEY_EXCLUDED', 'GET', '健康检查端点', 21),
(4, '/api/public/**', '公开接口', 'API_KEY_EXCLUDED', 'GET,POST', '公开访问接口', 22),
(4, '/api/validation/**', '验证接口', 'API_KEY_EXCLUDED', 'GET,POST', '验证相关接口', 23),
(4, '/api/signature/**', '签名验证接口', 'API_KEY_EXCLUDED', 'GET,POST', '签名验证相关接口', 24),
(4, '/api/keys/**', '密钥管理接口', 'API_KEY_EXCLUDED', 'GET,POST,PUT,DELETE', '密钥管理相关接口', 25),
(4, '/api/token/**', 'Token管理接口', 'API_KEY_EXCLUDED', 'GET,POST', 'Token管理相关接口', 26),
(4, '/swagger-ui/**', 'Swagger UI', 'API_KEY_EXCLUDED', 'GET', 'Swagger API文档界面', 27),
(4, '/v3/api-docs/**', 'API文档', 'API_KEY_EXCLUDED', 'GET', 'OpenAPI 3.0文档接口', 28)
ON DUPLICATE KEY UPDATE `updated_time` = CURRENT_TIMESTAMP;

-- ================================================
-- JWT认证排除路径配置
-- ================================================
INSERT INTO `path_config` (`category_id`, `path_pattern`, `path_name`, `path_type`, `http_methods`, `description`, `sort_order`) VALUES
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
(4, '/v3/api-docs/**', 'API文档', 'JWT_EXCLUDED', 'GET', 'OpenAPI 3.0文档接口', 39)
ON DUPLICATE KEY UPDATE `updated_time` = CURRENT_TIMESTAMP;

-- ================================================
-- 权限验证排除路径配置
-- ================================================
INSERT INTO `path_config` (`category_id`, `path_pattern`, `path_name`, `path_type`, `http_methods`, `description`, `sort_order`) VALUES
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
(4, '/v3/api-docs/**', 'API文档', 'PERMISSION_EXCLUDED', 'GET', 'OpenAPI 3.0文档接口', 49)
ON DUPLICATE KEY UPDATE `updated_time` = CURRENT_TIMESTAMP;

-- ================================================
-- 验证规则配置
-- ================================================
INSERT INTO `validation_rule_config` (`rule_code`, `rule_name`, `rule_type`, `enabled`, `strict_mode`, `timeout_ms`, `retry_count`, `retry_interval_ms`, `description`, `config_json`) VALUES
-- API Key认证验证规则（默认禁用，开发环境）
('API_KEY_AUTH_VALIDATION', 'API Key认证验证', 'API_KEY', 0, 1, 5000, 3, 1000, 'API Key认证验证规则（开发环境默认禁用）', 
 '{"headerName":"X-API-Key","authorizationHeaderName":"Authorization","bearerPrefix":"Bearer ","queryParamName":"apiKey"}'),

-- JWT验证规则（默认禁用，开发环境）
('JWT_VALIDATION', 'JWT验证', 'JWT', 0, 1, 5000, 3, 1000, 'JWT验证规则（开发环境默认禁用）', 
 '{"authorizationHeaderName":"Authorization","bearerPrefix":"Bearer ","jwtHeaderName":"X-JWT-Token","queryParamName":"token"}'),

-- 权限验证规则（默认禁用，开发环境）
('PERMISSION_VALIDATION', '权限验证', 'PERMISSION', 0, 1, 5000, 3, 1000, '权限验证规则（开发环境默认禁用）', 
 '{"requireUserId":true,"allowAnonymousAccess":false,"defaultRole":"user"}')
ON DUPLICATE KEY UPDATE `updated_time` = CURRENT_TIMESTAMP;

-- ================================================
-- 验证查询结果
-- ================================================
SELECT '路径配置查询结果' AS info;
SELECT path_pattern, path_name, path_type, http_methods 
FROM path_config 
WHERE path_type IN ('API_KEY_EXCLUDED', 'JWT_EXCLUDED', 'PERMISSION_EXCLUDED')
ORDER BY path_type, sort_order;

SELECT '验证规则配置查询结果' AS info;
SELECT rule_code, rule_name, rule_type, enabled 
FROM validation_rule_config 
WHERE rule_code IN ('API_KEY_AUTH_VALIDATION', 'JWT_VALIDATION', 'PERMISSION_VALIDATION')
ORDER BY rule_code;

-- ================================================
-- 硬编码替代说明
-- ================================================
/*
这些配置替代了以下硬编码逻辑：

1. ApiKeyAuthenticationFilter.isSkipApiKeyValidation():
   原硬编码路径：
   - /actuator、/health、/metrics、/public、/api/validation、/swagger-ui、/v3/api-docs
   
   现在由数据库配置 path_type = 'API_KEY_EXCLUDED' 的记录控制

2. JwtAuthenticationFilter.isSkipJwtValidation():
   原硬编码路径：
   - /actuator、/health、/metrics、/public、/api/validation、/api/metrics、
     /api/token、/api/keys、/swagger-ui、/v3/api-docs
   
   现在由数据库配置 path_type = 'JWT_EXCLUDED' 的记录控制

3. PermissionAuthorizationFilter.isSkipPermissionValidation():
   原硬编码路径：
   - /actuator、/health、/metrics、/public、/api/validation、/api/metrics、
     /api/token、/api/keys、/swagger-ui、/v3/api-docs
   
   现在由数据库配置 path_type = 'PERMISSION_EXCLUDED' 的记录控制

验证规则开关：
- 每个过滤器的启用/禁用状态由 validation_rule_config 表控制
- 可以通过修改 enabled 字段动态开启/关闭验证
- 修改配置后调用缓存刷新接口即可生效

使用方法：
1. 执行这些INSERT语句初始化配置
2. 重启signature-service服务
3. 通过配置管理接口动态调整验证规则
4. 硬编码逻辑已被数据库配置替代
*/
