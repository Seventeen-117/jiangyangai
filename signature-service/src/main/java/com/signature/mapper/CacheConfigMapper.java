package com.signature.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.signature.entity.CacheConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 缓存配置Mapper接口
 */
@Mapper
public interface CacheConfigMapper extends BaseMapper<CacheConfig> {
    // 所有查询方法已迁移到CacheConfigService中使用LambdaQueryWrapper实现
}
