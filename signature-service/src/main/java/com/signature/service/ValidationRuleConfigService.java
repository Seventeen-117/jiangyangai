package com.signature.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.signature.entity.ValidationRuleConfig;

import java.util.List;

/**
 * 验证规则配置Service接口
 *
 * @author signature-service
 * @since 2025-01-01
 */
public interface ValidationRuleConfigService extends IService<ValidationRuleConfig> {

    /**
     * 根据规则编码查询配置
     *
     * @param ruleCode 规则编码
     * @return 验证规则配置
     */
    ValidationRuleConfig findByRuleCode(String ruleCode);

    /**
     * 根据规则编码查询启用的配置
     *
     * @param ruleCode 规则编码
     * @return 验证规则配置
     */
    ValidationRuleConfig findEnabledByRuleCode(String ruleCode);

    /**
     * 根据规则编码查询启用且生效的配置
     *
     * @param ruleCode 规则编码
     * @return 验证规则配置
     */
    ValidationRuleConfig findByRuleCodeAndEnabled(String ruleCode);

    /**
     * 根据规则类型查询配置列表
     *
     * @param ruleType 规则类型
     * @return 验证规则配置列表
     */
    List<ValidationRuleConfig> findByRuleType(String ruleType);

    /**
     * 根据规则类型查询启用的配置列表
     *
     * @param ruleType 规则类型
     * @return 验证规则配置列表
     */
    List<ValidationRuleConfig> findEnabledByRuleType(String ruleType);

    /**
     * 查询所有启用的验证规则配置
     *
     * @return 验证规则配置列表
     */
    List<ValidationRuleConfig> findAllEnabled();

    /**
     * 根据状态查询验证规则配置
     *
     * @param status 状态
     * @return 验证规则配置列表
     */
    List<ValidationRuleConfig> findByStatus(Integer status);

    /**
     * 根据启用状态查询验证规则配置
     *
     * @param enabled 启用状态
     * @return 验证规则配置列表
     */
    List<ValidationRuleConfig> findByEnabled(Boolean enabled);

    /**
     * 创建验证规则配置
     *
     * @param ruleConfig 验证规则配置
     * @return 创建后的验证规则配置
     */
    ValidationRuleConfig createRuleConfig(ValidationRuleConfig ruleConfig);

    /**
     * 更新验证规则配置
     *
     * @param ruleConfig 验证规则配置
     * @return 更新后的验证规则配置
     */
    ValidationRuleConfig updateRuleConfig(ValidationRuleConfig ruleConfig);

    /**
     * 删除验证规则配置
     *
     * @param id 配置ID
     * @return 是否删除成功
     */
    boolean deleteRuleConfig(Long id);

    /**
     * 启用验证规则配置
     *
     * @param id 配置ID
     * @return 是否启用成功
     */
    boolean enableRuleConfig(Long id);

    /**
     * 禁用验证规则配置
     *
     * @param id 配置ID
     * @return 是否禁用成功
     */
    boolean disableRuleConfig(Long id);

    /**
     * 根据规则编码批量查询
     *
     * @param ruleCodes 规则编码列表
     * @return 验证规则配置列表
     */
    List<ValidationRuleConfig> findByRuleCodes(List<String> ruleCodes);

    /**
     * 分页查询验证规则配置
     *
     * @param page 页码
     * @param size 页大小
     * @param ruleType 规则类型（可选）
     * @param status 状态（可选）
     * @return 分页结果
     */
    Page<ValidationRuleConfig> findPage(Integer page, Integer size, String ruleType, Integer status);

    /**
     * 根据规则编码获取配置JSON
     *
     * @param ruleCode 规则编码
     * @return 配置JSON字符串
     */
    String getConfigJsonByRuleCode(String ruleCode);

    /**
     * 检查规则是否启用
     *
     * @param ruleCode 规则编码
     * @return 是否启用
     */
    boolean isRuleEnabled(String ruleCode);

    /**
     * 获取规则的优先级
     *
     * @param ruleCode 规则编码
     * @return 优先级，如果不存在返回null
     */
    Integer getRulePriority(String ruleCode);
}
