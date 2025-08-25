package com.bgpay.bgai.cache;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    @Value("${spring.data.redis.timeout:5000}")
    private long redisTimeout;

    @Value("${spring.data.redis.connect-timeout:5000}")
    private long connectTimeout;

    @Value("${spring.data.redis.lettuce.pool.max-active:16}")
    private int connectionPoolSize;

    @Value("${spring.data.redis.lettuce.pool.min-idle:4}")
    private int connectionMinimumIdleSize;

    @Value("${spring.data.redis.lettuce.pool.max-idle:8}")
    private int maxIdleSize;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort)
                .setPassword(redisPassword)
                .setDatabase(redisDatabase)
                .setConnectionPoolSize(connectionPoolSize)
                .setConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setIdleConnectionTimeout((int) redisTimeout)
                .setConnectTimeout((int) connectTimeout)
                .setTimeout((int) redisTimeout)
                .setRetryAttempts(3)
                .setRetryInterval(1500)
                .setKeepAlive(true)
                .setTcpNoDelay(true);
        return Redisson.create(config);
    }
}