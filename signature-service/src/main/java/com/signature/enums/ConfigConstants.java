package com.signature.enums;

/**
 * 配置管理常量类
 */
public class ConfigConstants {

    // ==================== 路径类型常量 ====================
    
    /**
     * 排除路径类型
     */
    public static final String PATH_TYPE_EXCLUDED = "EXCLUDED";
    
    /**
     * 严格验证路径类型
     */
    public static final String PATH_TYPE_STRICT = "STRICT";
    
    /**
     * 内部路径类型
     */
    public static final String PATH_TYPE_INTERNAL = "INTERNAL";

    // ==================== 服务类型常量 ====================
    
    /**
     * 微服务类型
     */
    public static final String SERVICE_TYPE_MICROSERVICE = "MICROSERVICE";
    
    /**
     * 网关类型
     */
    public static final String SERVICE_TYPE_GATEWAY = "GATEWAY";
    
    /**
     * 外部服务类型
     */
    public static final String SERVICE_TYPE_EXTERNAL = "EXTERNAL";

    // ==================== 规则类型常量 ====================
    
    /**
     * 签名验证规则类型
     */
    public static final String RULE_TYPE_SIGNATURE = "SIGNATURE";
    
    /**
     * API密钥验证规则类型
     */
    public static final String RULE_TYPE_API_KEY = "API_KEY";
    
    /**
     * 认证验证规则类型
     */
    public static final String RULE_TYPE_AUTHENTICATION = "AUTHENTICATION";

    // ==================== 缓存类型常量 ====================
    
    /**
     * Redis缓存类型
     */
    public static final String CACHE_TYPE_REDIS = "REDIS";
    
    /**
     * 本地缓存类型
     */
    public static final String CACHE_TYPE_LOCAL = "LOCAL";

    // ==================== 状态常量 ====================
    
    /**
     * 启用状态
     */
    public static final Integer STATUS_ENABLED = 1;
    
    /**
     * 禁用状态
     */
    public static final Integer STATUS_DISABLED = 0;

    // ==================== 默认值常量 ====================
    
    /**
     * 默认排序顺序
     */
    public static final Integer DEFAULT_SORT_ORDER = 0;
    
    /**
     * 默认超时时间（毫秒）
     */
    public static final Integer DEFAULT_TIMEOUT_MS = 5000;
    
    /**
     * 默认重试次数
     */
    public static final Integer DEFAULT_RETRY_COUNT = 3;
    
    /**
     * 默认重试间隔（毫秒）
     */
    public static final Integer DEFAULT_RETRY_INTERVAL_MS = 1000;
    
    /**
     * 默认过期时间（秒）
     */
    public static final Integer DEFAULT_EXPIRE_SECONDS = 3600;
    
    /**
     * 默认最大缓存数量
     */
    public static final Integer DEFAULT_MAX_SIZE = 1000;

    // ==================== 配置键常量 ====================
    
    /**
     * 应用密钥缓存键
     */
    public static final String CACHE_KEY_APP_SECRET = "app_secret_cache";
    
    /**
     * Nonce缓存键
     */
    public static final String CACHE_KEY_NONCE = "nonce_cache";
    
    /**
     * API密钥缓存键
     */
    public static final String CACHE_KEY_API_KEY = "api_key_cache";
    
    /**
     * 用户会话缓存键
     */
    public static final String CACHE_KEY_USER_SESSION = "user_session_cache";

    // ==================== 规则编码常量 ====================
    
    /**
     * API密钥验证规则编码
     */
    public static final String RULE_CODE_API_KEY_VALIDATION = "API_KEY_VALIDATION";
    
    /**
     * 签名验证规则编码
     */
    public static final String RULE_CODE_SIGNATURE_VALIDATION = "SIGNATURE_VALIDATION";
    
    /**
     * 认证验证规则编码
     */
    public static final String RULE_CODE_AUTHENTICATION_VALIDATION = "AUTHENTICATION_VALIDATION";

    // ==================== 分类编码常量 ====================
    
    /**
     * API密钥配置分类编码
     */
    public static final String CATEGORY_CODE_API_KEY = "API_KEY";
    
    /**
     * 签名验证配置分类编码
     */
    public static final String CATEGORY_CODE_SIGNATURE = "SIGNATURE";
    
    /**
     * 认证配置分类编码
     */
    public static final String CATEGORY_CODE_AUTHENTICATION = "AUTHENTICATION";
    
    /**
     * 路径配置分类编码
     */
    public static final String CATEGORY_CODE_PATH = "PATH";
    
    /**
     * 缓存配置分类编码
     */
    public static final String CATEGORY_CODE_CACHE = "CACHE";

    // ==================== 服务编码常量 ====================
    
    /**
     * bgai服务编码
     */
    public static final String SERVICE_CODE_BGAI = "bgai-service";
    
    /**
     * 网关服务编码
     */
    public static final String SERVICE_CODE_GATEWAY = "gateway-service";
    
    /**
     * 签名服务编码
     */
    public static final String SERVICE_CODE_SIGNATURE = "signature-service";
    
    /**
     * bgtech-ai服务编码
     */
    public static final String SERVICE_CODE_BGTECH_AI = "bgtech-ai";

    // ==================== HTTP方法常量 ====================
    
    /**
     * GET方法
     */
    public static final String HTTP_METHOD_GET = "GET";
    
    /**
     * POST方法
     */
    public static final String HTTP_METHOD_POST = "POST";
    
    /**
     * PUT方法
     */
    public static final String HTTP_METHOD_PUT = "PUT";
    
    /**
     * DELETE方法
     */
    public static final String HTTP_METHOD_DELETE = "DELETE";
    
    /**
     * PATCH方法
     */
    public static final String HTTP_METHOD_PATCH = "PATCH";
    
    /**
     * HEAD方法
     */
    public static final String HTTP_METHOD_HEAD = "HEAD";
    
    /**
     * OPTIONS方法
     */
    public static final String HTTP_METHOD_OPTIONS = "OPTIONS";

    // ==================== 表名常量 ====================
    
    /**
     * 配置分类表名
     */
    public static final String TABLE_CONFIG_CATEGORY = "config_category";
    
    /**
     * 路径配置表名
     */
    public static final String TABLE_PATH_CONFIG = "path_config";
    
    /**
     * 内部服务配置表名
     */
    public static final String TABLE_INTERNAL_SERVICE_CONFIG = "internal_service_config";
    
    /**
     * 验证规则配置表名
     */
    public static final String TABLE_VALIDATION_RULE_CONFIG = "validation_rule_config";
    
    /**
     * 缓存配置表名
     */
    public static final String TABLE_CACHE_CONFIG = "cache_config";

    // ==================== 错误消息常量 ====================
    
    /**
     * 配置不存在错误消息
     */
    public static final String ERROR_CONFIG_NOT_FOUND = "配置不存在";
    
    /**
     * 配置编码已存在错误消息
     */
    public static final String ERROR_CONFIG_CODE_EXISTS = "配置编码已存在";
    
    /**
     * 配置数据无效错误消息
     */
    public static final String ERROR_CONFIG_INVALID = "配置数据无效";
    
    /**
     * 配置冲突错误消息
     */
    public static final String ERROR_CONFIG_CONFLICT = "配置冲突";
    
    /**
     * 配置依赖错误消息
     */
    public static final String ERROR_CONFIG_DEPENDENCY = "配置存在依赖关系，无法删除";

    // ==================== 成功消息常量 ====================
    
    /**
     * 配置创建成功消息
     */
    public static final String SUCCESS_CONFIG_CREATED = "配置创建成功";
    
    /**
     * 配置更新成功消息
     */
    public static final String SUCCESS_CONFIG_UPDATED = "配置更新成功";
    
    /**
     * 配置删除成功消息
     */
    public static final String SUCCESS_CONFIG_DELETED = "配置删除成功";
    
    /**
     * 配置缓存刷新成功消息
     */
    public static final String SUCCESS_CACHE_REFRESHED = "配置缓存刷新成功";

    // ==================== 验证规则常量 ====================
    
    /**
     * 签名算法常量
     */
    public static final String SIGNATURE_ALGORITHM_SHA256 = "SHA256";
    
    /**
     * 时间戳过期时间（秒）
     */
    public static final Integer TIMESTAMP_EXPIRE_SECONDS = 300;
    
    /**
     * Nonce缓存过期时间（秒）
     */
    public static final Integer NONCE_CACHE_EXPIRE_SECONDS = 1800;
    
    /**
     * 授权头名称
     */
    public static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    
    /**
     * Bearer前缀
     */
    public static final String BEARER_PREFIX = "Bearer ";
}
