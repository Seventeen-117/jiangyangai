package com.signature.utils;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * 路径匹配工具类
 * </p>
 *
 * @author signature-service
 * @since 2025-01-01
 */
public class PathMatcherUtil {

    private static final PathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 检查路径是否匹配指定的模式列表
     *
     * @param path 要检查的路径
     * @param patterns 模式列表
     * @return 是否匹配
     */
    public static boolean matchesAny(String path, String... patterns) {
        if (path == null || patterns == null) {
            return false;
        }
        return Arrays.stream(patterns).anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 检查路径是否匹配指定的模式列表
     *
     * @param path 要检查的路径
     * @param patterns 模式列表
     * @return 是否匹配
     */
    public static boolean matchesAny(String path, List<String> patterns) {
        if (path == null || patterns == null) {
            return false;
        }
        return patterns.stream()
                .filter(pattern -> pattern != null && !pattern.trim().isEmpty())
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 检查路径是否以指定的前缀开头
     *
     * @param path 要检查的路径
     * @param prefixes 前缀列表
     * @return 是否匹配
     */
    public static boolean startsWithAny(String path, String... prefixes) {
        if (path == null || prefixes == null) {
            return false;
        }
        return Arrays.stream(prefixes).anyMatch(prefix -> path.startsWith(prefix));
    }

    /**
     * 检查路径是否包含指定的字符串
     *
     * @param path 要检查的路径
     * @param substrings 子字符串列表
     * @return 是否包含
     */
    public static boolean containsAny(String path, String... substrings) {
        if (path == null || substrings == null) {
            return false;
        }
        return Arrays.stream(substrings).anyMatch(path::contains);
    }

    /**
     * 检查路径是否包含指定的字符串列表
     *
     * @param path 要检查的路径
     * @param substrings 子字符串列表
     * @return 是否包含
     */
    public static boolean containsAny(String path, List<String> substrings) {
        if (path == null || substrings == null) {
            return false;
        }
        return substrings.stream().anyMatch(path::contains);
    }

    /**
     * 检查路径是否等于指定的字符串
     *
     * @param path 要检查的路径
     * @param equals 相等字符串列表
     * @return 是否相等
     */
    public static boolean equalsAny(String path, String... equals) {
        if (path == null || equals == null) {
            return false;
        }
        return Arrays.stream(equals).anyMatch(path::equals);
    }

    /**
     * 检查路径是否等于指定的字符串列表
     *
     * @param path 要检查的路径
     * @param equals 相等字符串列表
     * @return 是否相等
     */
    public static boolean equalsAny(String path, List<String> equals) {
        if (path == null || equals == null) {
            return false;
        }
        return equals.stream().anyMatch(path::equals);
    }

    /**
     * 获取路径的根部分（第一个斜杠后的部分）
     *
     * @param path 完整路径
     * @return 根部分
     */
    public static String getRootPath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        
        // 移除开头的斜杠
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        
        // 找到第一个斜杠的位置
        int firstSlash = cleanPath.indexOf('/');
        if (firstSlash == -1) {
            return cleanPath;
        }
        
        return cleanPath.substring(0, firstSlash);
    }

    /**
     * 检查是否为API路径
     *
     * @param path 路径
     * @return 是否为API路径
     */
    public static boolean isApiPath(String path) {
        return path != null && path.startsWith("/api/");
    }

    /**
     * 检查是否为管理路径
     *
     * @param path 路径
     * @return 是否为管理路径
     */
    public static boolean isManagementPath(String path) {
        return path != null && (path.startsWith("/actuator") || 
                               path.startsWith("/health") || 
                               path.startsWith("/info") || 
                               path.startsWith("/metrics"));
    }

    /**
     * 检查是否为文档路径
     *
     * @param path 路径
     * @return 是否为文档路径
     */
    public static boolean isDocumentationPath(String path) {
        return path != null && (path.startsWith("/swagger-ui") || 
                               path.startsWith("/v3/api-docs"));
    }

    /**
     * 检查是否为静态资源路径
     *
     * @param path 路径
     * @return 是否为静态资源路径
     */
    public static boolean isStaticResourcePath(String path) {
        return path != null && (path.equals("/favicon.ico") || 
                               path.startsWith("/static/") || 
                               path.startsWith("/public/"));
    }
}
