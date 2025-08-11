package com.signature.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.signature.entity.CacheConfig;

import java.util.List;

/**
 * 缓存配置服务接口
 *
 * @author signature-service
 * @since 2025-01-01
 */
public interface CacheConfigService extends IService<CacheConfig> {

    /**
     * 根据缓存键查询启用的缓存配置
     *
     * @param cacheKey 缓存键
     * @return 缓存配置
     */
    CacheConfig findByCacheKey(String cacheKey);

    /**
     * 根据缓存类型查询启用的缓存配置
     *
     * @param cacheType 缓存类型
     * @return 缓存配置列表
     */
    List<CacheConfig> findByCacheType(String cacheType);

    /**
     * 查询所有启用的缓存配置
     *
     * @return 缓存配置列表
     */
    List<CacheConfig> findAllEnabled();

    /**
     * 根据状态查询缓存配置
     *
     * @param status 状态
     * @return 缓存配置列表
     */
    List<CacheConfig> findByStatus(Integer status);

    /**
     * 根据缓存名称模糊查询
     *
     * @param cacheName 缓存名称
     * @return 缓存配置列表
     */
    List<CacheConfig> findByCacheNameLike(String cacheName);

    /**
     * 创建缓存配置
     *
     * @param cacheConfig 缓存配置
     * @return 创建的缓存配置
     */
    CacheConfig createCacheConfig(CacheConfig cacheConfig);

    /**
     * 更新缓存配置
     *
     * @param cacheConfig 缓存配置
     * @return 更新后的缓存配置
     */
    CacheConfig updateCacheConfig(CacheConfig cacheConfig);

    /**
     * 删除缓存配置
     *
     * @param id 缓存配置ID
     * @return 是否删除成功
     */
    boolean deleteCacheConfig(Long id);

    /**
     * 启用/禁用缓存配置
     *
     * @param id     缓存配置ID
     * @param status 状态 (0: 禁用, 1: 启用)
     * @return 是否更新成功
     */
    boolean updateStatus(Long id, Integer status);

    /**
     * 批量更新缓存配置状态
     *
     * @param ids    缓存配置ID列表
     * @param status 状态
     * @return 更新数量
     */
    int batchUpdateStatus(List<Long> ids, Integer status);

    /**
     * 检查缓存键是否已存在
     *
     * @param cacheKey 缓存键
     * @return 是否存在
     */
    boolean existsByCacheKey(String cacheKey);

    /**
     * 根据缓存键和类型查询配置
     *
     * @param cacheKey  缓存键
     * @param cacheType 缓存类型
     * @return 缓存配置
     */
    CacheConfig findByCacheKeyAndType(String cacheKey, String cacheType);

    /**
     * 获取缓存配置统计信息
     *
     * @return 统计信息
     */
    Object getCacheConfigStatistics();

    /**
     * 根据过期时间范围查询配置
     *
     * @param minExpireSeconds 最小过期时间（秒）
     * @param maxExpireSeconds 最大过期时间（秒）
     * @return 缓存配置列表
     */
    List<CacheConfig> findByExpireSecondsRange(Integer minExpireSeconds, Integer maxExpireSeconds);

    /**
     * 清理过期的缓存配置
     *
     * @return 清理数量
     */
    int cleanExpiredConfigs();
}
