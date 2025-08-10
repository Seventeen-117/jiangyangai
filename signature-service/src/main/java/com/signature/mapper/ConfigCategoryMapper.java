package com.signature.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.signature.entity.ConfigCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 配置分类Mapper接口
 */
@Mapper
public interface ConfigCategoryMapper extends BaseMapper<ConfigCategory> {
    // 所有查询方法已迁移到ConfigCategoryService中使用LambdaQueryWrapper实现
}
