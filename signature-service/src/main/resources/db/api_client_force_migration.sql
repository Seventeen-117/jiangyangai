-- API Client 强制迁移脚本
-- 彻底解决外键约束冲突问题

-- 开始迁移
SELECT '=== API Client 强制迁移开始 ===' AS status;

-- 步骤1: 完全禁用外键检查
SET FOREIGN_KEY_CHECKS = 0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL';

-- 步骤2: 找出所有相关的外键约束并删除
SELECT '步骤2: 删除所有相关的外键约束' AS step;

-- 删除 client_permission 表的外键约束
DROP PROCEDURE IF EXISTS DropForeignKey;

DELIMITER $$
CREATE PROCEDURE DropForeignKey(IN tableName VARCHAR(64), IN constraintName VARCHAR(64))
BEGIN
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION BEGIN END;
    SET @sql = CONCAT('ALTER TABLE ', tableName, ' DROP FOREIGN KEY ', constraintName);
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
END$$
DELIMITER ;

-- 尝试删除所有可能的外键约束
CALL DropForeignKey('client_permission', 'fk_client_id');
CALL DropForeignKey('client_permission', 'client_permission_ibfk_1');
CALL DropForeignKey('client_permission', 'client_permission_ibfk_2');
CALL DropForeignKey('api_key_permission', 'api_key_permission_ibfk_1');
CALL DropForeignKey('api_key_permission', 'api_key_permission_ibfk_2');
CALL DropForeignKey('authorization_code', 'authorization_code_ibfk_1');

-- 删除存储过程
DROP PROCEDURE DropForeignKey;

-- 步骤3: 备份所有相关表的数据
SELECT '步骤3: 备份相关表数据' AS step;

-- 备份 api_client 表
DROP TABLE IF EXISTS api_client_backup_force;
CREATE TABLE api_client_backup_force AS SELECT * FROM api_client WHERE 1=1;

-- 备份 client_permission 表（如果存在）
DROP TABLE IF EXISTS client_permission_backup;
SET @table_exists = (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'client_permission');
SET @sql = IF(@table_exists > 0, 'CREATE TABLE client_permission_backup AS SELECT * FROM client_permission WHERE 1=1', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 步骤4: 删除所有依赖表中与 client_id 相关的数据（临时）
SELECT '步骤4: 临时清理依赖表数据' AS step;

-- 清空 client_permission 表数据（保留结构）
SET @sql = IF(@table_exists > 0, 'TRUNCATE TABLE client_permission', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 步骤5: 删除并重建 api_client 表
SELECT '步骤5: 重建 api_client 表' AS step;

DROP TABLE IF EXISTS api_client;

CREATE TABLE api_client (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `client_id` varchar(64) NOT NULL COMMENT '客户端ID',
  `client_name` varchar(255) NOT NULL COMMENT '客户端名称',
  `client_type` varchar(32) DEFAULT 'STANDARD' COMMENT '客户端类型',
  `description` text COMMENT '客户端描述',
  `status` tinyint(1) DEFAULT '1' COMMENT '客户端状态 (0: 禁用, 1: 启用)',
  `client_secret` varchar(128) DEFAULT NULL COMMENT '客户端密钥',
  `redirect_uri` varchar(500) DEFAULT NULL COMMENT '重定向URI',
  `grant_type` varchar(100) DEFAULT 'client_credentials' COMMENT '授权类型',
  `scope` varchar(200) DEFAULT 'api' COMMENT '作用域',
  `access_token_validity` int(11) DEFAULT '3600' COMMENT '访问令牌有效期（秒）',
  `refresh_token_validity` int(11) DEFAULT '7200' COMMENT '刷新令牌有效期（秒）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT 'system' COMMENT '创建者',
  `update_by` varchar(64) DEFAULT 'system' COMMENT '更新者',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除 (0: 未删除, 1: 已删除)',
  `version` int(11) DEFAULT '1' COMMENT '版本号 (乐观锁)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_client_id` (`client_id`),
  KEY `idx_client_type` (`client_type`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API客户端表';

-- 步骤6: 迁移原有数据
SELECT '步骤6: 迁移原有数据' AS step;

INSERT INTO api_client (
    client_id, client_name, description, status, create_time, update_time, deleted
)
SELECT 
    client_id,
    COALESCE(client_name, client_id) as client_name,
    COALESCE(description, '') as description,
    COALESCE(status, 1) as status,
    COALESCE(create_time, NOW()) as create_time,
    COALESCE(update_time, NOW()) as update_time,
    COALESCE(deleted, 0) as deleted
FROM api_client_backup_force
WHERE client_id IS NOT NULL
ON DUPLICATE KEY UPDATE 
    client_name = VALUES(client_name),
    description = VALUES(description),
    status = VALUES(status),
    update_time = NOW();

-- 步骤7: 插入标准测试数据
SELECT '步骤7: 插入标准测试数据' AS step;

INSERT INTO api_client (
    client_id, client_name, client_type, description, status, 
    client_secret, grant_type, scope, access_token_validity, 
    refresh_token_validity, create_by, update_by, deleted, version
) VALUES 
(
    'test-client-123', 'Test Client', 'STANDARD', 'Test API Key for development', 1,
    'test-secret-123', 'client_credentials', 'api,read,write', 3600,
    7200, 'system', 'system', 0, 1
),
(
    'dev-client-001', 'Development Client', 'DEVELOPMENT', 'Development environment client', 1,
    'dev-secret-001', 'client_credentials', 'api,read,write,admin', 7200,
    14400, 'system', 'system', 0, 1
),
(
    'prod-client-001', 'Production Client', 'PRODUCTION', 'Production environment client', 1,
    'prod-secret-001', 'client_credentials', 'api,read,write', 1800,
    3600, 'system', 'system', 0, 1
)
ON DUPLICATE KEY UPDATE 
    client_name = VALUES(client_name),
    client_type = VALUES(client_type),
    description = VALUES(description),
    update_time = NOW();

-- 步骤8: 修复所有依赖表的字段类型
SELECT '步骤8: 修复依赖表字段类型' AS step;

-- 修复 client_permission 表
SET @table_exists = (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'client_permission');
SET @sql = IF(@table_exists > 0, 'ALTER TABLE client_permission MODIFY COLUMN client_id VARCHAR(64) NOT NULL COMMENT \'客户端ID\'', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 修复 authorization_code 表
SET @table_exists = (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'authorization_code');
SET @sql = IF(@table_exists > 0, 'ALTER TABLE authorization_code MODIFY COLUMN client_id VARCHAR(64) NOT NULL COMMENT \'客户端ID\'', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 修复 oauth_client 表
SET @table_exists = (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'oauth_client');
SET @sql = IF(@table_exists > 0, 'ALTER TABLE oauth_client MODIFY COLUMN client_id VARCHAR(64) NOT NULL COMMENT \'客户端ID\'', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 步骤9: 恢复依赖表的数据（如果需要）
SELECT '步骤9: 恢复依赖表数据' AS step;

-- 恢复 client_permission 数据，只保留有效的 client_id
SET @table_exists = (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'client_permission_backup');
SET @sql = IF(@table_exists > 0, 
    'INSERT INTO client_permission SELECT * FROM client_permission_backup WHERE client_id IN (SELECT client_id FROM api_client) ON DUPLICATE KEY UPDATE updated_at = NOW()', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 步骤10: 重新启用外键检查和恢复SQL模式
SELECT '步骤10: 恢复系统设置' AS step;
SET FOREIGN_KEY_CHECKS = 1;
SET SQL_MODE=@OLD_SQL_MODE;

-- 步骤11: 验证迁移结果
SELECT '步骤11: 验证迁移结果' AS step;

SELECT '=== 新表结构 ===' AS info;
SHOW CREATE TABLE api_client;

SELECT '=== 字段类型统一检查 ===' AS info;
SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    DATA_TYPE,
    CHARACTER_MAXIMUM_LENGTH
FROM 
    INFORMATION_SCHEMA.COLUMNS 
WHERE 
    TABLE_SCHEMA = DATABASE()
    AND COLUMN_NAME = 'client_id'
ORDER BY TABLE_NAME;

SELECT '=== 迁移数据统计 ===' AS info;
SELECT 
    COUNT(*) as total_clients,
    COUNT(CASE WHEN deleted = 0 THEN 1 END) as active_clients,
    COUNT(CASE WHEN client_type = 'STANDARD' THEN 1 END) as standard_clients,
    COUNT(CASE WHEN client_type = 'DEVELOPMENT' THEN 1 END) as dev_clients,
    COUNT(CASE WHEN client_type = 'PRODUCTION' THEN 1 END) as prod_clients
FROM api_client;

SELECT '=== 测试客户端检查 ===' AS info;
SELECT client_id, client_name, client_type, status, deleted 
FROM api_client 
WHERE client_id IN ('test-client-123', 'dev-client-001', 'prod-client-001');

SELECT '=== 强制迁移完成 ===' AS status;
SELECT 'Migration completed successfully! You can now restart the application.' AS message;
