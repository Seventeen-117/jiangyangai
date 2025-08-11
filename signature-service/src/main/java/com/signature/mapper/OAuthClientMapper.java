package com.signature.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.signature.entity.OAuthClient;
import org.apache.ibatis.annotations.Mapper;

/**
 * OAuth客户端Mapper接口
 */
@Mapper
public interface OAuthClientMapper extends BaseMapper<OAuthClient> {
    // 所有查询方法已迁移到OAuthClientService中使用LambdaQueryWrapper实现
}
