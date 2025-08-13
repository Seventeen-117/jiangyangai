package com.jiangyang.messages.service.impl;

import com.jiangyang.messages.service.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis缓存服务实现类
 * 
 * @author jiangyang
 * @since 1.0.0
 */
@Slf4j
@Service
public class RedisCacheServiceImpl implements CacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 业务状态缓存key前缀
     */
    private static final String BUSINESS_STATUS_KEY_PREFIX = "business:status:";

    /**
     * 默认过期时间（24小时）
     */
    private static final long DEFAULT_EXPIRE_SECONDS = 24 * 60 * 60;

    @Override
    public boolean updateBusinessStatus(String messageId, String status) {
        try {
            String key = buildBusinessStatusKey(messageId);
            redisTemplate.opsForValue().set(key, status, DEFAULT_EXPIRE_SECONDS, TimeUnit.SECONDS);
            log.debug("业务状态缓存更新成功: messageId={}, status={}", messageId, status);
            return true;
        } catch (Exception e) {
            log.error("业务状态缓存更新失败: messageId={}, status={}, error={}", messageId, status, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getBusinessStatus(String messageId) {
        try {
            String key = buildBusinessStatusKey(messageId);
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.debug("获取业务状态缓存成功: messageId={}, status={}", messageId, value);
                return value.toString();
            }
            log.debug("业务状态缓存不存在: messageId={}", messageId);
            return null;
        } catch (Exception e) {
            log.error("获取业务状态缓存失败: messageId={}, error={}", messageId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean deleteBusinessStatus(String messageId) {
        try {
            String key = buildBusinessStatusKey(messageId);
            Boolean result = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(result)) {
                log.debug("删除业务状态缓存成功: messageId={}", messageId);
                return true;
            }
            log.debug("业务状态缓存删除失败或不存在: messageId={}", messageId);
            return false;
        } catch (Exception e) {
            log.error("删除业务状态缓存失败: messageId={}, error={}", messageId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean expireBusinessStatus(String messageId, long expireSeconds) {
        try {
            String key = buildBusinessStatusKey(messageId);
            Boolean result = redisTemplate.expire(key, expireSeconds, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(result)) {
                log.debug("设置业务状态缓存过期时间成功: messageId={}, expireSeconds={}", messageId, expireSeconds);
                return true;
            }
            log.debug("设置业务状态缓存过期时间失败: messageId={}, expireSeconds={}", messageId, expireSeconds);
            return false;
        } catch (Exception e) {
            log.error("设置业务状态缓存过期时间失败: messageId={}, expireSeconds={}, error={}", messageId, expireSeconds, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean existsBusinessStatus(String messageId) {
        try {
            String key = buildBusinessStatusKey(messageId);
            Boolean result = redisTemplate.hasKey(key);
            log.debug("检查业务状态缓存存在性: messageId={}, exists={}", messageId, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("检查业务状态缓存存在性失败: messageId={}, error={}", messageId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 构建业务状态缓存key
     * 
     * @param messageId 消息ID
     * @return 完整的缓存key
     */
    private String buildBusinessStatusKey(String messageId) {
        return BUSINESS_STATUS_KEY_PREFIX + messageId;
    }
}
