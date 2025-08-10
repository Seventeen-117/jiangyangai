package com.signature.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.signature.entity.PathConfig;

import java.util.List;

/**
 * 路径配置服务接口
 *
 * @author signature-service
 * @since 2025-01-01
 */
public interface PathConfigService extends IService<PathConfig> {

    /**
     * 根据路径类型查询启用的路径配置
     *
     * @param pathType 路径类型
     * @return 路径配置列表
     */
    List<PathConfig> findByPathTypeAndEnabled(String pathType);

    /**
     * 根据路径类型查询所有路径配置（包括禁用的）
     *
     * @param pathType 路径类型
     * @return 路径配置列表
     */
    List<PathConfig> findByPathType(String pathType);

    /**
     * 根据路径类型和HTTP方法查询匹配的路径配置
     *
     * @param pathType   路径类型
     * @param httpMethod HTTP方法
     * @return 路径配置列表
     */
    List<PathConfig> findByPathTypeAndHttpMethod(String pathType, String httpMethod);

    /**
     * 查询所有启用的排除路径
     *
     * @return 路径配置列表
     */
    List<PathConfig> findAllExcludedPaths();

    /**
     * 查询所有启用的严格验证路径
     *
     * @return 路径配置列表
     */
    List<PathConfig> findAllStrictPaths();

    /**
     * 查询所有启用的内部路径
     *
     * @return 路径配置列表
     */
    List<PathConfig> findAllInternalPaths();

    /**
     * 创建路径配置
     *
     * @param pathConfig 路径配置
     * @return 创建的路径配置
     */
    PathConfig createPathConfig(PathConfig pathConfig);

    /**
     * 更新路径配置
     *
     * @param pathConfig 路径配置
     * @return 更新后的路径配置
     */
    PathConfig updatePathConfig(PathConfig pathConfig);

    /**
     * 删除路径配置
     *
     * @param id 路径配置ID
     * @return 是否删除成功
     */
    boolean deletePathConfig(Long id);

    /**
     * 启用/禁用路径配置
     *
     * @param id     路径配置ID
     * @param status 状态 (0: 禁用, 1: 启用)
     * @return 是否更新成功
     */
    boolean updateStatus(Long id, Integer status);

    /**
     * 批量更新路径配置状态
     *
     * @param ids    路径配置ID列表
     * @param status 状态
     * @return 更新数量
     */
    int batchUpdateStatus(List<Long> ids, Integer status);

    /**
     * 根据分类ID查询路径配置
     *
     * @param categoryId 分类ID
     * @return 路径配置列表
     */
    List<PathConfig> findByCategoryId(Long categoryId);

    /**
     * 检查路径模式是否已存在
     *
     * @param pathPattern 路径模式
     * @param pathType    路径类型
     * @return 是否存在
     */
    boolean existsByPathPatternAndType(String pathPattern, String pathType);

    /**
     * 根据路径模式模糊查询
     *
     * @param pathPattern 路径模式
     * @return 路径配置列表
     */
    List<PathConfig> findByPathPatternLike(String pathPattern);

    /**
     * 检查指定路径和方法是否匹配某个配置
     *
     * @param path       请求路径
     * @param method     HTTP方法
     * @param pathType   路径类型
     * @return 是否匹配
     */
    boolean isPathMatched(String path, String method, String pathType);

    /**
     * 获取所有启用的配置，按排序顺序返回
     *
     * @return 路径配置列表
     */
    List<PathConfig> findAllEnabledOrderBySortOrder();
}
