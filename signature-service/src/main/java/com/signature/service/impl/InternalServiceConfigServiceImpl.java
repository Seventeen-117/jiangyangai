package com.signature.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.signature.entity.InternalServiceConfig;
import com.signature.mapper.InternalServiceConfigMapper;
import com.signature.service.InternalServiceConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 内部服务配置Service实现类
 *
 * @author signature-service
 * @since 2025-01-01
 */
@Slf4j
@Service
public class InternalServiceConfigServiceImpl extends ServiceImpl<InternalServiceConfigMapper, InternalServiceConfig> 
        implements InternalServiceConfigService {

    @Override
    public InternalServiceConfig findByServiceCode(String serviceCode) {
        if (!StringUtils.hasText(serviceCode)) {
            log.warn("findByServiceCode: serviceCode is empty");
            return null;
        }

        LambdaQueryWrapper<InternalServiceConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InternalServiceConfig::getServiceCode, serviceCode)
               .orderByDesc(InternalServiceConfig::getCreatedTime)
               .last("LIMIT 1");

        InternalServiceConfig result = this.getOne(wrapper);
        log.info("findByServiceCode: serviceCode={}, found={}", serviceCode, result != null);
        return result;
    }

    @Override
    public InternalServiceConfig findEnabledByServiceCode(String serviceCode) {
        if (!StringUtils.hasText(serviceCode)) {
            log.warn("findEnabledByServiceCode: serviceCode is empty");
            return null;
        }

        LambdaQueryWrapper<InternalServiceConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InternalServiceConfig::getServiceCode, serviceCode)
               .eq(InternalServiceConfig::getStatus, 1)

               .orderByDesc(InternalServiceConfig::getCreatedTime)
               .last("LIMIT 1");

        InternalServiceConfig result = this.getOne(wrapper);
        log.info("findEnabledByServiceCode: serviceCode={}, found={}", serviceCode, result != null);
        return result;
    }

    @Override
    public InternalServiceConfig findByApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            log.warn("findByApiKey: apiKey is empty");
            return null;
        }

        LambdaQueryWrapper<InternalServiceConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InternalServiceConfig::getApiKey, apiKey)

               .orderByDesc(InternalServiceConfig::getCreatedTime)
               .last("LIMIT 1");

        InternalServiceConfig result = this.getOne(wrapper);
        log.info("findByApiKey: found={}", result != null);
        return result;
    }

    @Override
    public InternalServiceConfig findEnabledByApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            log.warn("findEnabledByApiKey: apiKey is empty");
            return null;
        }

        LambdaQueryWrapper<InternalServiceConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InternalServiceConfig::getApiKey, apiKey)
               .eq(InternalServiceConfig::getStatus, 1)

               .orderByDesc(InternalServiceConfig::getCreatedTime)
               .last("LIMIT 1");

        InternalServiceConfig result = this.getOne(wrapper);
        log.info("findEnabledByApiKey: found={}", result != null);
        return result;
    }

    @Override
    public List<InternalServiceConfig> findByServiceType(String serviceType) {
        if (!StringUtils.hasText(serviceType)) {
            log.warn("findByServiceType: serviceType is empty");
            return List.of();
        }

        LambdaQueryWrapper<InternalServiceConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InternalServiceConfig::getServiceType, serviceType)

               .orderByAsc(InternalServiceConfig::getSortOrder)
               .orderByDesc(InternalServiceConfig::getCreatedTime);

        List<InternalServiceConfig> result = this.list(wrapper);
        log.info("findByServiceType: serviceType={}, count={}", serviceType, result.size());
        return result;
    }

    @Override
    public List<InternalServiceConfig> findEnabledByServiceType(String serviceType) {
        if (!StringUtils.hasText(serviceType)) {
            log.warn("findEnabledByServiceType: serviceType is empty");
            return List.of();
        }

        LambdaQueryWrapper<InternalServiceConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InternalServiceConfig::getServiceType, serviceType)
               .eq(InternalServiceConfig::getStatus, 1)

               .orderByAsc(InternalServiceConfig::getSortOrder)
               .orderByDesc(InternalServiceConfig::getCreatedTime);

        List<InternalServiceConfig> result = this.list(wrapper);
        log.info("findEnabledByServiceType: serviceType={}, count={}", serviceType, result.size());
        return result;
    }

    @Override
    public List<InternalServiceConfig> findAllEnabled() {
        LambdaQueryWrapper<InternalServiceConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InternalServiceConfig::getStatus, 1)

               .orderByAsc(InternalServiceConfig::getSortOrder)
               .orderByDesc(InternalServiceConfig::getCreatedTime);

        List<InternalServiceConfig> result = this.list(wrapper);
        log.info("findAllEnabled: count={}", result.size());
        return result;
    }

    @Override
    public List<InternalServiceConfig> findByStatus(Integer status) {
        if (status == null) {
            log.warn("findByStatus: status is null");
            return List.of();
        }

        LambdaQueryWrapper<InternalServiceConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InternalServiceConfig::getStatus, status)

               .orderByAsc(InternalServiceConfig::getSortOrder)
               .orderByDesc(InternalServiceConfig::getCreatedTime);

        List<InternalServiceConfig> result = this.list(wrapper);
        log.info("findByStatus: status={}, count={}", status, result.size());
        return result;
    }

    @Override
    @Transactional
    public InternalServiceConfig createServiceConfig(InternalServiceConfig serviceConfig) {
        if (serviceConfig == null) {
            throw new IllegalArgumentException("InternalServiceConfig cannot be null");
        }

        // 验证服务编码唯一性
        if (StringUtils.hasText(serviceConfig.getServiceCode())) {
            InternalServiceConfig existing = findByServiceCode(serviceConfig.getServiceCode());
            if (existing != null) {
                throw new RuntimeException("服务编码已存在: " + serviceConfig.getServiceCode());
            }
        }

        // 设置默认值
        serviceConfig.setCreatedTime(LocalDateTime.now());
        serviceConfig.setUpdatedTime(LocalDateTime.now());
        if (serviceConfig.getStatus() == null) {
            serviceConfig.setStatus(1);
        }


        boolean saved = this.save(serviceConfig);
        if (saved) {
            log.info("createServiceConfig: Created service config with ID {}", serviceConfig.getId());
        }
        return saved ? serviceConfig : null;
    }

    @Override
    @Transactional
    public InternalServiceConfig updateServiceConfig(InternalServiceConfig serviceConfig) {
        if (serviceConfig == null || serviceConfig.getId() == null) {
            throw new IllegalArgumentException("InternalServiceConfig and ID cannot be null");
        }

        // 验证服务编码唯一性（排除自身）
        if (StringUtils.hasText(serviceConfig.getServiceCode())) {
            InternalServiceConfig existing = findByServiceCode(serviceConfig.getServiceCode());
            if (existing != null && !existing.getId().equals(serviceConfig.getId())) {
                throw new RuntimeException("服务编码已存在: " + serviceConfig.getServiceCode());
            }
        }

        serviceConfig.setUpdatedTime(LocalDateTime.now());
        boolean updated = this.updateById(serviceConfig);
        if (updated) {
            log.info("updateServiceConfig: Updated service config with ID {}", serviceConfig.getId());
        }
        return updated ? serviceConfig : null;
    }

    @Override
    @Transactional
    public boolean deleteServiceConfig(Long id) {
        if (id == null) {
            log.warn("deleteServiceConfig: id is null");
            return false;
        }

        // 禁用服务配置（因为没有deleted字段，改为设置status=0）
        LambdaUpdateWrapper<InternalServiceConfig> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(InternalServiceConfig::getId, id)
               .set(InternalServiceConfig::getStatus, 0)
               .set(InternalServiceConfig::getUpdatedTime, LocalDateTime.now());

        boolean deleted = this.update(wrapper);
        log.info("deleteServiceConfig: id={}, deleted={}", id, deleted);
        return deleted;
    }

    @Override
    @Transactional
    public boolean enableServiceConfig(Long id) {
        if (id == null) {
            log.warn("enableServiceConfig: id is null");
            return false;
        }

        LambdaUpdateWrapper<InternalServiceConfig> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(InternalServiceConfig::getId, id)
               .set(InternalServiceConfig::getStatus, 1)
               .set(InternalServiceConfig::getUpdatedTime, LocalDateTime.now());

        boolean enabled = this.update(wrapper);
        log.info("enableServiceConfig: id={}, enabled={}", id, enabled);
        return enabled;
    }

    @Override
    @Transactional
    public boolean disableServiceConfig(Long id) {
        if (id == null) {
            log.warn("disableServiceConfig: id is null");
            return false;
        }

        LambdaUpdateWrapper<InternalServiceConfig> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(InternalServiceConfig::getId, id)
               .set(InternalServiceConfig::getStatus, 0)
               .set(InternalServiceConfig::getUpdatedTime, LocalDateTime.now());

        boolean disabled = this.update(wrapper);
        log.info("disableServiceConfig: id={}, disabled={}", id, disabled);
        return disabled;
    }

    @Override
    public List<InternalServiceConfig> findByServiceCodes(List<String> serviceCodes) {
        if (serviceCodes == null || serviceCodes.isEmpty()) {
            log.warn("findByServiceCodes: serviceCodes is empty");
            return List.of();
        }

        LambdaQueryWrapper<InternalServiceConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(InternalServiceConfig::getServiceCode, serviceCodes)
               .orderByAsc(InternalServiceConfig::getSortOrder)
               .orderByDesc(InternalServiceConfig::getCreatedTime);

        List<InternalServiceConfig> result = this.list(wrapper);
        log.info("findByServiceCodes: count={}", result.size());
        return result;
    }

    @Override
    public Page<InternalServiceConfig> findPage(Integer page, Integer size, String serviceType, Integer status) {
        Page<InternalServiceConfig> pageParam = new Page<>(page != null ? page : 1, size != null ? size : 10);
        
        LambdaQueryWrapper<InternalServiceConfig> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(serviceType)) {
            wrapper.eq(InternalServiceConfig::getServiceType, serviceType);
        }
        if (status != null) {
            wrapper.eq(InternalServiceConfig::getStatus, status);
        }
        
        wrapper.orderByAsc(InternalServiceConfig::getSortOrder)
               .orderByDesc(InternalServiceConfig::getCreatedTime);

        Page<InternalServiceConfig> result = this.page(pageParam, wrapper);
        log.info("findPage: page={}, size={}, serviceType={}, status={}, total={}", 
                page, size, serviceType, status, result.getTotal());
        return result;
    }
}
