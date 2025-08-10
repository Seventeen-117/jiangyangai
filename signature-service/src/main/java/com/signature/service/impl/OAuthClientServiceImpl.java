package com.signature.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.signature.entity.OAuthClient;
import com.signature.mapper.OAuthClientMapper;
import com.signature.service.OAuthClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * OAuth客户端服务实现类
 *
 * @author signature-service
 * @since 2025-01-01
 */
@Slf4j
@Service
public class OAuthClientServiceImpl extends ServiceImpl<OAuthClientMapper, OAuthClient> implements OAuthClientService {

    @Override
    public OAuthClient findByClientId(String clientId) {
        if (!StringUtils.hasText(clientId)) {
            log.warn("findByClientId: clientId is empty");
            return null;
        }

        LambdaQueryWrapper<OAuthClient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OAuthClient::getClientId, clientId);

        OAuthClient result = this.getOne(wrapper);
        log.info("findByClientId: clientId={}, found={}", clientId, result != null);
        return result;
    }

    @Override
    public OAuthClient findEnabledByClientId(String clientId) {
        if (!StringUtils.hasText(clientId)) {
            log.warn("findEnabledByClientId: clientId is empty");
            return null;
        }

        LambdaQueryWrapper<OAuthClient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OAuthClient::getClientId, clientId)
               .eq(OAuthClient::getStatus, 1);

        OAuthClient result = this.getOne(wrapper);
        log.info("findEnabledByClientId: clientId={}, found={}", clientId, result != null);
        return result;
    }

    @Override
    public int validateCredentials(String clientId, String clientSecret) {
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            log.warn("validateCredentials: clientId or clientSecret is empty");
            return 0;
        }

        LambdaQueryWrapper<OAuthClient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OAuthClient::getClientId, clientId)
               .eq(OAuthClient::getClientSecret, clientSecret)
               .eq(OAuthClient::getStatus, 1);

        int count = Math.toIntExact(this.count(wrapper));
        log.info("validateCredentials: clientId={}, valid={}", clientId, count > 0);
        return count;
    }

    @Override
    public OAuthClient findByCredentials(String clientId, String clientSecret) {
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            log.warn("findByCredentials: clientId or clientSecret is empty");
            return null;
        }

        LambdaQueryWrapper<OAuthClient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OAuthClient::getClientId, clientId)
               .eq(OAuthClient::getClientSecret, clientSecret)
               .eq(OAuthClient::getStatus, 1);

        OAuthClient result = this.getOne(wrapper);
        log.info("findByCredentials: clientId={}, found={}", clientId, result != null);
        return result;
    }

    @Override
    @Transactional
    public OAuthClient createOAuthClient(OAuthClient oAuthClient) {
        if (oAuthClient == null) {
            log.warn("createOAuthClient: oAuthClient is null");
            return null;
        }

        // 检查客户端ID是否已存在
        if (StringUtils.hasText(oAuthClient.getClientId())) {
            OAuthClient existing = findByClientId(oAuthClient.getClientId());
            if (existing != null) {
                throw new RuntimeException("客户端ID已存在: " + oAuthClient.getClientId());
            }
        }

        // 设置默认值
        oAuthClient.setCreatedAt(LocalDateTime.now());
        oAuthClient.setUpdatedAt(LocalDateTime.now());
        if (oAuthClient.getStatus() == null) {
            oAuthClient.setStatus(1);
        }

        boolean saved = this.save(oAuthClient);
        if (saved) {
            log.info("createOAuthClient: Created OAuth client with ID {}", oAuthClient.getId());
        }
        return saved ? oAuthClient : null;
    }

    @Override
    @Transactional
    public OAuthClient updateOAuthClient(OAuthClient oAuthClient) {
        if (oAuthClient == null || oAuthClient.getId() == null) {
            log.warn("updateOAuthClient: oAuthClient or ID is null");
            return null;
        }

        // 检查客户端ID是否已存在（排除当前记录）
        if (StringUtils.hasText(oAuthClient.getClientId())) {
            OAuthClient existing = findByClientId(oAuthClient.getClientId());
            if (existing != null && !existing.getId().equals(oAuthClient.getId())) {
                throw new RuntimeException("客户端ID已存在: " + oAuthClient.getClientId());
            }
        }

        oAuthClient.setUpdatedAt(LocalDateTime.now());
        boolean updated = this.updateById(oAuthClient);
        if (updated) {
            log.info("updateOAuthClient: Updated OAuth client with ID {}", oAuthClient.getId());
        }
        return updated ? oAuthClient : null;
    }

    @Override
    @Transactional
    public boolean deleteOAuthClient(Long id) {
        if (id == null) {
            log.warn("deleteOAuthClient: id is null");
            return false;
        }

        boolean deleted = this.removeById(id);
        log.info("deleteOAuthClient: id={}, deleted={}", id, deleted);
        return deleted;
    }

    @Override
    @Transactional
    public boolean updateStatus(Long id, Integer status) {
        if (id == null || status == null) {
            log.warn("updateStatus: id or status is null");
            return false;
        }

        OAuthClient oAuthClient = new OAuthClient();
        oAuthClient.setId(id);
        oAuthClient.setStatus(status);
        oAuthClient.setUpdatedAt(LocalDateTime.now());

        boolean updated = this.updateById(oAuthClient);
        log.info("updateStatus: id={}, status={}, updated={}", id, status, updated);
        return updated;
    }
}
