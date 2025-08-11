package com.signature.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.signature.entity.AuthorizationCode;
import org.apache.ibatis.annotations.Mapper;

/**
 * 授权码Mapper接口
 */
@Mapper
public interface AuthorizationCodeMapper extends BaseMapper<AuthorizationCode> {
    // 所有查询方法已迁移到AuthorizationCodeService中使用LambdaQueryWrapper实现
}
