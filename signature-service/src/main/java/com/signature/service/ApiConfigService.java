package com.signature.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.signature.entity.ApiConfig;

import java.util.List;

/**
 * <p>
 * API配置服务接口
 * </p>
 *
 * @author signature-service
 * @since 2025-01-01
 */
public interface ApiConfigService extends IService<ApiConfig> {

    /**
     * 获取用户最新的配置
     *
     * @param userId 用户ID
     * @return 最新的API配置
     */
    ApiConfig getLatestConfig(String userId);

    /**
     * 查找匹配的配置
     *
     * @param userId    用户ID
     * @param apiUrl    API URL
     * @param apiKey    API密钥
     * @param modelName 模型名称
     * @return 匹配的API配置
     */
    ApiConfig findMatchingConfig(String userId, String apiUrl, String apiKey, String modelName);

    /**
     * 查找替代配置
     *
     * @param userId          用户ID
     * @param currentModelName 当前模型名称
     * @return 替代的API配置
     */
    ApiConfig findAlternativeConfig(String userId, String currentModelName);

    /**
     * 获取所有API配置
     *
     * @return API配置列表
     */
    List<ApiConfig> getAllApiConfigs();

    /**
     * 根据ID获取API配置
     *
     * @param id 配置ID
     * @return API配置
     */
    ApiConfig getApiConfigById(Long id);

    /**
     * 创建API配置
     *
     * @param apiConfig API配置
     * @return 创建的API配置
     */
    ApiConfig createApiConfig(ApiConfig apiConfig);

    /**
     * 更新API配置
     *
     * @param apiConfig API配置
     * @return 更新后的API配置
     */
    ApiConfig updateApiConfig(ApiConfig apiConfig);

    /**
     * 删除API配置
     *
     * @param id 配置ID
     * @return 是否删除成功
     */
    boolean deleteApiConfig(Long id);

    /**
     * 根据模型类型获取API配置
     *
     * @param modelType 模型类型
     * @return API配置
     */
    ApiConfig getApiConfigByModelType(String modelType);

    /**
     * 获取默认API配置
     *
     * @return 默认API配置
     */
    ApiConfig getDefaultApiConfig();

    /**
     * 设置默认API配置
     *
     * @param id 配置ID
     * @return 是否设置成功
     */
    boolean setDefaultApiConfig(Long id);

    /**
     * 根据用户ID获取配置列表
     *
     * @param userId 用户ID
     * @return API配置列表
     */
    List<ApiConfig> getConfigsByUserId(String userId);

    /**
     * 根据API类型获取配置
     *
     * @param userId  用户ID
     * @param apiType API类型
     * @return API配置列表
     */
    List<ApiConfig> getConfigsByApiType(String userId, String apiType);

    /**
     * 获取高优先级配置
     *
     * @param userId 用户ID
     * @param limit  限制数量
     * @return API配置列表
     */
    List<ApiConfig> getHighPriorityConfigs(String userId, Integer limit);

    /**
     * 验证API配置
     *
     * @param apiConfig API配置
     * @return 验证结果
     */
    boolean validateApiConfig(ApiConfig apiConfig);

    /**
     * 测试API配置连接
     *
     * @param apiConfig API配置
     * @return 测试结果
     */
    boolean testApiConnection(ApiConfig apiConfig);

    /**
     * 批量更新配置状态
     *
     * @param ids     配置ID列表
     * @param enabled 是否启用
     * @return 更新数量
     */
    int batchUpdateStatus(List<Long> ids, Boolean enabled);

    /**
     * 获取配置统计信息
     *
     * @param userId 用户ID
     * @return 统计信息
     */
    Object getConfigStatistics(String userId);
}
