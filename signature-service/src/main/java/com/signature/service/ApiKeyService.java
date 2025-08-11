package com.signature.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.signature.entity.ApiKey;
import com.signature.entity.ApiKeyInfo;
import com.signature.model.ApiKeyValidationResult;

import java.util.List;

/**
 * <p>
 * API密钥服务接口
 * </p>
 *
 * @author signature-service
 * @since 2025-01-01
 */
public interface ApiKeyService extends IService<ApiKey> {

    /**
     * 生成API密钥
     *
     * @param clientId    客户端ID
     * @param clientName  客户端名称
     * @param description 描述
     * @return API密钥信息
     */
    ApiKeyInfo generateApiKey(String clientId, String clientName, String description);

    /**
     * 撤销API密钥
     *
     * @param apiKey API密钥
     */
    void revokeApiKey(String apiKey);

    /**
     * 验证API密钥状态
     *
     * @param apiKey API密钥
     * @return 验证结果
     */
    ApiKeyValidationResult validateApiKeyStatus(String apiKey);

    /**
     * 获取所有API密钥
     *
     * @return API密钥列表
     */
    List<ApiKey> getAllApiKeys();

    /**
     * 获取API密钥信息
     *
     * @param apiKey API密钥
     * @return API密钥信息
     */
    ApiKey getApiKeyInfo(String apiKey);

    /**
     * 更新API密钥状态
     *
     * @param apiKey API密钥
     * @param active 是否激活
     */
    void updateApiKeyStatus(String apiKey, boolean active);

    /**
     * 根据客户端ID获取API密钥列表
     *
     * @param clientId 客户端ID
     * @return API密钥列表
     */
    List<ApiKey> getApiKeysByClientId(String clientId);

    /**
     * 批量撤销API密钥
     *
     * @param apiKeys API密钥列表
     * @return 撤销数量
     */
    int batchRevokeApiKeys(List<String> apiKeys);

    /**
     * 批量更新API密钥状态
     *
     * @param apiKeys API密钥列表
     * @param active  是否激活
     * @return 更新数量
     */
    int batchUpdateApiKeyStatus(List<String> apiKeys, boolean active);

    /**
     * 检查API密钥是否有效
     *
     * @param apiKey API密钥
     * @return 是否有效
     */
    boolean isApiKeyValid(String apiKey);

    /**
     * 获取API密钥统计信息
     *
     * @param clientId 客户端ID
     * @return 统计信息
     */
    Object getApiKeyStatistics(String clientId);

    /**
     * 刷新API密钥
     *
     * @param apiKey API密钥
     * @return 新的API密钥信息
     */
    ApiKeyInfo refreshApiKey(String apiKey);

    /**
     * 延长API密钥有效期
     *
     * @param apiKey      API密钥
     * @param daysToAdd   延长的天数
     * @return 是否成功
     */
    boolean extendApiKeyExpiration(String apiKey, int daysToAdd);

    /**
     * 验证API密钥权限
     *
     * @param apiKey    API密钥
     * @param permission 权限
     * @return 是否有权限
     */
    boolean validateApiKeyPermission(String apiKey, String permission);
} 