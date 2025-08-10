package com.signature.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.signature.entity.ValidationRuleConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 验证规则配置Mapper接口
 * 
 * @author signature-service
 * @since 2025-01-01
 */
@Mapper
public interface ValidationRuleConfigMapper extends BaseMapper<ValidationRuleConfig> {
    // 所有查询方法已迁移到ValidationRuleConfigService中使用LambdaQueryWrapper实现
}