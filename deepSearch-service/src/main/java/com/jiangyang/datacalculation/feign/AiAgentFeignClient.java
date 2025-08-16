package com.jiangyang.datacalculation.feign;

import com.jiangyang.datacalculation.model.AiAgentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * AI代理服务Feign客户端
 * 
 * @author jiangyang
 * @since 1.0.0
 */
@FeignClient(name = "aiAgent-service", url = "${service.ai-agent.base-url}")
public interface AiAgentFeignClient {

    /**
     * 发送聊天消息，获取AI分析结果
     * 
     * @param request 请求参数
     * @return AI代理响应
     */
    @PostMapping("/api/aiChat/chat")
    AiAgentResponse chat(@RequestBody Map<String, String> request);

    /**
     * 快速聊天（GET方式）
     * 
     * @param message 消息内容
     * @param type 类型
     * @return AI代理响应
     */
    @PostMapping("/api/aiChat/quick")
    AiAgentResponse quickChat(@RequestParam("message") String message, 
                             @RequestParam("type") String type);

    /**
     * 流式聊天
     * 
     * @param request 请求参数
     * @return 流式响应
     */
    @PostMapping("/api/aiChat/stream")
    String streamChat(@RequestBody Map<String, String> request);
}
