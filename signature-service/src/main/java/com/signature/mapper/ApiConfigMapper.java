package com.signature.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.signature.entity.ApiConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * API配置Mapper接口
 * </p>
 *
 * @author signature-service
 * @since 2025-01-01
 */
@Mapper
public interface ApiConfigMapper extends BaseMapper<ApiConfig> {

    /**
     * 根据用户ID和模型类型查找配置
     *
     * @param userId    用户ID
     * @param modelType 模型类型
     * @return API配置列表
     */
    List<ApiConfig> findByUserIdAndModelType(@Param("userId") String userId, @Param("modelType") String modelType);

    /**
     * 根据用户ID查找启用的配置
     *
     * @param userId 用户ID
     * @return API配置列表
     */
    List<ApiConfig> findEnabledByUserId(@Param("userId") String userId);

    /**
     * 查找默认配置
     *
     * @param userId 用户ID
     * @return 默认API配置
     */
    ApiConfig findDefaultByUserId(@Param("userId") String userId);

    /**
     * 根据API类型查找配置
     *
     * @param userId  用户ID
     * @param apiType API类型
     * @return API配置列表
     */
    List<ApiConfig> findByUserIdAndApiType(@Param("userId") String userId, @Param("apiType") String apiType);

    /**
     * 查找高优先级配置
     *
     * @param userId 用户ID
     * @param limit  限制数量
     * @return API配置列表
     */
    List<ApiConfig> findHighPriorityByUserId(@Param("userId") String userId, @Param("limit") Integer limit);

    /**
     * 统计用户配置数量
     *
     * @param userId 用户ID
     * @return 配置数量
     */
    Long countByUserId(@Param("userId") String userId);

    /**
     * 查找可用的替代配置
     *
     * @param userId       用户ID
     * @param currentModel 当前模型名称
     * @return API配置列表
     */
    List<ApiConfig> findAlternativeConfigs(@Param("userId") String userId, @Param("currentModel") String currentModel);
}
