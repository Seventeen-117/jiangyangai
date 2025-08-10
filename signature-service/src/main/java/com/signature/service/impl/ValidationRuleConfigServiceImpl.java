package com.signature.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.signature.entity.ValidationRuleConfig;
import com.signature.mapper.ValidationRuleConfigMapper;
import com.signature.service.ValidationRuleConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 验证规则配置Service实现类
 *
 * @author signature-service
 * @since 2025-01-01
 */
@Slf4j
@Service
public class ValidationRuleConfigServiceImpl extends ServiceImpl<ValidationRuleConfigMapper, ValidationRuleConfig> 
        implements ValidationRuleConfigService {

    @Override
    public ValidationRuleConfig findByRuleCode(String ruleCode) {
        if (!StringUtils.hasText(ruleCode)) {
            log.warn("findByRuleCode: ruleCode is empty");
            return null;
        }

        LambdaQueryWrapper<ValidationRuleConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ValidationRuleConfig::getRuleCode, ruleCode)

               .orderByDesc(ValidationRuleConfig::getCreatedTime)
               .last("LIMIT 1");

        ValidationRuleConfig result = this.getOne(wrapper);
        log.info("findByRuleCode: ruleCode={}, found={}", ruleCode, result != null);
        return result;
    }

    @Override
    public ValidationRuleConfig findEnabledByRuleCode(String ruleCode) {
        if (!StringUtils.hasText(ruleCode)) {
            log.warn("findEnabledByRuleCode: ruleCode is empty");
            return null;
        }

        LambdaQueryWrapper<ValidationRuleConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ValidationRuleConfig::getRuleCode, ruleCode)
               .eq(ValidationRuleConfig::getStatus, 1)

               .orderByDesc(ValidationRuleConfig::getCreatedTime)
               .last("LIMIT 1");

        ValidationRuleConfig result = this.getOne(wrapper);
        log.info("findEnabledByRuleCode: ruleCode={}, found={}", ruleCode, result != null);
        return result;
    }

    @Override
    public ValidationRuleConfig findByRuleCodeAndEnabled(String ruleCode) {
        if (!StringUtils.hasText(ruleCode)) {
            log.warn("findByRuleCodeAndEnabled: ruleCode is empty");
            return null;
        }

        LambdaQueryWrapper<ValidationRuleConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ValidationRuleConfig::getRuleCode, ruleCode)
               .eq(ValidationRuleConfig::getStatus, 1)
               .eq(ValidationRuleConfig::getEnabled, true)

               .orderByDesc(ValidationRuleConfig::getCreatedTime)
               .last("LIMIT 1");

        ValidationRuleConfig result = this.getOne(wrapper);
        log.info("findByRuleCodeAndEnabled: ruleCode={}, found={}", ruleCode, result != null);
        return result;
    }

    @Override
    public List<ValidationRuleConfig> findByRuleType(String ruleType) {
        if (!StringUtils.hasText(ruleType)) {
            log.warn("findByRuleType: ruleType is empty");
            return List.of();
        }

        LambdaQueryWrapper<ValidationRuleConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ValidationRuleConfig::getRuleType, ruleType)

               .orderByAsc(ValidationRuleConfig::getSortOrder)
               .orderByDesc(ValidationRuleConfig::getCreatedTime);

        List<ValidationRuleConfig> result = this.list(wrapper);
        log.info("findByRuleType: ruleType={}, count={}", ruleType, result.size());
        return result;
    }

    @Override
    public List<ValidationRuleConfig> findEnabledByRuleType(String ruleType) {
        if (!StringUtils.hasText(ruleType)) {
            log.warn("findEnabledByRuleType: ruleType is empty");
            return List.of();
        }

        LambdaQueryWrapper<ValidationRuleConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ValidationRuleConfig::getRuleType, ruleType)
               .eq(ValidationRuleConfig::getStatus, 1)
               .eq(ValidationRuleConfig::getEnabled, true)

               .orderByAsc(ValidationRuleConfig::getSortOrder)
               .orderByDesc(ValidationRuleConfig::getCreatedTime);

        List<ValidationRuleConfig> result = this.list(wrapper);
        log.info("findEnabledByRuleType: ruleType={}, count={}", ruleType, result.size());
        return result;
    }

    @Override
    public List<ValidationRuleConfig> findAllEnabled() {
        LambdaQueryWrapper<ValidationRuleConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ValidationRuleConfig::getStatus, 1)
               .eq(ValidationRuleConfig::getEnabled, true)

               .orderByAsc(ValidationRuleConfig::getSortOrder)
               .orderByDesc(ValidationRuleConfig::getCreatedTime);

        List<ValidationRuleConfig> result = this.list(wrapper);
        log.info("findAllEnabled: count={}", result.size());
        return result;
    }

    @Override
    public List<ValidationRuleConfig> findByStatus(Integer status) {
        if (status == null) {
            log.warn("findByStatus: status is null");
            return List.of();
        }

        LambdaQueryWrapper<ValidationRuleConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ValidationRuleConfig::getStatus, status)

               .orderByAsc(ValidationRuleConfig::getSortOrder)
               .orderByDesc(ValidationRuleConfig::getCreatedTime);

        List<ValidationRuleConfig> result = this.list(wrapper);
        log.info("findByStatus: status={}, count={}", status, result.size());
        return result;
    }

    @Override
    public List<ValidationRuleConfig> findByEnabled(Boolean enabled) {
        if (enabled == null) {
            log.warn("findByEnabled: enabled is null");
            return List.of();
        }

        LambdaQueryWrapper<ValidationRuleConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ValidationRuleConfig::getEnabled, enabled)

               .orderByAsc(ValidationRuleConfig::getSortOrder)
               .orderByDesc(ValidationRuleConfig::getCreatedTime);

        List<ValidationRuleConfig> result = this.list(wrapper);
        log.info("findByEnabled: enabled={}, count={}", enabled, result.size());
        return result;
    }

    @Override
    @Transactional
    public ValidationRuleConfig createRuleConfig(ValidationRuleConfig ruleConfig) {
        if (ruleConfig == null) {
            throw new IllegalArgumentException("ValidationRuleConfig cannot be null");
        }

        // 验证规则编码唯一性
        if (StringUtils.hasText(ruleConfig.getRuleCode())) {
            ValidationRuleConfig existing = findByRuleCode(ruleConfig.getRuleCode());
            if (existing != null) {
                throw new RuntimeException("规则编码已存在: " + ruleConfig.getRuleCode());
            }
        }

        // 设置默认值
        ruleConfig.setCreatedTime(LocalDateTime.now());
        ruleConfig.setUpdatedTime(LocalDateTime.now());
        if (ruleConfig.getStatus() == null) {
            ruleConfig.setStatus(1);
        }
        if (ruleConfig.getEnabled() == null) {
            ruleConfig.setEnabled(1);
        }


        boolean saved = this.save(ruleConfig);
        if (saved) {
            log.info("createRuleConfig: Created rule config with ID {}", ruleConfig.getId());
        }
        return saved ? ruleConfig : null;
    }

    @Override
    @Transactional
    public ValidationRuleConfig updateRuleConfig(ValidationRuleConfig ruleConfig) {
        if (ruleConfig == null || ruleConfig.getId() == null) {
            throw new IllegalArgumentException("ValidationRuleConfig and ID cannot be null");
        }

        // 验证规则编码唯一性（排除自身）
        if (StringUtils.hasText(ruleConfig.getRuleCode())) {
            ValidationRuleConfig existing = findByRuleCode(ruleConfig.getRuleCode());
            if (existing != null && !existing.getId().equals(ruleConfig.getId())) {
                throw new RuntimeException("规则编码已存在: " + ruleConfig.getRuleCode());
            }
        }

        ruleConfig.setUpdatedTime(LocalDateTime.now());
        boolean updated = this.updateById(ruleConfig);
        if (updated) {
            log.info("updateRuleConfig: Updated rule config with ID {}", ruleConfig.getId());
        }
        return updated ? ruleConfig : null;
    }

    @Override
    @Transactional
    public boolean deleteRuleConfig(Long id) {
        if (id == null) {
            log.warn("deleteRuleConfig: id is null");
            return false;
        }

        // 禁用规则配置（因为没有deleted字段，改为设置status=0）
        LambdaUpdateWrapper<ValidationRuleConfig> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ValidationRuleConfig::getId, id)
               .set(ValidationRuleConfig::getStatus, 0)
               .set(ValidationRuleConfig::getEnabled, 0)
               .set(ValidationRuleConfig::getUpdatedTime, LocalDateTime.now());

        boolean deleted = this.update(wrapper);
        log.info("deleteRuleConfig: id={}, deleted={}", id, deleted);
        return deleted;
    }

    @Override
    @Transactional
    public boolean enableRuleConfig(Long id) {
        if (id == null) {
            log.warn("enableRuleConfig: id is null");
            return false;
        }

        LambdaUpdateWrapper<ValidationRuleConfig> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ValidationRuleConfig::getId, id)

               .set(ValidationRuleConfig::getStatus, 1)
               .set(ValidationRuleConfig::getEnabled, 1)
               .set(ValidationRuleConfig::getUpdatedTime, LocalDateTime.now());

        boolean enabled = this.update(wrapper);
        log.info("enableRuleConfig: id={}, enabled={}", id, enabled);
        return enabled;
    }

    @Override
    @Transactional
    public boolean disableRuleConfig(Long id) {
        if (id == null) {
            log.warn("disableRuleConfig: id is null");
            return false;
        }

        LambdaUpdateWrapper<ValidationRuleConfig> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ValidationRuleConfig::getId, id)

               .set(ValidationRuleConfig::getStatus, 0)
               .set(ValidationRuleConfig::getEnabled, 0)
               .set(ValidationRuleConfig::getUpdatedTime, LocalDateTime.now());

        boolean disabled = this.update(wrapper);
        log.info("disableRuleConfig: id={}, disabled={}", id, disabled);
        return disabled;
    }

    @Override
    public List<ValidationRuleConfig> findByRuleCodes(List<String> ruleCodes) {
        if (ruleCodes == null || ruleCodes.isEmpty()) {
            log.warn("findByRuleCodes: ruleCodes is empty");
            return List.of();
        }

        LambdaQueryWrapper<ValidationRuleConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ValidationRuleConfig::getRuleCode, ruleCodes)

               .orderByAsc(ValidationRuleConfig::getSortOrder)
               .orderByDesc(ValidationRuleConfig::getCreatedTime);

        List<ValidationRuleConfig> result = this.list(wrapper);
        log.info("findByRuleCodes: count={}", result.size());
        return result;
    }

    @Override
    public Page<ValidationRuleConfig> findPage(Integer page, Integer size, String ruleType, Integer status) {
        Page<ValidationRuleConfig> pageParam = new Page<>(page != null ? page : 1, size != null ? size : 10);
        
        LambdaQueryWrapper<ValidationRuleConfig> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(ruleType)) {
            wrapper.eq(ValidationRuleConfig::getRuleType, ruleType);
        }
        if (status != null) {
            wrapper.eq(ValidationRuleConfig::getStatus, status);
        }
        
        wrapper.orderByAsc(ValidationRuleConfig::getSortOrder)
               .orderByDesc(ValidationRuleConfig::getCreatedTime);

        Page<ValidationRuleConfig> result = this.page(pageParam, wrapper);
        log.info("findPage: page={}, size={}, ruleType={}, status={}, total={}", 
                page, size, ruleType, status, result.getTotal());
        return result;
    }

    @Override
    public String getConfigJsonByRuleCode(String ruleCode) {
        ValidationRuleConfig config = findEnabledByRuleCode(ruleCode);
        if (config != null && StringUtils.hasText(config.getConfigJson())) {
            return config.getConfigJson();
        }
        log.info("getConfigJsonByRuleCode: ruleCode={}, configJson not found", ruleCode);
        return null;
    }

    @Override
    public boolean isRuleEnabled(String ruleCode) {
        ValidationRuleConfig config = findByRuleCode(ruleCode);
        boolean enabled = config != null && config.getStatus() == 1 && 
                         Boolean.TRUE.equals(config.getEnabled());
        log.info("isRuleEnabled: ruleCode={}, enabled={}", ruleCode, enabled);
        return enabled;
    }

    @Override
    public Integer getRulePriority(String ruleCode) {
        ValidationRuleConfig config = findByRuleCode(ruleCode);
        Integer priority = config != null ? config.getSortOrder() : null;
        log.info("getRulePriority: ruleCode={}, priority={}", ruleCode, priority);
        return priority;
    }
}
