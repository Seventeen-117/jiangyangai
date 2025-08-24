package com.jiangyang.messages.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务回调地址配置类
 * 用于配置各个消费者服务的回调地址，避免硬编码
 */
@Data
@Component
@ConfigurationProperties(prefix = "messages.service.callback")
public class ServiceCallbackConfig {

    /**
     * 服务回调地址映射
     * 格式：serviceName -> callbackUrl
     */
    private Map<String, String> urls = new HashMap<>();

    /**
     * 默认回调地址模板
     * 用于动态构建回调地址
     */
    private String defaultTemplate = "http://{host}:{port}/api/messages/consume";

    /**
     * 服务主机地址映射
     * 格式：serviceName -> host
     */
    private Map<String, String> hosts = new HashMap<>();

    /**
     * 服务端口映射
     * 格式：serviceName -> port
     */
    private Map<String, Integer> ports = new HashMap<>();

    /**
     * 获取服务的回调地址
     * 
     * @param serviceName 服务名称
     * @return 回调地址，如果未配置则返回null
     */
    public String getCallbackUrl(String serviceName) {
        // 优先从直接配置的URL映射中获取
        if (urls.containsKey(serviceName)) {
            return urls.get(serviceName);
        }

        // 如果配置了主机和端口，使用模板动态构建
        if (hosts.containsKey(serviceName) && ports.containsKey(serviceName)) {
            String host = hosts.get(serviceName);
            Integer port = ports.get(serviceName);
            return defaultTemplate
                    .replace("{host}", host)
                    .replace("{port}", String.valueOf(port));
        }

        return null;
    }

    /**
     * 检查服务是否配置了回调地址
     * 
     * @param serviceName 服务名称
     * @return 是否已配置
     */
    public boolean hasCallbackUrl(String serviceName) {
        return urls.containsKey(serviceName) || 
               (hosts.containsKey(serviceName) && ports.containsKey(serviceName));
    }

    /**
     * 获取所有已配置的服务名称
     * 
     * @return 服务名称集合
     */
    public java.util.Set<String> getConfiguredServices() {
        java.util.Set<String> services = new java.util.HashSet<>();
        services.addAll(urls.keySet());
        services.addAll(hosts.keySet());
        return services;
    }
}
