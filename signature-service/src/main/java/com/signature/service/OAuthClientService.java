package com.signature.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.signature.entity.OAuthClient;

/**
 * OAuth客户端服务接口
 *
 * @author signature-service
 * @since 2025-01-01
 */
public interface OAuthClientService extends IService<OAuthClient> {

    /**
     * 根据客户端ID查询客户端
     *
     * @param clientId 客户端ID
     * @return OAuth客户端
     */
    OAuthClient findByClientId(String clientId);

    /**
     * 根据客户端ID查询启用的客户端
     *
     * @param clientId 客户端ID
     * @return OAuth客户端
     */
    OAuthClient findEnabledByClientId(String clientId);

    /**
     * 验证客户端凭据
     *
     * @param clientId     客户端ID
     * @param clientSecret 客户端密钥
     * @return 匹配的客户端数量
     */
    int validateCredentials(String clientId, String clientSecret);

    /**
     * 根据客户端ID和密钥查询客户端
     *
     * @param clientId     客户端ID
     * @param clientSecret 客户端密钥
     * @return OAuth客户端
     */
    OAuthClient findByCredentials(String clientId, String clientSecret);

    /**
     * 创建OAuth客户端
     *
     * @param oAuthClient OAuth客户端
     * @return 创建后的OAuth客户端
     */
    OAuthClient createOAuthClient(OAuthClient oAuthClient);

    /**
     * 更新OAuth客户端
     *
     * @param oAuthClient OAuth客户端
     * @return 更新后的OAuth客户端
     */
    OAuthClient updateOAuthClient(OAuthClient oAuthClient);

    /**
     * 删除OAuth客户端
     *
     * @param id 客户端ID
     * @return 是否删除成功
     */
    boolean deleteOAuthClient(Long id);

    /**
     * 启用/禁用OAuth客户端
     *
     * @param id     客户端ID
     * @param status 状态 (0: 禁用, 1: 启用)
     * @return 是否更新成功
     */
    boolean updateStatus(Long id, Integer status);
}
