package com.signature.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.signature.entity.AuthorizationCode;
import com.signature.mapper.AuthorizationCodeMapper;
import com.signature.service.AuthorizationCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 授权码服务实现类
 *
 * @author signature-service
 * @since 2025-01-01
 */
@Slf4j
@Service
public class AuthorizationCodeServiceImpl extends ServiceImpl<AuthorizationCodeMapper, AuthorizationCode> implements AuthorizationCodeService {

    @Override
    public AuthorizationCode findValidByCode(String code) {
        if (!StringUtils.hasText(code)) {
            log.warn("findValidByCode: code is empty");
            return null;
        }

        LambdaQueryWrapper<AuthorizationCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuthorizationCode::getCode, code)
                .gt(AuthorizationCode::getExpiresAt, LocalDateTime.now())
                .eq(AuthorizationCode::getUsed, false);

        AuthorizationCode result = this.getOne(wrapper);
        log.info("findValidByCode: code={}, found={}", code, result != null);
        return result;
    }

    @Override
    public AuthorizationCode findByCode(String code) {
        if (!StringUtils.hasText(code)) {
            log.warn("findByCode: code is empty");
            return null;
        }

        LambdaQueryWrapper<AuthorizationCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuthorizationCode::getCode, code);

        AuthorizationCode result = this.getOne(wrapper);
        log.info("findByCode: code={}, found={}", code, result != null);
        return result;
    }

    @Override
    @Transactional
    public boolean markAsUsed(String code) {
        if (!StringUtils.hasText(code)) {
            log.warn("markAsUsed: code is empty");
            return false;
        }

        LambdaUpdateWrapper<AuthorizationCode> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AuthorizationCode::getCode, code)
                .set(AuthorizationCode::getUsed, true)
                .set(AuthorizationCode::getUpdatedAt, LocalDateTime.now());

        boolean updated = this.update(wrapper);
        log.info("markAsUsed: code={}, updated={}", code, updated);
        return updated;
    }

    @Override
    @Transactional
    public int cleanupExpired() {
        LambdaQueryWrapper<AuthorizationCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(AuthorizationCode::getExpiresAt, LocalDateTime.now());

        List<AuthorizationCode> expiredCodes = this.list(wrapper);
        int count = 0;
        
        if (!expiredCodes.isEmpty()) {
            boolean deleted = this.remove(wrapper);
            count = deleted ? expiredCodes.size() : 0;
        }

        log.info("cleanupExpired: cleaned {} expired authorization codes", count);
        return count;
    }

    @Override
    @Transactional
    public AuthorizationCode createAuthorizationCode(AuthorizationCode authorizationCode) {
        if (authorizationCode == null) {
            log.error("createAuthorizationCode: authorizationCode is null");
            return null;
        }

        // 验证必需字段
        if (!StringUtils.hasText(authorizationCode.getCode())) {
            log.error("createAuthorizationCode: code is required");
            return null;
        }

        if (!StringUtils.hasText(authorizationCode.getClientId())) {
            log.error("createAuthorizationCode: clientId is required");
            return null;
        }

        if (!StringUtils.hasText(authorizationCode.getUserId())) {
            log.error("createAuthorizationCode: userId is required");
            return null;
        }

        // 设置默认值
        if (authorizationCode.getUsed() == null) {
            authorizationCode.setUsed(false);
        }
        if (authorizationCode.getCreatedAt() == null) {
            authorizationCode.setCreatedAt(LocalDateTime.now());
        }
        if (authorizationCode.getUpdatedAt() == null) {
            authorizationCode.setUpdatedAt(LocalDateTime.now());
        }

        boolean saved = this.save(authorizationCode);
        if (saved) {
            log.info("createAuthorizationCode: created authorization code id={}, code={}", 
                    authorizationCode.getId(), authorizationCode.getCode());
            return authorizationCode;
        } else {
            log.error("createAuthorizationCode: failed to save authorization code");
            return null;
        }
    }

    @Override
    @Transactional
    public AuthorizationCode generateAuthorizationCode(String clientId, String userId, 
                                                     String redirectUri, String scope, 
                                                     String state, int expiresIn) {
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(userId)) {
            log.error("generateAuthorizationCode: clientId or userId is empty");
            return null;
        }

        // 生成授权码
        String code = UUID.randomUUID().toString().replace("-", "");
        
        AuthorizationCode authorizationCode = new AuthorizationCode();
        authorizationCode.setCode(code);
        authorizationCode.setClientId(clientId);
        authorizationCode.setUserId(userId);
        authorizationCode.setRedirectUri(redirectUri);
        authorizationCode.setScope(scope);
        authorizationCode.setState(state);
        authorizationCode.setExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));
        authorizationCode.setUsed(false);
        authorizationCode.setCreatedAt(LocalDateTime.now());
        authorizationCode.setUpdatedAt(LocalDateTime.now());

        return createAuthorizationCode(authorizationCode);
    }

    @Override
    public boolean validateAuthorizationCode(String code, String clientId) {
        if (!StringUtils.hasText(code) || !StringUtils.hasText(clientId)) {
            log.warn("validateAuthorizationCode: code or clientId is empty");
            return false;
        }

        LambdaQueryWrapper<AuthorizationCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuthorizationCode::getCode, code)
                .eq(AuthorizationCode::getClientId, clientId)
                .gt(AuthorizationCode::getExpiresAt, LocalDateTime.now())
                .eq(AuthorizationCode::getUsed, false);

        long count = this.count(wrapper);
        boolean valid = count > 0;
        log.info("validateAuthorizationCode: code={}, clientId={}, valid={}", code, clientId, valid);
        return valid;
    }

    @Override
    public List<AuthorizationCode> findByClientId(String clientId) {
        if (!StringUtils.hasText(clientId)) {
            log.warn("findByClientId: clientId is empty");
            return List.of();
        }

        LambdaQueryWrapper<AuthorizationCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuthorizationCode::getClientId, clientId)
                .orderByDesc(AuthorizationCode::getCreatedAt);

        List<AuthorizationCode> result = this.list(wrapper);
        log.info("findByClientId: clientId={}, count={}", clientId, result.size());
        return result;
    }

    @Override
    public List<AuthorizationCode> findByUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            log.warn("findByUserId: userId is empty");
            return List.of();
        }

        LambdaQueryWrapper<AuthorizationCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuthorizationCode::getUserId, userId)
                .orderByDesc(AuthorizationCode::getCreatedAt);

        List<AuthorizationCode> result = this.list(wrapper);
        log.info("findByUserId: userId={}, count={}", userId, result.size());
        return result;
    }

    @Override
    public List<AuthorizationCode> findUnused() {
        LambdaQueryWrapper<AuthorizationCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuthorizationCode::getUsed, false)
                .gt(AuthorizationCode::getExpiresAt, LocalDateTime.now())
                .orderByDesc(AuthorizationCode::getCreatedAt);

        List<AuthorizationCode> result = this.list(wrapper);
        log.info("findUnused: count={}", result.size());
        return result;
    }

    @Override
    public List<AuthorizationCode> findExpired() {
        LambdaQueryWrapper<AuthorizationCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(AuthorizationCode::getExpiresAt, LocalDateTime.now())
                .orderByDesc(AuthorizationCode::getCreatedAt);

        List<AuthorizationCode> result = this.list(wrapper);
        log.info("findExpired: count={}", result.size());
        return result;
    }

    @Override
    @Transactional
    public int cleanupByClientId(String clientId) {
        if (!StringUtils.hasText(clientId)) {
            log.warn("cleanupByClientId: clientId is empty");
            return 0;
        }

        LambdaQueryWrapper<AuthorizationCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuthorizationCode::getClientId, clientId);

        List<AuthorizationCode> codes = this.list(wrapper);
        int count = 0;
        
        if (!codes.isEmpty()) {
            boolean deleted = this.remove(wrapper);
            count = deleted ? codes.size() : 0;
        }

        log.info("cleanupByClientId: clientId={}, cleaned={}", clientId, count);
        return count;
    }

    @Override
    public Object getAuthorizationCodeStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // 统计总数
        long totalCount = this.count();
        statistics.put("total", totalCount);

        // 统计未使用数量
        LambdaQueryWrapper<AuthorizationCode> unusedWrapper = new LambdaQueryWrapper<>();
        unusedWrapper.eq(AuthorizationCode::getUsed, false);
        long unusedCount = this.count(unusedWrapper);
        statistics.put("unused", unusedCount);

        // 统计已使用数量
        LambdaQueryWrapper<AuthorizationCode> usedWrapper = new LambdaQueryWrapper<>();
        usedWrapper.eq(AuthorizationCode::getUsed, true);
        long usedCount = this.count(usedWrapper);
        statistics.put("used", usedCount);

        // 统计过期数量
        LambdaQueryWrapper<AuthorizationCode> expiredWrapper = new LambdaQueryWrapper<>();
        expiredWrapper.lt(AuthorizationCode::getExpiresAt, LocalDateTime.now());
        long expiredCount = this.count(expiredWrapper);
        statistics.put("expired", expiredCount);

        // 统计有效数量
        LambdaQueryWrapper<AuthorizationCode> validWrapper = new LambdaQueryWrapper<>();
        validWrapper.eq(AuthorizationCode::getUsed, false)
                   .gt(AuthorizationCode::getExpiresAt, LocalDateTime.now());
        long validCount = this.count(validWrapper);
        statistics.put("valid", validCount);

        log.info("getAuthorizationCodeStatistics: statistics={}", statistics);
        return statistics;
    }
}
