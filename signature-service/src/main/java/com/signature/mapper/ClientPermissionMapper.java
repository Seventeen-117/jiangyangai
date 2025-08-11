package com.signature.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.signature.entity.ClientPermission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户端权限Mapper接口
 */
@Mapper
public interface ClientPermissionMapper extends BaseMapper<ClientPermission> {
    // 所有查询方法已迁移到ClientPermissionService中使用LambdaQueryWrapper实现
}
