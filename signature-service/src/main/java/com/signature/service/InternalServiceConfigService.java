package com.signature.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.signature.entity.InternalServiceConfig;

import java.util.List;

/**
 * 内部服务配置Service接口
 *
 * @author signature-service
 * @since 2025-01-01
 */
public interface InternalServiceConfigService extends IService<InternalServiceConfig> {

    /**
     * 根据服务编码查询配置
     *
     * @param serviceCode 服务编码
     * @return 服务配置
     */
    InternalServiceConfig findByServiceCode(String serviceCode);

    /**
     * 根据服务编码查询启用的配置
     *
     * @param serviceCode 服务编码
     * @return 服务配置
     */
    InternalServiceConfig findEnabledByServiceCode(String serviceCode);

    /**
     * 根据API密钥查询配置
     *
     * @param apiKey API密钥
     * @return 服务配置
     */
    InternalServiceConfig findByApiKey(String apiKey);

    /**
     * 根据API密钥查询启用的配置
     *
     * @param apiKey API密钥
     * @return 服务配置
     */
    InternalServiceConfig findEnabledByApiKey(String apiKey);

    /**
     * 根据服务类型查询配置列表
     *
     * @param serviceType 服务类型
     * @return 服务配置列表
     */
    List<InternalServiceConfig> findByServiceType(String serviceType);

    /**
     * 根据服务类型查询启用的配置列表
     *
     * @param serviceType 服务类型
     * @return 服务配置列表
     */
    List<InternalServiceConfig> findEnabledByServiceType(String serviceType);

    /**
     * 查询所有启用的服务配置
     *
     * @return 服务配置列表
     */
    List<InternalServiceConfig> findAllEnabled();

    /**
     * 根据状态查询服务配置
     *
     * @param status 状态
     * @return 服务配置列表
     */
    List<InternalServiceConfig> findByStatus(Integer status);

    /**
     * 创建服务配置
     *
     * @param serviceConfig 服务配置
     * @return 创建后的服务配置
     */
    InternalServiceConfig createServiceConfig(InternalServiceConfig serviceConfig);

    /**
     * 更新服务配置
     *
     * @param serviceConfig 服务配置
     * @return 更新后的服务配置
     */
    InternalServiceConfig updateServiceConfig(InternalServiceConfig serviceConfig);

    /**
     * 删除服务配置
     *
     * @param id 配置ID
     * @return 是否删除成功
     */
    boolean deleteServiceConfig(Long id);

    /**
     * 启用服务配置
     *
     * @param id 配置ID
     * @return 是否启用成功
     */
    boolean enableServiceConfig(Long id);

    /**
     * 禁用服务配置
     *
     * @param id 配置ID
     * @return 是否禁用成功
     */
    boolean disableServiceConfig(Long id);

    /**
     * 根据服务编码批量查询
     *
     * @param serviceCodes 服务编码列表
     * @return 服务配置列表
     */
    List<InternalServiceConfig> findByServiceCodes(List<String> serviceCodes);

    /**
     * 分页查询服务配置
     *
     * @param page 页码
     * @param size 页大小
     * @param serviceType 服务类型（可选）
     * @param status 状态（可选）
     * @return 分页结果
     */
    Page<InternalServiceConfig> findPage(Integer page, Integer size, String serviceType, Integer status);
}
