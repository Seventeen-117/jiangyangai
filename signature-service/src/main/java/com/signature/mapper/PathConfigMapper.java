package com.signature.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.signature.entity.PathConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 路径配置Mapper接口
 * 
 * @author signature-service
 * @since 2025-01-01
 */
@Mapper
public interface PathConfigMapper extends BaseMapper<PathConfig> {
    // 所有查询方法已迁移到PathConfigService中使用LambdaQueryWrapper实现
}