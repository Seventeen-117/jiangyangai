package com.signature.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 配置管理工具类
 */
@Slf4j
public class ConfigUtils {

    private static final AntPathMatcher pathMatcher = new AntPathMatcher();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 检查路径是否匹配模式
     */
    public static boolean isPathMatch(String path, String pattern) {
        if (!StringUtils.hasText(path) || !StringUtils.hasText(pattern)) {
            return false;
        }
        return pathMatcher.match(pattern, path);
    }

    /**
     * 检查路径是否匹配多个模式中的任意一个
     */
    public static boolean isPathMatchAny(String path, List<String> patterns) {
        if (!StringUtils.hasText(path) || patterns == null || patterns.isEmpty()) {
            return false;
        }
        return patterns.stream().anyMatch(pattern -> isPathMatch(path, pattern));
    }

    /**
     * 检查路径是否匹配所有模式
     */
    public static boolean isPathMatchAll(String path, List<String> patterns) {
        if (!StringUtils.hasText(path) || patterns == null || patterns.isEmpty()) {
            return false;
        }
        return patterns.stream().allMatch(pattern -> isPathMatch(path, pattern));
    }

    /**
     * 验证路径模式格式
     */
    public static boolean isValidPathPattern(String pattern) {
        if (!StringUtils.hasText(pattern)) {
            return false;
        }
        
        // 检查是否包含有效的通配符
        if (pattern.contains("*") || pattern.contains("?")) {
            // 检查通配符使用是否合理
            if (pattern.contains("**") && pattern.contains("**/**")) {
                return false; // 避免过度复杂的模式
            }
        }
        
        return true;
    }

    /**
     * 验证HTTP方法格式
     */
    public static boolean isValidHttpMethods(String httpMethods) {
        if (!StringUtils.hasText(httpMethods)) {
            return true; // 空值表示所有方法
        }
        
        String[] methods = httpMethods.split(",");
        Set<String> validMethods = Set.of("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS");
        
        for (String method : methods) {
            String trimmedMethod = method.trim().toUpperCase();
            if (!validMethods.contains(trimmedMethod)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * 检查HTTP方法是否匹配
     */
    public static boolean isHttpMethodMatch(String requestMethod, String configuredMethods) {
        if (!StringUtils.hasText(requestMethod) || !StringUtils.hasText(configuredMethods)) {
            return true; // 如果配置为空，表示匹配所有方法
        }
        
        String[] methods = configuredMethods.split(",");
        String upperRequestMethod = requestMethod.toUpperCase();
        
        for (String method : methods) {
            if (upperRequestMethod.equals(method.trim().toUpperCase())) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 验证JSON配置格式
     */
    public static boolean isValidJsonConfig(String jsonConfig) {
        if (!StringUtils.hasText(jsonConfig)) {
            return true; // 空值也是有效的
        }
        
        try {
            objectMapper.readTree(jsonConfig);
            return true;
        } catch (JsonProcessingException e) {
            log.warn("JSON配置格式无效: {}", jsonConfig, e);
            return false;
        }
    }

    /**
     * 解析JSON配置为Map
     */
    public static Map<String, Object> parseJsonConfig(String jsonConfig) {
        if (!StringUtils.hasText(jsonConfig)) {
            return new HashMap<>();
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = objectMapper.readValue(jsonConfig, Map.class);
            return config;
        } catch (JsonProcessingException e) {
            log.error("解析JSON配置失败: {}", jsonConfig, e);
            return new HashMap<>();
        }
    }

    /**
     * 将Map转换为JSON字符串
     */
    public static String toJsonString(Map<String, Object> config) {
        if (config == null || config.isEmpty()) {
            return "{}";
        }
        
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            log.error("转换为JSON字符串失败: {}", config, e);
            return "{}";
        }
    }

    /**
     * 验证服务编码格式
     */
    public static boolean isValidServiceCode(String serviceCode) {
        if (!StringUtils.hasText(serviceCode)) {
            return false;
        }
        
        // 服务编码格式：字母、数字、下划线、连字符，长度3-50
        return Pattern.matches("^[a-zA-Z0-9_-]{3,50}$", serviceCode);
    }

    /**
     * 验证规则编码格式
     */
    public static boolean isValidRuleCode(String ruleCode) {
        if (!StringUtils.hasText(ruleCode)) {
            return false;
        }
        
        // 规则编码格式：字母、数字、下划线，长度3-50
        return Pattern.matches("^[a-zA-Z0-9_]{3,50}$", ruleCode);
    }

    /**
     * 验证缓存键格式
     */
    public static boolean isValidCacheKey(String cacheKey) {
        if (!StringUtils.hasText(cacheKey)) {
            return false;
        }
        
        // 缓存键格式：字母、数字、下划线、连字符、点号，长度3-100
        return Pattern.matches("^[a-zA-Z0-9_.-]{3,100}$", cacheKey);
    }

    /**
     * 验证分类编码格式
     */
    public static boolean isValidCategoryCode(String categoryCode) {
        if (!StringUtils.hasText(categoryCode)) {
            return false;
        }
        
        // 分类编码格式：字母、数字、下划线，长度3-50
        return Pattern.matches("^[a-zA-Z0-9_]{3,50}$", categoryCode);
    }

    /**
     * 生成唯一的配置ID
     */
    public static String generateConfigId(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 检查配置是否过期
     */
    public static boolean isConfigExpired(Date configTime, int expireSeconds) {
        if (configTime == null) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        long configTimeMillis = configTime.getTime();
        long expireTimeMillis = configTimeMillis + (expireSeconds * 1000L);
        
        return currentTime > expireTimeMillis;
    }

    /**
     * 计算配置的剩余有效期（秒）
     */
    public static long getConfigRemainingSeconds(Date configTime, int expireSeconds) {
        if (configTime == null) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis();
        long configTimeMillis = configTime.getTime();
        long expireTimeMillis = configTimeMillis + (expireSeconds * 1000L);
        
        long remaining = expireTimeMillis - currentTime;
        return Math.max(0, remaining / 1000);
    }

    /**
     * 格式化配置描述
     */
    public static String formatConfigDescription(String description, int maxLength) {
        if (!StringUtils.hasText(description)) {
            return "";
        }
        
        if (description.length() <= maxLength) {
            return description;
        }
        
        return description.substring(0, maxLength - 3) + "...";
    }

    /**
     * 清理配置数据
     */
    public static Map<String, Object> cleanConfigData(Map<String, Object> data) {
        if (data == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> cleaned = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value != null) {
                if (value instanceof String) {
                    String strValue = (String) value;
                    if (StringUtils.hasText(strValue)) {
                        cleaned.put(key, strValue.trim());
                    }
                } else {
                    cleaned.put(key, value);
                }
            }
        }
        
        return cleaned;
    }

    /**
     * 验证配置数据的完整性
     */
    public static List<String> validateConfigData(Map<String, Object> data, Set<String> requiredFields) {
        List<String> errors = new ArrayList<>();
        
        if (data == null) {
            errors.add("配置数据不能为空");
            return errors;
        }
        
        for (String field : requiredFields) {
            if (!data.containsKey(field) || data.get(field) == null) {
                errors.add("必填字段缺失: " + field);
            }
        }
        
        return errors;
    }

    /**
     * 获取配置的默认值
     */
    public static Map<String, Object> getDefaultConfigValues() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("status", 1);
        defaults.put("sortOrder", 0);
        defaults.put("enabled", 1);
        defaults.put("strictMode", 0);
        defaults.put("timeoutMs", 5000);
        defaults.put("retryCount", 3);
        defaults.put("retryIntervalMs", 1000);
        defaults.put("expireSeconds", 3600);
        defaults.put("maxSize", 1000);
        return defaults;
    }
}
