package com.signature.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.signature.entity.ApiPermission;
import org.apache.ibatis.annotations.Mapper;

/**
 * API权限Mapper接口
 */
@Mapper
public interface ApiPermissionMapper extends BaseMapper<ApiPermission> {
    // 所有查询方法已迁移到ApiPermissionService中使用LambdaQueryWrapper实现
}
