package com.signature.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * API 安全服务类
 * 处理API密钥验证、权限管理等安全相关业务逻辑
 */
@Slf4j
@Service
public class ApiSecurityService {

    @Value("${api.security.header-name:X-API-Key}")
    private String headerName;

    @Value("${api.security.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${api.security.rate-limit.default-limit:100}")
    private int defaultRateLimit;

    @Value("${api.security.rate-limit.burst-capacity:200}")
    private int burstCapacity;

    // 模拟API密钥存储
    private final Map<String, String> apiKeyToClientMap = new HashMap<>();
    private final Map<String, List<String>> clientPermissions = new HashMap<>();
    private final Map<String, Map<String, Object>> clientInfo = new HashMap<>();
    private final Set<String> revokedApiKeys = new HashSet<>();

    public ApiSecurityService() {
        // 初始化默认API密钥
        initializeDefaultApiKeys();
    }

    /**
     * 验证API密钥
     */
    public boolean verifyApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return false;
        }

        // 检查是否已被撤销
        if (revokedApiKeys.contains(apiKey)) {
            log.warn("API key has been revoked: {}", apiKey);
            return false;
        }

        // 检查API密钥是否存在
        boolean isValid = apiKeyToClientMap.containsKey(apiKey);
        if (isValid) {
            String clientId = apiKeyToClientMap.get(apiKey);
            log.debug("API key verified for client: {}", clientId);
        } else {
            log.warn("Invalid API key: {}", apiKey);
        }

        return isValid;
    }

    /**
     * 根据API密钥获取客户端ID
     */
    public String getClientIdByApiKey(String apiKey) {
        return apiKeyToClientMap.get(apiKey);
    }

    /**
     * 获取客户端权限
     */
    public Map<String, Object> getClientPermissions(String clientId) {
        Map<String, Object> permissions = new HashMap<>();
        List<String> permissionList = clientPermissions.getOrDefault(clientId, new ArrayList<>());
        
        permissions.put("permissions", permissionList);
        permissions.put("client_id", clientId);
        permissions.put("total_permissions", permissionList.size());
        
        return permissions;
    }

    /**
     * 检查权限
     */
    public boolean checkPermission(String apiKey, String permission, String resource) {
        if (!verifyApiKey(apiKey)) {
            return false;
        }

        String clientId = getClientIdByApiKey(apiKey);
        List<String> permissions = clientPermissions.getOrDefault(clientId, new ArrayList<>());
        
        // 检查具体权限
        String fullPermission = permission + ":" + resource;
        boolean hasPermission = permissions.contains(fullPermission) || permissions.contains(permission + ":*");
        
        log.debug("Permission check for client {}: {} -> {}", clientId, fullPermission, hasPermission);
        return hasPermission;
    }

    /**
     * 获取客户端信息
     */
    public Map<String, Object> getClientInfo(String clientId) {
        return clientInfo.get(clientId);
    }

    /**
     * 获取所有客户端
     */
    public Map<String, Object> getAllClients() {
        Map<String, Object> clients = new HashMap<>();
        
        for (String clientId : clientInfo.keySet()) {
            Map<String, Object> clientData = new HashMap<>(clientInfo.get(clientId));
            clientData.put("permissions", getClientPermissions(clientId));
            clientData.put("rate_limit", getRateLimitInfo(clientId));
            clients.put(clientId, clientData);
        }
        
        return clients;
    }

    /**
     * 创建API密钥
     */
    public Map<String, Object> createApiKey(String clientId, String description) {
        if (!clientInfo.containsKey(clientId)) {
            throw new RuntimeException("Client not found: " + clientId);
        }

        String apiKey = generateApiKey();
        apiKeyToClientMap.put(apiKey, clientId);
        
        Map<String, Object> apiKeyInfo = new HashMap<>();
        apiKeyInfo.put("api_key", apiKey);
        apiKeyInfo.put("client_id", clientId);
        apiKeyInfo.put("description", description);
        apiKeyInfo.put("created_at", System.currentTimeMillis());
        apiKeyInfo.put("status", "active");
        
        log.info("Created API key for client: {}", clientId);
        return apiKeyInfo;
    }

    /**
     * 撤销API密钥
     */
    public boolean revokeApiKey(String apiKey) {
        if (!apiKeyToClientMap.containsKey(apiKey)) {
            return false;
        }

        String clientId = apiKeyToClientMap.remove(apiKey);
        revokedApiKeys.add(apiKey);
        
        log.info("Revoked API key for client: {}", clientId);
        return true;
    }

    /**
     * 获取API使用统计
     */
    public Map<String, Object> getApiStats(String clientId, Integer days) {
        Map<String, Object> stats = new HashMap<>();
        
        // 模拟统计数据
        stats.put("total_requests", 1000);
        stats.put("successful_requests", 950);
        stats.put("failed_requests", 50);
        stats.put("average_response_time", 150);
        stats.put("peak_requests_per_minute", 25);
        stats.put("unique_ips", 15);
        stats.put("period_days", days);
        
        if (clientId != null) {
            stats.put("client_id", clientId);
        }
        
        return stats;
    }

    /**
     * 获取限流信息
     */
    public Map<String, Object> getRateLimitInfo(String clientId) {
        Map<String, Object> rateLimitInfo = new HashMap<>();
        
        rateLimitInfo.put("enabled", rateLimitEnabled);
        rateLimitInfo.put("limit_per_minute", defaultRateLimit);
        rateLimitInfo.put("burst_capacity", burstCapacity);
        rateLimitInfo.put("client_id", clientId);
        
        // 模拟当前使用情况
        rateLimitInfo.put("current_usage", 45);
        rateLimitInfo.put("remaining_requests", defaultRateLimit - 45);
        rateLimitInfo.put("reset_time", System.currentTimeMillis() + 60000); // 1分钟后重置
        
        return rateLimitInfo;
    }

    /**
     * 更新客户端权限
     */
    public boolean updateClientPermissions(String clientId, Map<String, Object> permissions) {
        if (!clientInfo.containsKey(clientId)) {
            return false;
        }

        List<String> permissionList = new ArrayList<>();
        if (permissions.containsKey("permissions")) {
            Object perms = permissions.get("permissions");
            if (perms instanceof List) {
                permissionList = (List<String>) perms;
            }
        }

        clientPermissions.put(clientId, permissionList);
        log.info("Updated permissions for client {}: {}", clientId, permissionList);
        return true;
    }

    /**
     * 初始化默认API密钥
     */
    private void initializeDefaultApiKeys() {
        // 客户端1
        String client1ApiKey = "804822262af64439aeab611143864948";
        String client1Id = "client-1";
        
        apiKeyToClientMap.put(client1ApiKey, client1Id);
        clientPermissions.put(client1Id, Arrays.asList(
            "read:user", "write:user", "read:chat", "write:chat"
        ));
        
        Map<String, Object> client1Info = new HashMap<>();
        client1Info.put("name", "Client 1");
        client1Info.put("description", "Full access client");
        client1Info.put("status", "active");
        client1Info.put("created_at", System.currentTimeMillis());
        client1Info.put("rate_limit", defaultRateLimit);
        clientInfo.put(client1Id, client1Info);

        // 客户端2
        String client2ApiKey = "fdac4b850ba74f8f86338d3c445a88f5";
        String client2Id = "client-2";
        
        apiKeyToClientMap.put(client2ApiKey, client2Id);
        clientPermissions.put(client2Id, Arrays.asList(
            "read:user", "read:chat"
        ));
        
        Map<String, Object> client2Info = new HashMap<>();
        client2Info.put("name", "Client 2");
        client2Info.put("description", "Read-only client");
        client2Info.put("status", "active");
        client2Info.put("created_at", System.currentTimeMillis());
        client2Info.put("rate_limit", defaultRateLimit);
        clientInfo.put(client2Id, client2Info);

        log.info("Initialized default API keys for {} clients", clientInfo.size());
    }

    /**
     * 生成API密钥
     */
    private String generateApiKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }
} 