package com.jiangyang.messages.audit.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 审计日志配置类
 */
@Configuration
@EnableAsync
public class AuditLogConfig {

    /**
     * 审计日志数据源
     * 使用@ConfigurationProperties动态获取配置
     */
    @Bean(name = "auditLogDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.audit-log")
    public DataSource auditLogDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * 审计日志SqlSessionFactory
     * 使用独立的审计日志数据源，不会与主数据源冲突
     */
    @Bean(name = "auditLogSqlSessionFactory")
    public SqlSessionFactory auditLogSqlSessionFactory(@Qualifier("auditLogDataSource") DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean sqlSessionFactory = new MybatisSqlSessionFactoryBean();
        sqlSessionFactory.setDataSource(dataSource);
        
        // 设置Mapper XML文件位置
        sqlSessionFactory.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath:mapper/audit/*.xml")
        );
        
        // 配置MyBatis-Plus
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setBanner(false);
        sqlSessionFactory.setGlobalConfig(globalConfig);
        
        // 配置分页插件
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        sqlSessionFactory.setPlugins(interceptor);
        
        return sqlSessionFactory.getObject();
    }
    
    /**
     * 审计日志Mapper扫描配置
     * 指定使用auditLogSqlSessionFactory
     */
    @Bean(name = "auditLogMapperScannerConfigurer")
    public MapperScannerConfigurer auditLogMapperScannerConfigurer() {
        MapperScannerConfigurer scannerConfigurer = new MapperScannerConfigurer();
        scannerConfigurer.setBasePackage("com.jiangyang.messages.audit.mapper");
        scannerConfigurer.setSqlSessionFactoryBeanName("auditLogSqlSessionFactory");
        return scannerConfigurer;
    }

    /**
     * 审计日志线程池
     */
    @Bean(name = "auditLogExecutor")
    public Executor auditLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(1000);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("audit-log-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * 审计日志清理线程池
     */
    @Bean(name = "auditLogCleanExecutor")
    public Executor auditLogCleanExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("audit-clean-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
