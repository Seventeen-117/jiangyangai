package com.jiangyang.messages.controller;

import com.jiangyang.messages.entity.MessageConsumerConfig;
import com.jiangyang.messages.service.AutoConsumeService;
import com.jiangyang.messages.service.MessageConsumerConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自动消费控制器
 * 提供自动消费服务的REST API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/messages/auto-consume")
public class AutoConsumeController {

    @Autowired
    private AutoConsumeService autoConsumeService;

    @Autowired
    private MessageConsumerConfigService messageConsumerConfigService;

    /**
     * 启动自动消费服务
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startAutoConsume() {
        try {
            autoConsumeService.startAutoConsume();
            return ResponseEntity.ok(createSuccessResponse("自动消费服务启动成功"));
        } catch (Exception e) {
            log.error("启动自动消费服务失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("启动自动消费服务失败: " + e.getMessage()));
        }
    }

    /**
     * 停止自动消费服务
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopAutoConsume() {
        try {
            autoConsumeService.stopAutoConsume();
            return ResponseEntity.ok(createSuccessResponse("自动消费服务停止成功"));
        } catch (Exception e) {
            log.error("停止自动消费服务失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("停止自动消费服务失败: " + e.getMessage()));
        }
    }

    /**
     * 重新加载消费配置
     */
    @PostMapping("/reload")
    public ResponseEntity<Map<String, Object>> reloadConsumeConfig() {
        try {
            autoConsumeService.reloadConsumeConfig();
            return ResponseEntity.ok(createSuccessResponse("消费配置重新加载成功"));
        } catch (Exception e) {
            log.error("重新加载消费配置失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("重新加载消费配置失败: " + e.getMessage()));
        }
    }

    /**
     * 根据服务名称启动消费
     */
    @PostMapping("/start/{serviceName}")
    public ResponseEntity<Map<String, Object>> startConsumeByService(@PathVariable String serviceName) {
        try {
            autoConsumeService.startConsumeByService(serviceName);
            return ResponseEntity.ok(createSuccessResponse("服务 " + serviceName + " 的消费启动成功"));
        } catch (Exception e) {
            log.error("启动服务 {} 的消费失败: {}", serviceName, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("启动消费失败: " + e.getMessage()));
        }
    }

    /**
     * 根据服务名称停止消费
     */
    @PostMapping("/stop/{serviceName}")
    public ResponseEntity<Map<String, Object>> stopConsumeByService(@PathVariable String serviceName) {
        try {
            autoConsumeService.stopConsumeByService(serviceName);
            return ResponseEntity.ok(createSuccessResponse("服务 " + serviceName + " 的消费停止成功"));
        } catch (Exception e) {
            log.error("停止服务 {} 的消费失败: {}", serviceName, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("停止消费失败: " + e.getMessage()));
        }
    }

    /**
     * 获取所有消费配置
     */
    @GetMapping("/configs")
    public ResponseEntity<Map<String, Object>> getAllConsumeConfigs() {
        try {
            List<MessageConsumerConfig> configs = autoConsumeService.getAllConsumeConfigs();
            return ResponseEntity.ok(createSuccessResponse("获取消费配置成功", configs));
        } catch (Exception e) {
            log.error("获取消费配置失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("获取消费配置失败: " + e.getMessage()));
        }
    }

    /**
     * 根据服务名称获取消费配置
     */
    @GetMapping("/configs/{serviceName}")
    public ResponseEntity<Map<String, Object>> getConsumeConfigsByService(@PathVariable String serviceName) {
        try {
            List<MessageConsumerConfig> configs = autoConsumeService.getConsumeConfigsByService(serviceName);
            return ResponseEntity.ok(createSuccessResponse("获取服务 " + serviceName + " 的消费配置成功", configs));
        } catch (Exception e) {
            log.error("获取服务 {} 的消费配置失败: {}", serviceName, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("获取消费配置失败: " + e.getMessage()));
        }
    }

    /**
     * 检查消费状态
     */
    @GetMapping("/status/{serviceName}")
    public ResponseEntity<Map<String, Object>> getConsumeStatus(@PathVariable String serviceName) {
        try {
            boolean isConsuming = autoConsumeService.isConsuming(serviceName);
            Map<String, Object> status = Map.of(
                "serviceName", serviceName,
                "isConsuming", isConsuming,
                "timestamp", System.currentTimeMillis()
            );
            return ResponseEntity.ok(createSuccessResponse("获取消费状态成功", status));
        } catch (Exception e) {
            log.error("获取服务 {} 的消费状态失败: {}", serviceName, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("获取消费状态失败: " + e.getMessage()));
        }
    }

    /**
     * 获取消费统计信息
     */
    @GetMapping("/statistics/{serviceName}")
    public ResponseEntity<Map<String, Object>> getConsumeStatistics(@PathVariable String serviceName) {
        try {
            Object statistics = autoConsumeService.getConsumeStatistics(serviceName);
            return ResponseEntity.ok(createSuccessResponse("获取消费统计信息成功", statistics));
        } catch (Exception e) {
            log.error("获取服务 {} 的消费统计信息失败: {}", serviceName, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("获取消费统计信息失败: " + e.getMessage()));
        }
    }

    /**
     * 保存消费配置
     */
    @PostMapping("/configs")
    public ResponseEntity<Map<String, Object>> saveConsumeConfig(@RequestBody MessageConsumerConfig config) {
        try {
            boolean success = messageConsumerConfigService.saveConfig(config);
            if (success) {
                return ResponseEntity.ok(createSuccessResponse("消费配置保存成功", config));
            } else {
                return ResponseEntity.internalServerError().body(createErrorResponse("消费配置保存失败"));
            }
        } catch (Exception e) {
            log.error("保存消费配置失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("保存消费配置失败: " + e.getMessage()));
        }
    }

    /**
     * 更新消费配置
     */
    @PutMapping("/configs/{id}")
    public ResponseEntity<Map<String, Object>> updateConsumeConfig(@PathVariable Long id, @RequestBody MessageConsumerConfig config) {
        try {
            config.setId(id);
            boolean success = messageConsumerConfigService.updateConfig(config);
            if (success) {
                return ResponseEntity.ok(createSuccessResponse("消费配置更新成功", config));
            } else {
                return ResponseEntity.internalServerError().body(createErrorResponse("消费配置更新失败"));
            }
        } catch (Exception e) {
            log.error("更新消费配置失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("更新消费配置失败: " + e.getMessage()));
        }
    }

    /**
     * 删除消费配置
     */
    @DeleteMapping("/configs/{id}")
    public ResponseEntity<Map<String, Object>> deleteConsumeConfig(@PathVariable Long id) {
        try {
            boolean success = messageConsumerConfigService.deleteConfig(id);
            if (success) {
                return ResponseEntity.ok(createSuccessResponse("消费配置删除成功"));
            } else {
                return ResponseEntity.internalServerError().body(createErrorResponse("消费配置删除失败"));
            }
        } catch (Exception e) {
            log.error("删除消费配置失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("删除消费配置失败: " + e.getMessage()));
        }
    }

    /**
     * 启用消费配置
     */
    @PostMapping("/configs/{id}/enable")
    public ResponseEntity<Map<String, Object>> enableConsumeConfig(@PathVariable Long id) {
        try {
            boolean success = messageConsumerConfigService.enableConfig(id);
            if (success) {
                return ResponseEntity.ok(createSuccessResponse("消费配置启用成功"));
            } else {
                return ResponseEntity.internalServerError().body(createErrorResponse("消费配置启用失败"));
            }
        } catch (Exception e) {
            log.error("启用消费配置失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("启用消费配置失败: " + e.getMessage()));
        }
    }

    /**
     * 禁用消费配置
     */
    @PostMapping("/configs/{id}/disable")
    public ResponseEntity<Map<String, Object>> disableConsumeConfig(@PathVariable Long id) {
        try {
            boolean success = messageConsumerConfigService.disableConfig(id);
            if (success) {
                return ResponseEntity.ok(createSuccessResponse("消费配置禁用成功"));
            } else {
                return ResponseEntity.internalServerError().body(createErrorResponse("消费配置禁用失败"));
            }
        } catch (Exception e) {
            log.error("禁用消费配置失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("禁用消费配置失败: " + e.getMessage()));
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Auto Consume Service");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * 创建成功响应
     */
    private Map<String, Object> createSuccessResponse(String message) {
        return createSuccessResponse(message, null);
    }

    /**
     * 创建成功响应
     */
    private Map<String, Object> createSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        if (data != null) {
            response.put("data", data);
        }
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}
