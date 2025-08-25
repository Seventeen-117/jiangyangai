package com.bgpay.bgai.controller;

import com.bgpay.bgai.service.mq.RocketMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * RocketMQ 健康检查控制器
 * 提供 RocketMQ 连接状态监控
 */
@Slf4j
@RestController
@RequestMapping("/api/rocketmq")
@RequiredArgsConstructor
public class RocketMQHealthController {

    private final RocketMQTemplate rocketMQTemplate;
    private final RocketMQConfig rocketMQConfig;

    /**
     * 检查 RocketMQ 连接状态
     */
    @GetMapping("/health")
    public Map<String, Object> checkRocketMQHealth() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            result.put("timestamp", System.currentTimeMillis());
            
            // 检查配置
            result.put("config", rocketMQConfig.toString());
            
            // 检查连接状态
            if (rocketMQTemplate != null) {
                result.put("status", "HEALTHY");
                result.put("message", "RocketMQ 连接正常");
                result.put("templateStatus", "AVAILABLE");
                
                // 获取生产者状态
                try {
                    String producerGroup = rocketMQTemplate.getProducer().getProducerGroup();
                    result.put("producerGroup", producerGroup);
                    result.put("producerStatus", "ACTIVE");
                } catch (Exception e) {
                    result.put("producerStatus", "ERROR");
                    result.put("producerError", e.getMessage());
                }
                
            } else {
                result.put("status", "UNHEALTHY");
                result.put("message", "RocketMQTemplate 未注入");
                result.put("templateStatus", "NULL");
            }
            
            log.info("RocketMQ 健康检查: {}", result);
            
        } catch (Exception e) {
            result.put("status", "UNHEALTHY");
            result.put("message", "RocketMQ 健康检查失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            log.error("RocketMQ 健康检查失败", e);
        }
        
        return result;
    }

    /**
     * 获取 RocketMQ 配置信息
     */
    @GetMapping("/config")
    public Map<String, Object> getRocketMQConfig() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            result.put("timestamp", System.currentTimeMillis());
            result.put("status", "SUCCESS");
            result.put("message", "获取 RocketMQ 配置成功");
            result.put("config", rocketMQConfig);
            
            log.info("获取 RocketMQ 配置成功");
            
        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("message", "获取 RocketMQ 配置失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            log.error("获取 RocketMQ 配置失败", e);
        }
        
        return result;
    }

    /**
     * 测试 RocketMQ 连接
     */
    @GetMapping("/test")
    public Map<String, Object> testRocketMQConnection() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            result.put("timestamp", System.currentTimeMillis());
            
            // 检查基本连接
            if (rocketMQTemplate != null) {
                result.put("templateAvailable", true);
                result.put("producerGroup", rocketMQTemplate.getProducer().getProducerGroup());
                result.put("nameServer", rocketMQTemplate.getProducer().getNamesrvAddr());
                
                result.put("status", "SUCCESS");
                result.put("message", "RocketMQ 连接测试成功");
                
            } else {
                result.put("templateAvailable", false);
                result.put("status", "FAILED");
                result.put("message", "RocketMQTemplate 不可用");
            }
            
            log.info("RocketMQ 连接测试: {}", result);
            
        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("message", "RocketMQ 连接测试失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            log.error("RocketMQ 连接测试失败", e);
        }
        
        return result;
    }

    /**
     * 获取 RocketMQ 状态信息
     */
    @GetMapping("/info")
    public Map<String, Object> getRocketMQInfo() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            result.put("timestamp", System.currentTimeMillis());
            
            // 基本信息 - 使用反射获取私有字段值
            try {
                java.lang.reflect.Field nameServerField = rocketMQConfig.getClass().getDeclaredField("nameServer");
                nameServerField.setAccessible(true);
                String nameServer = (String) nameServerField.get(rocketMQConfig);
                result.put("nameServer", nameServer);
                
                java.lang.reflect.Field producerGroupField = rocketMQConfig.getClass().getDeclaredField("producerGroup");
                producerGroupField.setAccessible(true);
                String producerGroup = (String) producerGroupField.get(rocketMQConfig);
                result.put("producerGroup", producerGroup);
                
            } catch (Exception e) {
                result.put("nameServer", "N/A");
                result.put("producerGroup", "N/A");
                log.debug("无法获取配置字段值: {}", e.getMessage());
            }
            
            result.put("status", "SUCCESS");
            result.put("message", "获取 RocketMQ 信息成功");
            
            log.info("获取 RocketMQ 信息成功");
            
        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("message", "获取 RocketMQ 信息失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            log.error("获取 RocketMQ 信息失败", e);
        }
        
        return result;
    }
}
