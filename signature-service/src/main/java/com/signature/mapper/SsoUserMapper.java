package com.signature.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.signature.entity.SsoUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * SSO用户Mapper接口
 */
@Mapper
public interface SsoUserMapper extends BaseMapper<SsoUser> {
    // 所有查询方法已迁移到SsoUserService中使用LambdaQueryWrapper实现
}
