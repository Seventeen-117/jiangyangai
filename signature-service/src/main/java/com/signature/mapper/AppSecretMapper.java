package com.signature.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.signature.entity.AppSecret;
import org.apache.ibatis.annotations.Mapper;

/**
 * 应用密钥Mapper接口
 * 
 * @author bgpay
 * @since 2024-01-01
 */
@Mapper
public interface AppSecretMapper extends BaseMapper<AppSecret> {
    // 所有查询方法已迁移到AppSecretService中使用LambdaQueryWrapper实现
} 