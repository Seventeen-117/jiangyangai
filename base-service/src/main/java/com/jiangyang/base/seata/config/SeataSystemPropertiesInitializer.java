package com.jiangyang.base.seata.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * Seata系统属性初始化器
 * 用于设置Seata相关的系统属性
 */
@Slf4j
@Component
public class SeataSystemPropertiesInitializer implements InitializingBean {

    @Override
    public void afterPropertiesSet() throws Exception {
        // 设置Seata系统属性
        setSeataSystemProperties();
        log.info("Seata系统属性初始化完成");
    }

    /**
     * 设置Seata系统属性
     */
    private void setSeataSystemProperties() {
        log.info("初始化Seata系统属性...");

        // 确保Seata使用Nacos作为注册中心和配置中心
        // 这些属性需要在Seata初始化之前设置，否则Seata可能默认使用文件配置
        System.setProperty("seata.config.type", "nacos");
        System.setProperty("seata.registry.type", "nacos");

        // 设置Nacos服务器地址
        System.setProperty("seata.config.nacos.server-addr", "8.133.246.113:8848");
        System.setProperty("seata.registry.nacos.server-addr", "8.133.246.113:8848");
        
        // 设置Nacos命名空间
        System.setProperty("seata.config.nacos.namespace", "4a49080d-0e41-4a48-9013-4a30a5c3ffb6");
        System.setProperty("seata.registry.nacos.namespace", "4a49080d-0e41-4a48-9013-4a30a5c3ffb6");
        
        // 设置Nacos分组
        System.setProperty("seata.config.nacos.group", "SEATA_GROUP");
        System.setProperty("seata.registry.nacos.group", "SEATA_GROUP");
        
        // 设置配置文件的data-id
        System.setProperty("seata.config.nacos.data-id", "seataServer.properties");

        // 禁用Saga状态机自动注册，避免重复注册错误
        System.setProperty("seata.saga.state-machine.auto-register", "false");
        
        // 设置Seata日志级别
        System.setProperty("seata.log.exceptionRate", "100");
        
        // 设置事务超时时间（默认60秒）
        if (System.getProperty("seata.client.rm.report.retry.count") == null) {
            System.setProperty("seata.client.rm.report.retry.count", "5");
        }
        
        // 设置全局事务超时时间
        if (System.getProperty("seata.client.tm.degrade.check.allow") == null) {
            System.setProperty("seata.client.tm.degrade.check.allow", "false");
        }
        
        log.info("Seata系统属性初始化完成: seata.config.type=nacos, seata.registry.type=nacos, seata.saga.state-machine.auto-register=false");
    }
}
