package com.bgpay.bgai.config;

import org.apache.dubbo.rpc.RpcException;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Dubbo异常处理配置
 * 
 * @author jiangyang
 */
@Configuration
@RestControllerAdvice
public class DubboExceptionConfig {

    /**
     * 处理Dubbo RPC异常
     */
    @ExceptionHandler(RpcException.class)
    public Map<String, Object> handleRpcException(RpcException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("code", "DUBBO_ERROR");
        
        switch (e.getCode()) {
            case RpcException.UNKNOWN_EXCEPTION:
                result.put("message", "Dubbo服务调用未知异常");
                break;
            case RpcException.NETWORK_EXCEPTION:
                result.put("message", "Dubbo网络异常，请检查网络连接");
                break;
            case RpcException.TIMEOUT_EXCEPTION:
                result.put("message", "Dubbo服务调用超时");
                break;
            case RpcException.BIZ_EXCEPTION:
                result.put("message", "Dubbo业务异常: " + e.getMessage());
                break;
            case RpcException.FORBIDDEN_EXCEPTION:
                result.put("message", "Dubbo服务被禁止访问");
                break;
            case RpcException.SERIALIZATION_EXCEPTION:
                result.put("message", "Dubbo序列化异常");
                break;
            default:
                result.put("message", "Dubbo服务调用异常: " + e.getMessage());
        }
        
        result.put("error", e.getMessage());
        result.put("timestamp", System.currentTimeMillis());
        
        return result;
    }

    /**
     * 处理Dubbo相关的运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public Map<String, Object> handleRuntimeException(RuntimeException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("code", "RUNTIME_ERROR");
        
        if (e.getMessage() != null && e.getMessage().contains("Dubbo")) {
            result.put("message", "Dubbo服务调用失败: " + e.getMessage());
        } else {
            result.put("message", "运行时异常: " + e.getMessage());
        }
        
        result.put("error", e.getMessage());
        result.put("timestamp", System.currentTimeMillis());
        
        return result;
    }
}
