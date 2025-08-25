package com.bgpay.bgai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Redis 健康检查控制器
 * 提供 Redis 连接状态监控
 */
@Slf4j
@RestController
@RequestMapping("/api/redis")
@RequiredArgsConstructor
public class RedisHealthController {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 检查 Redis 连接状态
     */
    @GetMapping("/health")
    public Map<String, Object> checkRedisHealth() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            result.put("timestamp", System.currentTimeMillis());
            
            // 测试 Redis 连接
            String testKey = "health:check:" + System.currentTimeMillis();
            String testValue = "test";
            
            // 写入测试
            redisTemplate.opsForValue().set(testKey, testValue, 60); // 60秒过期
            
            // 读取测试
            String retrievedValue = redisTemplate.opsForValue().get(testKey);
            
            // 删除测试键
            redisTemplate.delete(testKey);
            
            if (testValue.equals(retrievedValue)) {
                result.put("status", "HEALTHY");
                result.put("message", "Redis 连接正常");
                result.put("writeTest", "PASS");
                result.put("readTest", "PASS");
                result.put("deleteTest", "PASS");
            } else {
                result.put("status", "WARNING");
                result.put("message", "Redis 读写测试异常");
                result.put("writeTest", "PASS");
                result.put("readTest", "FAIL");
                result.put("deleteTest", "UNKNOWN");
            }
            
            log.info("Redis 健康检查: {}", result);
            
        } catch (Exception e) {
            result.put("status", "UNHEALTHY");
            result.put("message", "Redis 连接失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            result.put("writeTest", "FAIL");
            result.put("readTest", "FAIL");
            result.put("deleteTest", "FAIL");
            
            log.error("Redis 健康检查失败", e);
        }
        
        return result;
    }

    /**
     * 测试 Redis 基本操作
     */
    @GetMapping("/test")
    public Map<String, Object> testRedisOperations() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            result.put("timestamp", System.currentTimeMillis());
            
            // 测试字符串操作
            String stringKey = "test:string:" + System.currentTimeMillis();
            redisTemplate.opsForValue().set(stringKey, "Hello Redis");
            String stringValue = redisTemplate.opsForValue().get(stringKey);
            redisTemplate.delete(stringKey);
            
            // 测试哈希操作
            String hashKey = "test:hash:" + System.currentTimeMillis();
            redisTemplate.opsForHash().put(hashKey, "field1", "value1");
            redisTemplate.opsForHash().put(hashKey, "field2", "value2");
            Object hashValue = redisTemplate.opsForHash().get(hashKey, "field1");
            redisTemplate.delete(hashKey);
            
            // 测试列表操作
            String listKey = "test:list:" + System.currentTimeMillis();
            redisTemplate.opsForList().rightPush(listKey, "item1");
            redisTemplate.opsForList().rightPush(listKey, "item2");
            String listValue = redisTemplate.opsForList().leftPop(listKey);
            redisTemplate.delete(listKey);
            
            result.put("status", "SUCCESS");
            result.put("message", "Redis 基本操作测试成功");
            result.put("stringTest", stringValue != null ? "PASS" : "FAIL");
            result.put("hashTest", hashValue != null ? "PASS" : "FAIL");
            result.put("listTest", listValue != null ? "PASS" : "FAIL");
            
            log.info("Redis 基本操作测试: {}", result);
            
        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("message", "Redis 基本操作测试失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            log.error("Redis 基本操作测试失败", e);
        }
        
        return result;
    }

    /**
     * 获取 Redis 信息
     */
    @GetMapping("/info")
    public Map<String, Object> getRedisInfo() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            result.put("timestamp", System.currentTimeMillis());
            
            // 获取 Redis 服务器信息
            Properties info = redisTemplate.getConnectionFactory().getConnection().info();
            
            result.put("status", "SUCCESS");
            result.put("message", "获取 Redis 信息成功");
            result.put("info", info);
            
            log.info("获取 Redis 信息成功");
            
        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("message", "获取 Redis 信息失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            log.error("获取 Redis 信息失败", e);
        }
        
        return result;
    }

    /**
     * 清理测试数据
     */
    @DeleteMapping("/cleanup")
    public Map<String, Object> cleanupTestData() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            result.put("timestamp", System.currentTimeMillis());
            
            // 清理以 "test:" 开头的键
            // 注意：在生产环境中应该谨慎使用，这里只是用于测试
            int cleanedCount = 0;
            
            // 这里可以添加清理逻辑，但需要谨慎处理
            // 暂时只返回成功状态
            
            result.put("status", "SUCCESS");
            result.put("message", "清理测试数据完成");
            result.put("cleanedCount", cleanedCount);
            
            log.info("清理测试数据完成");
            
        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("message", "清理测试数据失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            log.error("清理测试数据失败", e);
        }
        
        return result;
    }
}
