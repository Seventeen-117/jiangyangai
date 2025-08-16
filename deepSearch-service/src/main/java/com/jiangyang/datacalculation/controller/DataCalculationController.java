package com.jiangyang.datacalculation.controller;

import com.jiangyang.datacalculation.model.CalculationRequest;
import com.jiangyang.datacalculation.model.CalculationResponse;
import com.jiangyang.datacalculation.service.DataCalculationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据计算服务控制器
 * 
 * @author jiangyang
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/calculation")
@Validated
public class DataCalculationController {

    @Autowired
    private DataCalculationService dataCalculationService;

    /**
     * 执行数据计算（同步）
     * 
     * @param request 计算请求
     * @return 计算响应
     */
    @PostMapping("/execute")
    public ResponseEntity<CalculationResponse> executeCalculation(@Valid @RequestBody CalculationRequest request) {
        log.info("收到同步计算请求，请求ID: {}, 业务类型: {}", request.getRequestId(), request.getBusinessType());
        
        try {
            CalculationResponse response = dataCalculationService.executeCalculation(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("执行同步计算失败，请求ID: {}, 错误: {}", request.getRequestId(), e.getMessage(), e);
            
            CalculationResponse errorResponse = new CalculationResponse();
            errorResponse.setCode(500);
            errorResponse.setMessage("计算执行失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 执行数据计算（异步）
     * 
     * @param request 计算请求
     * @return 任务ID
     */
    @PostMapping("/execute-async")
    public ResponseEntity<Map<String, Object>> executeCalculationAsync(@Valid @RequestBody CalculationRequest request) {
        log.info("收到异步计算请求，请求ID: {}, 业务类型: {}", request.getRequestId(), request.getBusinessType());
        
        try {
            String taskId = dataCalculationService.executeCalculationAsync(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "异步计算任务已创建");
            response.put("taskId", taskId);
            response.put("requestId", request.getRequestId());
            response.put("status", "PENDING");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("创建异步计算任务失败，请求ID: {}, 错误: {}", request.getRequestId(), e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "创建异步计算任务失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 获取计算状态
     * 
     * @param requestId 请求ID
     * @return 计算状态
     */
    @GetMapping("/status/{requestId}")
    public ResponseEntity<CalculationResponse> getCalculationStatus(@PathVariable @NotBlank String requestId) {
        log.debug("查询计算状态，请求ID: {}", requestId);
        
        try {
            CalculationResponse response = dataCalculationService.getCalculationStatus(requestId);
            
            if (response != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("查询计算状态失败，请求ID: {}, 错误: {}", requestId, e.getMessage(), e);
            
            CalculationResponse errorResponse = new CalculationResponse();
            errorResponse.setCode(500);
            errorResponse.setMessage("查询计算状态失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 取消计算任务
     * 
     * @param requestId 请求ID
     * @return 取消结果
     */
    @DeleteMapping("/cancel/{requestId}")
    public ResponseEntity<Map<String, Object>> cancelCalculation(@PathVariable @NotBlank String requestId) {
        log.info("取消计算任务，请求ID: {}", requestId);
        
        try {
            boolean success = dataCalculationService.cancelCalculation(requestId);
            
            Map<String, Object> response = new HashMap<>();
            if (success) {
                response.put("code", 200);
                response.put("message", "计算任务已取消");
                response.put("requestId", requestId);
                return ResponseEntity.ok(response);
            } else {
                response.put("code", 400);
                response.put("message", "取消计算任务失败");
                response.put("requestId", requestId);
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("取消计算任务失败，请求ID: {}, 错误: {}", requestId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "取消计算任务失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 健康检查接口
     * 
     * @return 健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "deepSearch-service");
        health.put("timestamp", System.currentTimeMillis());
        health.put("version", "1.0.0");
        
        return ResponseEntity.ok(health);
    }

    /**
     * 服务信息接口
     * 
     * @return 服务信息
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getServiceInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("serviceName", "deepSearch-service");
        info.put("description", "基础数据计算服务");
        info.put("version", "1.0.0");
        info.put("author", "jiangyang");
        info.put("features", new String[]{
            "自动逻辑判断",
            "逻辑流程图生成",
            "BGAI服务集成",
            "数据计算执行",
            "异步任务支持",
            "状态查询",
            "任务取消"
        });
        
        return ResponseEntity.ok(info);
    }
}
