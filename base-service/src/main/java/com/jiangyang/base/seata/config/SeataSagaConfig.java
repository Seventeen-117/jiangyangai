package com.jiangyang.base.seata.config;

import io.seata.saga.engine.StateMachineEngine;
import io.seata.saga.engine.config.DbStateMachineConfig;
import io.seata.saga.engine.impl.ProcessCtrlStateMachineEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Seata Saga配置
 * 用于自定义Seata Saga的行为，特别是避免状态机重复注册
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "seata.saga", name = "enabled", havingValue = "true", matchIfMissing = false)
public class SeataSagaConfig {

    /**
     * 配置Saga状态机引擎
     */
    @Bean
    public StateMachineEngine stateMachineEngine(DataSource dataSource) {
        log.info("初始化Seata Saga状态机引擎");
        
        // 设置系统属性，确保Saga状态机不会自动注册
        System.setProperty("seata.saga.state-machine.auto-register", "false");
        
        // 创建数据库状态机配置
        DbStateMachineConfig config = new DbStateMachineConfig();
        config.setDataSource(dataSource);
        config.setDbType("mysql");
        config.setTablePrefix("seata_");
        
        // 创建状态机引擎
        ProcessCtrlStateMachineEngine engine = new ProcessCtrlStateMachineEngine();
        engine.setStateMachineConfig(config);
        
        log.info("Seata Saga状态机引擎初始化完成");
        return engine;
    }
}
