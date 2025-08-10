package com.signature.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.signature.entity.AppSecret;

import java.util.List;

/**
 * 应用密钥服务接口
 *
 * @author bgpay
 * @since 2024-01-01
 */
public interface AppSecretService extends IService<AppSecret> {

    /**
     * 根据应用ID查询应用密钥
     *
     * @param appId 应用ID
     * @return 应用密钥信息
     */
    AppSecret selectByAppId(String appId);

    /**
     * 根据应用ID查询启用的应用密钥
     *
     * @param appId 应用ID
     * @return 应用密钥信息
     */
    AppSecret findEnabledByAppId(String appId);

    /**
     * 验证应用凭据
     *
     * @param appId     应用ID
     * @param appSecret 应用密钥
     * @return 是否有效
     */
    boolean validateCredentials(String appId, String appSecret);

    /**
     * 查询所有启用的应用密钥
     *
     * @return 应用密钥列表
     */
    List<AppSecret> findAllEnabled();

    /**
     * 根据状态查询应用密钥
     *
     * @param status 状态
     * @return 应用密钥列表
     */
    List<AppSecret> findByStatus(Integer status);

    /**
     * 创建应用密钥
     *
     * @param appSecret 应用密钥
     * @return 创建的应用密钥
     */
    AppSecret createAppSecret(AppSecret appSecret);

    /**
     * 更新应用密钥
     *
     * @param appSecret 应用密钥
     * @return 更新后的应用密钥
     */
    AppSecret updateAppSecret(AppSecret appSecret);

    /**
     * 删除应用密钥
     *
     * @param id 应用密钥ID
     * @return 是否删除成功
     */
    boolean deleteAppSecret(Long id);

    /**
     * 启用/禁用应用
     *
     * @param id     应用密钥ID
     * @param status 状态 (0: 禁用, 1: 启用)
     * @return 是否更新成功
     */
    boolean updateStatus(Long id, Integer status);

    /**
     * 批量更新应用状态
     *
     * @param ids    应用密钥ID列表
     * @param status 状态
     * @return 更新数量
     */
    int batchUpdateStatus(List<Long> ids, Integer status);

    /**
     * 根据应用名称模糊查询
     *
     * @param appName 应用名称
     * @return 应用密钥列表
     */
    List<AppSecret> findByAppNameLike(String appName);

    /**
     * 检查应用ID是否已存在
     *
     * @param appId 应用ID
     * @return 是否存在
     */
    boolean existsByAppId(String appId);

    /**
     * 统计应用数量
     *
     * @param status 状态
     * @return 应用数量
     */
    Long countByStatus(Integer status);
}
