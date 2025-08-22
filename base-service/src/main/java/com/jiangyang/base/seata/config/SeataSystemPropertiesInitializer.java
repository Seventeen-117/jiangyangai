package com.jiangyang.base.seata.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;

/**
 * Seata系统属性初始化器
 * 用于设置Seata相关的系统属性
 */
@Slf4j
@Component
public class SeataSystemPropertiesInitializer implements InitializingBean {

    private final Environment environment;

    public SeataSystemPropertiesInitializer(Environment environment) {
        this.environment = environment;
    }

    /**
     * PostConstruct：在依赖注入完成后立即执行
     * 用于设置从应用配置中读取的属性
     */
    @PostConstruct
    public void init() {
        log.info("PostConstruct初始化Seata系统属性...");
        setSeataSystemProperties();
        log.info("Seata系统属性初始化完成");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 这个方法现在主要用于日志记录，实际初始化在@PostConstruct中完成
        log.info("Seata系统属性初始化完成");
    }

    /**
     * 设置Seata系统属性
     */
    private void setSeataSystemProperties() {
        log.info("初始化Seata系统属性(从应用配置复制到System Properties)...");

        // 设置Seata必需的系统属性
        setRequiredSeataProperties();
        
        // 复制应用配置中的属性到系统属性
        copyIfPresent("seata.config.type");
        copyIfPresent("seata.registry.type");

        // Nacos Config
        copyIfPresent("seata.config.nacos.server-addr");
        copyIfPresent("seata.config.nacos.namespace");
        copyIfPresent("seata.config.nacos.group");
        copyIfPresent("seata.config.nacos.data-id");
        copyIfPresent("seata.config.nacos.username");
        copyIfPresent("seata.config.nacos.password");

        // Nacos Registry
        copyIfPresent("seata.registry.nacos.server-addr");
        copyIfPresent("seata.registry.nacos.namespace");
        copyIfPresent("seata.registry.nacos.group");
        copyIfPresent("seata.registry.nacos.cluster");
        copyIfPresent("seata.registry.nacos.username");
        copyIfPresent("seata.registry.nacos.password");

        // Optional client-side tuning copied only if configured
        copyIfPresent("seata.saga.state-machine.auto-register");
        copyIfPresent("seata.log.exceptionRate");
        copyIfPresent("seata.client.rm.report.retry.count");
        copyIfPresent("seata.client.tm.degrade.check.allow");

        log.info("Seata系统属性初始化完成");
    }

    /**
     * 设置Seata必需的系统属性
     */
    private void setRequiredSeataProperties() {
        log.info("设置Seata必需的系统属性...");
        
        // 从应用配置中读取并设置所有 Seata 相关配置
        // 基础配置
        copyIfPresent("seata.application-id");
        copyIfPresent("seata.tx-service-group");
        copyIfPresent("seata.timeout");
        copyIfPresent("seata.enable-auto-data-source-proxy");
        copyIfPresent("seata.data-source-proxy-mode");
        
        // 事务组映射配置 - 转换为 Seata 期望的系统属性格式
        copyAndTransform("seata.service.vgroup-mapping.bgai-service-tx-group", "service.vgroupMapping.bgai-service-tx-group");
        copyAndTransform("seata.service.vgroup-mapping.messages-service-tx-group", "service.vgroupMapping.messages-service-tx-group");
        copyAndTransform("seata.service.vgroup-mapping.gateway-service-tx-group", "service.vgroupMapping.gateway-service-tx-group");
        
        // 集群配置 - 转换为 Seata 期望的系统属性格式
        copyAndTransform("seata.service.default.grouplist", "service.default.grouplist");
        
        // 注册中心配置
        copyIfPresent("seata.registry.type");
        copyIfPresent("seata.registry.nacos.server-addr");
        copyIfPresent("seata.registry.nacos.namespace");
        copyIfPresent("seata.registry.nacos.group");
        copyIfPresent("seata.registry.nacos.cluster");
        copyIfPresent("seata.registry.nacos.username");
        copyIfPresent("seata.registry.nacos.password");
        
        // 配置中心配置
        copyIfPresent("seata.config.type");
        copyIfPresent("seata.config.nacos.server-addr");
        copyIfPresent("seata.config.nacos.namespace");
        copyIfPresent("seata.config.nacos.group");
        copyIfPresent("seata.config.nacos.data-id");
        copyIfPresent("seata.config.nacos.username");
        copyIfPresent("seata.config.nacos.password");
        
        // 存储配置
        copyIfPresent("seata.store.mode");
        copyIfPresent("seata.store.file.dir");
        copyIfPresent("seata.store.db.datasource");
        copyIfPresent("seata.store.db.db-type");
        copyIfPresent("seata.store.db.driver-class-name");
        copyIfPresent("seata.store.db.url");
        copyIfPresent("seata.store.db.user");
        copyIfPresent("seata.store.db.password");
        copyIfPresent("seata.store.db.min-conn");
        copyIfPresent("seata.store.db.max-conn");
        
        // 锁配置
        copyIfPresent("seata.lock.mode");
        
        // 会话配置
        copyIfPresent("seata.store.session.mode");
        copyIfPresent("seata.store.session.file.dir");
        
        // Saga 配置
        copyIfPresent("seata.saga.enabled");
        copyIfPresent("seata.saga.state-machine.auto-register");
        
        // 客户端配置
        copyIfPresent("seata.client.rm.report-success-enable");
        copyIfPresent("seata.client.rm.table-meta-check-enable");
        copyIfPresent("seata.client.rm.report-retry-count");
        copyIfPresent("seata.client.tm.commit-retry-count");
        copyIfPresent("seata.client.tm.rollback-retry-count");
        copyIfPresent("seata.client.tm.default-global-transaction-timeout");
        
        log.info("Seata必需的系统属性设置完成（从配置文件读取并转换为系统属性格式）");
    }

    /**
     * 复制配置并转换为指定的系统属性名称
     */
    private void copyAndTransform(String configKey, String systemPropertyKey) {
        String value = environment.getProperty(configKey);
        if (StringUtils.hasText(value)) {
            System.setProperty(systemPropertyKey, value);
            log.debug("Set system property: {}={}", systemPropertyKey, value);
        } else {
            log.warn("配置项 {} 未设置，使用默认值", configKey);
            // 设置默认值
            if (systemPropertyKey.contains("vgroupMapping")) {
                System.setProperty(systemPropertyKey, "default");
                log.info("Set default system property: {}={}", systemPropertyKey, "default");
            } else if (systemPropertyKey.contains("grouplist")) {
                System.setProperty(systemPropertyKey, "8.133.246.113:8091");
                log.info("Set default system property: {}={}", systemPropertyKey, "8.133.246.113:8091");
            }
        }
    }

    private void copyIfPresent(String key) {
        String val = environment.getProperty(key);
        if (StringUtils.hasText(val)) {
            System.setProperty(key, val);
            log.debug("Set system property: {}=***masked***", key);
        }
    }
}
