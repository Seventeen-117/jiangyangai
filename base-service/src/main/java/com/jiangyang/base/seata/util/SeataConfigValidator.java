package com.jiangyang.base.seata.util;

import com.jiangyang.base.seata.properties.SeataProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Seata配置验证工具类
 * 用于验证Seata配置是否正确
 */
@Slf4j
@Component
public class SeataConfigValidator implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private SeataProperties seataProperties;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        validateSeataConfig();
    }

    /**
     * 验证Seata配置
     */
    public void validateSeataConfig() {
        log.info("开始验证Seata配置...");
        
        boolean isValid = true;
        
        // 验证基础配置
        if (!validateBasicConfig()) {
            isValid = false;
        }
        
        // 验证注册中心配置
        if (!validateRegistryConfig()) {
            isValid = false;
        }
        
        // 验证配置中心配置
        if (!validateConfigCenterConfig()) {
            isValid = false;
        }
        
        // 验证存储配置
        if (!validateStoreConfig()) {
            isValid = false;
        }
        
        if (isValid) {
            log.info("✅ Seata配置验证通过");
        } else {
            log.error("❌ Seata配置验证失败，请检查配置");
        }
    }

    /**
     * 验证基础配置
     */
    private boolean validateBasicConfig() {
        log.info("验证基础配置...");
        
        boolean isValid = true;
        
        // 检查enabled
        if (seataProperties.getEnabled() == null) {
            log.error("❌ seata.enabled 未配置");
            isValid = false;
        } else if (!seataProperties.getEnabled()) {
            log.warn("⚠️ seata.enabled = false，Seata功能已禁用");
            return true; // 如果禁用，其他配置不需要验证
        }
        
        // 检查applicationId
        if (!StringUtils.hasText(seataProperties.getApplicationId())) {
            log.error("❌ seata.application-id 未配置");
            isValid = false;
        } else {
            log.info("✅ seata.application-id = {}", seataProperties.getApplicationId());
        }
        
        // 检查txServiceGroup
        if (!StringUtils.hasText(seataProperties.getTxServiceGroup())) {
            log.error("❌ seata.tx-service-group 未配置");
            isValid = false;
        } else {
            log.info("✅ seata.tx-service-group = {}", seataProperties.getTxServiceGroup());
        }
        
        // 检查timeout
        if (seataProperties.getTimeout() == null) {
            log.warn("⚠️ seata.timeout 未配置，将使用默认值");
        } else {
            log.info("✅ seata.timeout = {}", seataProperties.getTimeout());
        }
        
        return isValid;
    }

    /**
     * 验证注册中心配置
     */
    private boolean validateRegistryConfig() {
        log.info("验证注册中心配置...");
        
        SeataProperties.Registry registry = seataProperties.getRegistry();
        if (registry == null) {
            log.error("❌ seata.registry 配置缺失");
            return false;
        }
        
        boolean isValid = true;
        
        // 检查type
        if (!StringUtils.hasText(registry.getType())) {
            log.error("❌ seata.registry.type 未配置");
            isValid = false;
        } else {
            log.info("✅ seata.registry.type = {}", registry.getType());
        }
        
        // 检查serverAddr
        if (!StringUtils.hasText(registry.getServerAddr())) {
            log.error("❌ seata.registry.server-addr 未配置");
            isValid = false;
        } else {
            log.info("✅ seata.registry.server-addr = {}", registry.getServerAddr());
        }
        
        // 检查group
        if (!StringUtils.hasText(registry.getGroup())) {
            log.error("❌ seata.registry.group 未配置");
            isValid = false;
        } else {
            log.info("✅ seata.registry.group = {}", registry.getGroup());
        }
        
        return isValid;
    }

    /**
     * 验证配置中心配置
     */
    private boolean validateConfigCenterConfig() {
        log.info("验证配置中心配置...");
        
        SeataProperties.Config config = seataProperties.getConfig();
        if (config == null) {
            log.error("❌ seata.config 配置缺失");
            return false;
        }
        
        boolean isValid = true;
        
        // 检查type
        if (!StringUtils.hasText(config.getType())) {
            log.error("❌ seata.config.type 未配置");
            isValid = false;
        } else {
            log.info("✅ seata.config.type = {}", config.getType());
        }
        
        // 检查serverAddr
        if (!StringUtils.hasText(config.getServerAddr())) {
            log.error("❌ seata.config.server-addr 未配置");
            isValid = false;
        } else {
            log.info("✅ seata.config.server-addr = {}", config.getServerAddr());
        }
        
        // 检查group
        if (!StringUtils.hasText(config.getGroup())) {
            log.error("❌ seata.config.group 未配置");
            isValid = false;
        } else {
            log.info("✅ seata.config.group = {}", config.getGroup());
        }
        
        return isValid;
    }

    /**
     * 验证存储配置
     */
    private boolean validateStoreConfig() {
        log.info("验证存储配置...");
        
        SeataProperties.Store store = seataProperties.getStore();
        if (store == null) {
            log.warn("⚠️ seata.store 配置缺失，将使用默认配置");
            return true;
        }
        
        // 检查mode
        if (!StringUtils.hasText(store.getMode())) {
            log.warn("⚠️ seata.store.mode 未配置，将使用默认值 'file'");
        } else {
            log.info("✅ seata.store.mode = {}", store.getMode());
            
            // 如果使用数据库存储，验证数据库配置
            if ("db".equals(store.getMode())) {
                return validateDatabaseConfig(store.getDatabase());
            }
        }
        
        return true;
    }

    /**
     * 验证数据库配置
     */
    private boolean validateDatabaseConfig(SeataProperties.Store.Database database) {
        if (database == null) {
            log.error("❌ seata.store.database 配置缺失");
            return false;
        }
        
        boolean isValid = true;
        
        // 检查必要的数据库配置
        if (!StringUtils.hasText(database.getUrl())) {
            log.error("❌ seata.store.database.url 未配置");
            isValid = false;
        } else {
            log.info("✅ seata.store.database.url = {}", database.getUrl());
        }
        
        if (!StringUtils.hasText(database.getUsername())) {
            log.error("❌ seata.store.database.username 未配置");
            isValid = false;
        } else {
            log.info("✅ seata.store.database.username = {}", database.getUsername());
        }
        
        if (!StringUtils.hasText(database.getPassword())) {
            log.error("❌ seata.store.database.password 未配置");
            isValid = false;
        } else {
            log.info("✅ seata.store.database.password = ***");
        }
        
        return isValid;
    }
}
