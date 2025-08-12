package com.yue.aiAgent.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yue.aiAgent.entity.AiAgent;
import com.yue.aiAgent.service.AiAgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * AI代理控制器
 * 
 * @author yue
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/aiAgent")
@RequiredArgsConstructor
public class AiAgentController {

    private final AiAgentService aiAgentService;

    /**
     * 分页查询AI代理列表
     */
    @GetMapping("/list")
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type) {
        
        Page<AiAgent> page = new Page<>(current, size);
        LambdaQueryWrapper<AiAgent> wrapper = new LambdaQueryWrapper<>();
        
        if (name != null && !name.trim().isEmpty()) {
            wrapper.like(AiAgent::getName, name);
        }
        if (type != null && !type.trim().isEmpty()) {
            wrapper.eq(AiAgent::getType, type);
        }
        
        wrapper.orderByDesc(AiAgent::getCreateTime);
        
        Page<AiAgent> result = aiAgentService.page(page, wrapper);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "查询成功");
        response.put("data", result);
        
        return response;
    }

    /**
     * 根据ID查询AI代理
     */
    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        AiAgent agent = aiAgentService.getById(id);
        
        Map<String, Object> response = new HashMap<>();
        if (agent != null) {
            response.put("code", 200);
            response.put("message", "查询成功");
            response.put("data", agent);
        } else {
            response.put("code", 404);
            response.put("message", "AI代理不存在");
        }
        
        return response;
    }

    /**
     * 新增AI代理
     */
    @PostMapping
    public Map<String, Object> add(@RequestBody AiAgent aiAgent) {
        aiAgent.setCreateTime(LocalDateTime.now());
        aiAgent.setUpdateTime(LocalDateTime.now());
        
        boolean success = aiAgentService.save(aiAgent);
        
        Map<String, Object> response = new HashMap<>();
        if (success) {
            response.put("code", 200);
            response.put("message", "新增成功");
            response.put("data", aiAgent);
        } else {
            response.put("code", 500);
            response.put("message", "新增失败");
        }
        
        return response;
    }

    /**
     * 更新AI代理
     */
    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody AiAgent aiAgent) {
        aiAgent.setId(id);
        aiAgent.setUpdateTime(LocalDateTime.now());
        
        boolean success = aiAgentService.updateById(aiAgent);
        
        Map<String, Object> response = new HashMap<>();
        if (success) {
            response.put("code", 200);
            response.put("message", "更新成功");
        } else {
            response.put("code", 500);
            response.put("message", "更新失败");
        }
        
        return response;
    }

    /**
     * 删除AI代理
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        boolean success = aiAgentService.removeById(id);
        
        Map<String, Object> response = new HashMap<>();
        if (success) {
            response.put("code", 200);
            response.put("message", "删除成功");
        } else {
            response.put("code", 500);
            response.put("message", "删除失败");
        }
        
        return response;
    }

    /**
     * 测试AI代理连接
     */
    @PostMapping("/{id}/test")
    public Map<String, Object> testConnection(@PathVariable Long id) {
        boolean success = aiAgentService.testConnection(id);
        
        Map<String, Object> response = new HashMap<>();
        if (success) {
            response.put("code", 200);
            response.put("message", "连接测试成功");
        } else {
            response.put("code", 500);
            response.put("message", "连接测试失败");
        }
        
        return response;
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "AI代理服务运行正常");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "aiAgent-service");
        
        return response;
    }
}
