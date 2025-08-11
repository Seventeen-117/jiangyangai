package com.signature.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.signature.entity.ConfigCategory;

import java.util.List;

/**
 * 配置分类服务接口
 *
 * @author signature-service
 * @since 2025-01-01
 */
public interface ConfigCategoryService extends IService<ConfigCategory> {

    /**
     * 根据分类编码查询配置分类
     *
     * @param categoryCode 分类编码
     * @return 配置分类
     */
    ConfigCategory findByCategoryCode(String categoryCode);

    /**
     * 查询所有启用的配置分类
     *
     * @return 配置分类列表
     */
    List<ConfigCategory> findAllEnabled();

    /**
     * 根据状态查询配置分类
     *
     * @param status 状态
     * @return 配置分类列表
     */
    List<ConfigCategory> findByStatus(Integer status);

    /**
     * 创建配置分类
     *
     * @param configCategory 配置分类
     * @return 创建的配置分类
     */
    ConfigCategory createConfigCategory(ConfigCategory configCategory);

    /**
     * 更新配置分类
     *
     * @param configCategory 配置分类
     * @return 更新后的配置分类
     */
    ConfigCategory updateConfigCategory(ConfigCategory configCategory);

    /**
     * 删除配置分类
     *
     * @param id 分类ID
     * @return 是否删除成功
     */
    boolean deleteConfigCategory(Long id);

    /**
     * 启用/禁用配置分类
     *
     * @param id     分类ID
     * @param status 状态 (0: 禁用, 1: 启用)
     * @return 是否更新成功
     */
    boolean updateStatus(Long id, Integer status);

    /**
     * 批量更新配置分类状态
     *
     * @param ids    分类ID列表
     * @param status 状态
     * @return 更新数量
     */
    int batchUpdateStatus(List<Long> ids, Integer status);

    /**
     * 检查分类编码是否已存在
     *
     * @param categoryCode 分类编码
     * @return 是否存在
     */
    boolean existsByCategoryCode(String categoryCode);

    /**
     * 根据分类名称模糊查询
     *
     * @param categoryName 分类名称
     * @return 配置分类列表
     */
    List<ConfigCategory> findByCategoryNameLike(String categoryName);

    /**
     * 根据排序顺序查询配置分类
     *
     * @param sortOrder 排序顺序
     * @return 配置分类列表
     */
    List<ConfigCategory> findBySortOrder(Integer sortOrder);

    /**
     * 获取下一个排序顺序
     *
     * @return 下一个排序顺序
     */
    Integer getNextSortOrder();

    /**
     * 调整分类排序
     *
     * @param id        分类ID
     * @param direction 调整方向 (up: 向上, down: 向下)
     * @return 是否调整成功
     */
    boolean adjustSortOrder(Long id, String direction);

    /**
     * 获取配置分类统计信息
     *
     * @return 统计信息
     */
    Object getConfigCategoryStatistics();
}
