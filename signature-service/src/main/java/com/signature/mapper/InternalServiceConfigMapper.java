package com.signature.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.signature.entity.InternalServiceConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 内部服务配置Mapper接口
 * 
 * @author signature-service
 * @since 2025-01-01
 */
@Mapper
public interface InternalServiceConfigMapper extends BaseMapper<InternalServiceConfig> {
    // 所有查询方法已迁移到InternalServiceConfigService中使用LambdaQueryWrapper实现
}