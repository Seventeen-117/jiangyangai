package com.bgpay.bgai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {


    @Bean("asyncTaskExcutor")
    public Executor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(200);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("AsyncReq-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    // 网络IO专用（大吞吐量）
    @Bean("ioTaskExecutor")
    public ThreadPoolTaskExecutor ioExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(50);
        executor.setMaxPoolSize(200);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("io-");
        return executor;
    }

    // 数据库专用（小并发）
    @Bean("dbTaskExecutor")
    public ThreadPoolTaskExecutor dbExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(5000);
        executor.setThreadNamePrefix("db-");
        return executor;
    }
    @Bean("fileWriteExecutor")
    public Executor fileWriteExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("FileWrite-");
        executor.initialize();
        return executor;
    }

    @Bean("mqAsyncExecutor")
    public Executor mqAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(10000);
        executor.setThreadNamePrefix("MQ-Async-");
        executor.initialize();
        return executor;
    }
    @Bean(name = "blockingExecutor")
    public Executor blockingExecutor() {
        return new ThreadPoolTaskExecutor() {{
            setCorePoolSize(10);
            setMaxPoolSize(50);
            setQueueCapacity(100);
            setThreadNamePrefix("file-process-");
        }};
    }
}
