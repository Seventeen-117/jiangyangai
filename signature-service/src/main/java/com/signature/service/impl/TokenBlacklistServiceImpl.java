package com.signature.service.impl;

import com.signature.service.TokenBlacklistService;
import com.signature.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 令牌黑名单服务实现类
 * 使用Redis和内存缓存管理已撤销的令牌
 */
@Slf4j
@Service
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JwtUtils jwtUtils;

    // Redis key前缀
    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    // 内存缓存，用于快速检查
    private final ConcurrentMap<String, Long> memoryBlacklist = new ConcurrentHashMap<>();

    @Override
    public void blacklistToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return;
            }

            // 获取令牌的过期时间
            Date expirationDate = jwtUtils.getExpirationDate(token);
            if (expirationDate == null) {
                log.warn("Cannot blacklist invalid token");
                return;
            }

            long expirationTime = expirationDate.getTime();
            long currentTime = System.currentTimeMillis();

            // 如果令牌已经过期，不需要加入黑名单
            if (expirationTime <= currentTime) {
                log.info("Token is already expired, skipping blacklist");
                return;
            }

            // 计算剩余时间（秒）
            long ttlSeconds = (expirationTime - currentTime) / 1000;

            // 存储到Redis
            String redisKey = BLACKLIST_PREFIX + token;
            redisTemplate.opsForValue().set(redisKey, "1", 
                java.time.Duration.ofSeconds(ttlSeconds));

            // 存储到内存缓存
            memoryBlacklist.put(token, expirationTime);

            log.info("Token blacklisted successfully, expires in {} seconds", ttlSeconds);
        } catch (Exception e) {
            log.error("Failed to blacklist token", e);
        }
    }

    @Override
    public boolean isTokenBlacklisted(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return false;
            }

            // 首先检查内存缓存
            Long expirationTime = memoryBlacklist.get(token);
            if (expirationTime != null) {
                // 检查是否已过期
                if (expirationTime <= System.currentTimeMillis()) {
                    memoryBlacklist.remove(token);
                    return false;
                }
                return true;
            }

            // 检查Redis
            String redisKey = BLACKLIST_PREFIX + token;
            String result = redisTemplate.opsForValue().get(redisKey);
            
            if (result != null) {
                // 如果Redis中存在，也添加到内存缓存
                Date expirationDate = jwtUtils.getExpirationDate(token);
                if (expirationDate != null) {
                    memoryBlacklist.put(token, expirationDate.getTime());
                }
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("Failed to check token blacklist status", e);
            return false;
        }
    }

    @Override
    public void removeFromBlacklist(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return;
            }

            // 从Redis中移除
            String redisKey = BLACKLIST_PREFIX + token;
            redisTemplate.delete(redisKey);

            // 从内存缓存中移除
            memoryBlacklist.remove(token);

            log.info("Token removed from blacklist");
        } catch (Exception e) {
            log.error("Failed to remove token from blacklist", e);
        }
    }

    @Override
    @Scheduled(fixedRate = 300000) // 每5分钟执行一次
    public void cleanupExpiredEntries() {
        try {
            long currentTime = System.currentTimeMillis();
            
            // 清理内存缓存中的过期条目
            memoryBlacklist.entrySet().removeIf(entry -> {
                boolean expired = entry.getValue() <= currentTime;
                if (expired) {
                    log.debug("Removing expired token from memory blacklist");
                }
                return expired;
            });

            log.info("Blacklist cleanup completed, memory cache size: {}", memoryBlacklist.size());
        } catch (Exception e) {
            log.error("Failed to cleanup expired blacklist entries", e);
        }
    }

    /**
     * 获取黑名单统计信息
     *
     * @return 统计信息
     */
    public BlacklistStats getBlacklistStats() {
        return BlacklistStats.builder()
                .memoryCacheSize(memoryBlacklist.size())
                .build();
    }

    /**
     * 黑名单统计信息
     */
    public static class BlacklistStats {
        private int memoryCacheSize;

        public static BlacklistStatsBuilder builder() {
            return new BlacklistStatsBuilder();
        }

        public static class BlacklistStatsBuilder {
            private BlacklistStats stats = new BlacklistStats();

            public BlacklistStatsBuilder memoryCacheSize(int memoryCacheSize) {
                stats.memoryCacheSize = memoryCacheSize;
                return this;
            }

            public BlacklistStats build() {
                return stats;
            }
        }

        public int getMemoryCacheSize() {
            return memoryCacheSize;
        }
    }
}
