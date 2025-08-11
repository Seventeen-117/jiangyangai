package com.signature.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.signature.entity.ApiKey;
import org.apache.ibatis.annotations.Mapper;

/**
 * API密钥Mapper接口
 */
@Mapper
public interface ApiKeyMapper extends BaseMapper<ApiKey> {
    // 所有查询方法已迁移到ApiKeyService中使用LambdaQueryWrapper实现
} 