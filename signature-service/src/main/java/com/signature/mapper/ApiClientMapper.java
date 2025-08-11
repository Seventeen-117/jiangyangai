package com.signature.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.signature.entity.ApiClient;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * API客户端Mapper接口
 * </p>
 *
 * @author signature-service
 * @since 2025-01-01
 */
@Mapper
public interface ApiClientMapper extends BaseMapper<ApiClient> {
    // 所有查询方法已迁移到ApiClientService中使用LambdaQueryWrapper实现
}
