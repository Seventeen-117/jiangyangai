package com.signature.controller;

import com.signature.service.DynamicConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置测试控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/config/test")
public class ConfigTestController {

    @Autowired
    private DynamicConfigService dynamicConfigService;

    /**
     * 测试路径配置
     */
    @GetMapping("/paths")
    public Map<String, Object> testPaths() {
        Map<String, Object> result = new HashMap<>();
        
        // 获取排除路径
        List<String> excludedPaths = dynamicConfigService.getExcludedPaths();
        result.put("excludedPaths", excludedPaths);
        
        // 获取严格验证路径
        List<String> strictPaths = dynamicConfigService.getStrictValidationPaths();
        result.put("strictPaths", strictPaths);
        
        // 获取内部路径
        List<String> internalPaths = dynamicConfigService.getInternalPaths();
        result.put("internalPaths", internalPaths);
        
        // 测试特定路径
        String testPath = "/api/sso/admin/user";
        boolean isExcluded = dynamicConfigService.isPathMatchType(testPath, "EXCLUDED");
        boolean isStrict = dynamicConfigService.isPathMatchType(testPath, "STRICT");
        boolean isInternal = dynamicConfigService.isPathMatchType(testPath, "INTERNAL");
        
        result.put("testPath", testPath);
        result.put("isExcluded", isExcluded);
        result.put("isStrict", isStrict);
        result.put("isInternal", isInternal);
        
        return result;
    }

    /**
     * 测试特定路径匹配
     */
    @GetMapping("/path-match")
    public Map<String, Object> testPathMatch(@RequestParam String path) {
        Map<String, Object> result = new HashMap<>();
        
        result.put("path", path);
        result.put("isExcluded", dynamicConfigService.isPathMatchType(path, "EXCLUDED"));
        result.put("isStrict", dynamicConfigService.isPathMatchType(path, "STRICT"));
        result.put("isInternal", dynamicConfigService.isPathMatchType(path, "INTERNAL"));
        
        return result;
    }

    /**
     * 刷新配置缓存
     */
    @GetMapping("/refresh")
    public Map<String, Object> refreshConfig() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            dynamicConfigService.refreshConfigCache();
            result.put("success", true);
            result.put("message", "配置缓存刷新成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "配置缓存刷新失败: " + e.getMessage());
            log.error("刷新配置缓存失败", e);
        }
        
        return result;
    }
}
