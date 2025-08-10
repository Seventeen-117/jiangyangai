package com.signature.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.signature.entity.User;
import com.signature.entity.UserToken;
import com.signature.mapper.UserMapper;
import com.signature.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final String TOKEN_KEY_PREFIX = "USER:TOKEN:";
    private static final String USER_INFO_KEY_PREFIX = "USER:INFO:";
    private static final int TOKEN_CACHE_DAYS = 7;
    private static final int USER_CACHE_DAYS = 30;



    @Autowired(required = false)
    private RedisTemplate<String, UserToken> userTokenRedisTemplate;

    @Autowired(required = false)
    private RedisTemplate<String, User> userRedisTemplate;

    @Override
    @Transactional
    public UserToken loginWithSSO(String code) {
        try {
            log.info("开始SSO登录流程, 授权码: {}", code);
            
            // 简化实现：创建模拟用户令牌
            // 在实际实现中，这里应该调用SSO服务获取用户信息
            return createMockUserToken();
            
        } catch (Exception e) {
            log.error("SSO登录失败", e);
            throw new RuntimeException("SSO登录失败: " + e.getMessage());
        }
    }

    @Override
    public UserToken validateToken(String accessToken) {
        try {
            if (accessToken == null || accessToken.isEmpty()) {
                return null;
            }

            // 先从Redis缓存中获取
            if (userTokenRedisTemplate != null) {
                String cacheKey = TOKEN_KEY_PREFIX + accessToken;
                UserToken cachedToken = userTokenRedisTemplate.opsForValue().get(cacheKey);
                if (cachedToken != null && cachedToken.isValid()) {
                    log.debug("从Redis缓存获取到有效令牌: {}", accessToken);
                    return cachedToken;
                }
            }

            // 从数据库查询用户信息
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User> wrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            wrapper.eq(User::getAccessToken, accessToken)
                   .eq(User::getStatus, 1);
            User user = this.getOne(wrapper);
            if (user == null) {
                log.warn("未找到对应的用户信息: {}", accessToken);
                return null;
            }

            // 检查令牌是否过期
            if (user.getTokenExpireTime() == null || user.getTokenExpireTime().isBefore(LocalDateTime.now())) {
                log.warn("令牌已过期: {}", accessToken);
                return null;
            }

            // 构建用户令牌
            UserToken userToken = UserToken.builder()
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .accessToken(accessToken)
                    .tokenExpireTime(user.getTokenExpireTime())
                    .loginTime(user.getLastLoginTime())
                    .valid(true)
                    .build();

            // 缓存到Redis
            if (userTokenRedisTemplate != null) {
                String cacheKey = TOKEN_KEY_PREFIX + accessToken;
                userTokenRedisTemplate.opsForValue().set(cacheKey, userToken, TOKEN_CACHE_DAYS, java.util.concurrent.TimeUnit.DAYS);
            }

            return userToken;

        } catch (Exception e) {
            log.error("验证令牌时发生错误: {}", accessToken, e);
            return null;
        }
    }

    @Override
    public User getUserInfo(String userId) {
        try {
            // 先从Redis缓存中获取
            if (userRedisTemplate != null) {
                String cacheKey = USER_INFO_KEY_PREFIX + userId;
                User cachedUser = userRedisTemplate.opsForValue().get(cacheKey);
                if (cachedUser != null) {
                    log.debug("从Redis缓存获取到用户信息: {}", userId);
                    return cachedUser;
                }
            }

            // 从数据库查询
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User> wrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            wrapper.eq(User::getUserId, userId)
                   .eq(User::getStatus, 1);
            User user = this.getOne(wrapper);
            if (user != null && userRedisTemplate != null) {
                // 缓存到Redis
                String cacheKey = USER_INFO_KEY_PREFIX + userId;
                userRedisTemplate.opsForValue().set(cacheKey, user, USER_CACHE_DAYS, java.util.concurrent.TimeUnit.DAYS);
            }

            return user;

        } catch (Exception e) {
            log.error("获取用户信息时发生错误: {}", userId, e);
            return null;
        }
    }

    @Override
    @Transactional
    public UserToken refreshToken(String refreshToken) {
        try {
            log.info("刷新令牌: {}", refreshToken);
            
            // 简化实现：创建新的模拟令牌
            // 在实际实现中，这里应该验证refreshToken并生成新的accessToken
            return createMockUserToken();
            
        } catch (Exception e) {
            log.error("刷新令牌失败", e);
            throw new RuntimeException("刷新令牌失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public UserToken refreshTokenByUserId(String userId) {
        try {
            log.info("根据用户ID刷新令牌: {}", userId);
            
            // 简化实现：创建新的模拟令牌
            // 在实际实现中，这里应该根据用户ID生成新的令牌
            return createMockUserToken();
            
        } catch (Exception e) {
            log.error("根据用户ID刷新令牌失败", e);
            throw new RuntimeException("根据用户ID刷新令牌失败: " + e.getMessage());
        }
    }

    @Override
    public void logout(String accessToken) {
        try {
            log.info("用户登出: {}", accessToken);
            
            // 从Redis缓存中删除令牌
            if (userTokenRedisTemplate != null) {
                String cacheKey = TOKEN_KEY_PREFIX + accessToken;
                userTokenRedisTemplate.delete(cacheKey);
            }
            
            // 在实际实现中，这里还应该更新数据库中的令牌状态
            
        } catch (Exception e) {
            log.error("用户登出时发生错误: {}", accessToken, e);
        }
    }

    /**
     * 创建模拟用户令牌（用于测试）
     */
    private UserToken createMockUserToken() {
        return UserToken.builder()
                .userId("mock-user-" + UUID.randomUUID().toString().substring(0, 8))
                .username("Mock User")
                .email("mock@example.com")
                .accessToken("mock-token-" + UUID.randomUUID().toString().replace("-", ""))
                .tokenExpireTime(LocalDateTime.now().plusDays(7))
                .loginTime(LocalDateTime.now())
                .valid(true)
                .build();
    }
} 