package com.signature.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.signature.entity.ApiClient;

import java.util.List;

/**
 * API客户端服务接口
 *
 * @author signature-service
 * @since 2025-01-01
 */
public interface ApiClientService extends IService<ApiClient> {

    /**
     * 根据客户端ID查找启用的客户端
     *
     * @param clientId 客户端ID
     * @return API客户端
     */
    ApiClient findEnabledByClientId(String clientId);

    /**
     * 根据客户端类型查找客户端
     *
     * @param clientType 客户端类型
     * @return API客户端列表
     */
    List<ApiClient> findByClientType(String clientType);

    /**
     * 查找所有启用的客户端
     *
     * @return API客户端列表
     */
    List<ApiClient> findAllEnabled();

    /**
     * 根据客户端ID查找客户端（包含已删除的）
     *
     * @param clientId 客户端ID
     * @return API客户端
     */
    ApiClient findByClientIdWithDeleted(String clientId);

    /**
     * 统计客户端数量
     *
     * @param status 状态
     * @return 客户端数量
     */
    Long countByStatus(Integer status);

    /**
     * 创建API客户端
     *
     * @param apiClient API客户端
     * @return 创建的API客户端
     */
    ApiClient createApiClient(ApiClient apiClient);

    /**
     * 更新API客户端
     *
     * @param apiClient API客户端
     * @return 更新后的API客户端
     */
    ApiClient updateApiClient(ApiClient apiClient);

    /**
     * 删除API客户端（逻辑删除）
     *
     * @param id 客户端ID
     * @return 是否删除成功
     */
    boolean deleteApiClient(Long id);

    /**
     * 启用/禁用客户端
     *
     * @param id     客户端ID
     * @param status 状态 (0: 禁用, 1: 启用)
     * @return 是否更新成功
     */
    boolean updateStatus(Long id, Integer status);

    /**
     * 批量更新客户端状态
     *
     * @param ids    客户端ID列表
     * @param status 状态
     * @return 更新数量
     */
    int batchUpdateStatus(List<Long> ids, Integer status);

    /**
     * 验证客户端凭据
     *
     * @param clientId     客户端ID
     * @param clientSecret 客户端密钥
     * @return 是否有效
     */
    boolean validateCredentials(String clientId, String clientSecret);

    /**
     * 根据作用域查找客户端
     *
     * @param scope 作用域
     * @return API客户端列表
     */
    List<ApiClient> findByScope(String scope);

    /**
     * 检查客户端是否支持指定的授权类型
     *
     * @param clientId  客户端ID
     * @param grantType 授权类型
     * @return 是否支持
     */
    boolean supportsGrantType(String clientId, String grantType);
}
