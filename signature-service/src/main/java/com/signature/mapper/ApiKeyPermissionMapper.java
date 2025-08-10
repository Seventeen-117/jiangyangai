package com.signature.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.signature.entity.ApiKeyPermission;
import org.apache.ibatis.annotations.Mapper;

/**
 * API密钥权限关联Mapper接口
 */
@Mapper
public interface ApiKeyPermissionMapper extends BaseMapper<ApiKeyPermission> {
    // 所有查询方法已迁移到ApiKeyPermissionService中使用LambdaQueryWrapper实现
}
