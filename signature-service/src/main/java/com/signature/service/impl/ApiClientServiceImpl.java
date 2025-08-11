package com.signature.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.signature.entity.ApiClient;
import com.signature.mapper.ApiClientMapper;
import com.signature.service.ApiClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API客户端服务实现类
 *
 * @author signature-service
 * @since 2025-01-01
 */
@Slf4j
@Service
public class ApiClientServiceImpl extends ServiceImpl<ApiClientMapper, ApiClient> implements ApiClientService {

    @Override
    public ApiClient findEnabledByClientId(String clientId) {
        if (!StringUtils.hasText(clientId)) {
            log.warn("findEnabledByClientId: clientId is empty");
            return null;
        }

        LambdaQueryWrapper<ApiClient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiClient::getClientId, clientId)
                .eq(ApiClient::getStatus, 1)
                .eq(ApiClient::getDeleted, false);

        ApiClient result = this.getOne(wrapper);
        log.info("findEnabledByClientId: clientId={}, found={}", clientId, result != null);
        return result;
    }

    @Override
    public List<ApiClient> findByClientType(String clientType) {
        if (!StringUtils.hasText(clientType)) {
            log.warn("findByClientType: clientType is empty");
            return List.of();
        }

        LambdaQueryWrapper<ApiClient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiClient::getClientType, clientType)
                .eq(ApiClient::getDeleted, false)
                .orderByDesc(ApiClient::getCreateTime);

        List<ApiClient> result = this.list(wrapper);
        log.info("findByClientType: clientType={}, count={}", clientType, result.size());
        return result;
    }

    @Override
    public List<ApiClient> findAllEnabled() {
        LambdaQueryWrapper<ApiClient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiClient::getStatus, 1)
                .eq(ApiClient::getDeleted, false)
                .orderByDesc(ApiClient::getCreateTime);

        List<ApiClient> result = this.list(wrapper);
        log.info("findAllEnabled: count={}", result.size());
        return result;
    }

    @Override
    public ApiClient findByClientIdWithDeleted(String clientId) {
        if (!StringUtils.hasText(clientId)) {
            log.warn("findByClientIdWithDeleted: clientId is empty");
            return null;
        }

        LambdaQueryWrapper<ApiClient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiClient::getClientId, clientId);

        ApiClient result = this.getOne(wrapper);
        log.info("findByClientIdWithDeleted: clientId={}, found={}", clientId, result != null);
        return result;
    }

    @Override
    public Long countByStatus(Integer status) {
        if (status == null) {
            log.warn("countByStatus: status is null");
            return 0L;
        }

        LambdaQueryWrapper<ApiClient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiClient::getStatus, status)
                .eq(ApiClient::getDeleted, false);

        long count = this.count(wrapper);
        log.info("countByStatus: status={}, count={}", status, count);
        return count;
    }

    @Override
    @Transactional
    public ApiClient createApiClient(ApiClient apiClient) {
        if (apiClient == null) {
            log.error("createApiClient: apiClient is null");
            return null;
        }

        // 验证必需字段
        if (!StringUtils.hasText(apiClient.getClientId())) {
            log.error("createApiClient: clientId is required");
            return null;
        }

        if (!StringUtils.hasText(apiClient.getClientName())) {
            log.error("createApiClient: clientName is required");
            return null;
        }

        // 检查客户端ID是否已存在
        ApiClient existing = findByClientIdWithDeleted(apiClient.getClientId());
        if (existing != null) {
            log.error("createApiClient: clientId already exists: {}", apiClient.getClientId());
            return null;
        }

        // 设置默认值
        if (apiClient.getStatus() == null) {
            apiClient.setStatus(1); // 默认启用
        }
        if (apiClient.getDeleted() == null) {
            apiClient.setDeleted(false);
        }
        if (apiClient.getVersion() == null) {
            apiClient.setVersion(1);
        }

        boolean saved = this.save(apiClient);
        if (saved) {
            log.info("createApiClient: created client id={}, clientId={}", 
                    apiClient.getId(), apiClient.getClientId());
            return apiClient;
        } else {
            log.error("createApiClient: failed to save client");
            return null;
        }
    }

    @Override
    @Transactional
    public ApiClient updateApiClient(ApiClient apiClient) {
        if (apiClient == null || apiClient.getId() == null) {
            log.error("updateApiClient: apiClient or id is null");
            return null;
        }

        ApiClient existing = this.getById(apiClient.getId());
        if (existing == null) {
            log.error("updateApiClient: client not found, id={}", apiClient.getId());
            return null;
        }

        // 如果更新了clientId，检查是否与其他客户端冲突
        if (StringUtils.hasText(apiClient.getClientId()) && 
            !apiClient.getClientId().equals(existing.getClientId())) {
            
            ApiClient conflicting = findByClientIdWithDeleted(apiClient.getClientId());
            if (conflicting != null && !conflicting.getId().equals(apiClient.getId())) {
                log.error("updateApiClient: clientId already exists: {}", apiClient.getClientId());
                return null;
            }
        }

        boolean updated = this.updateById(apiClient);
        if (updated) {
            log.info("updateApiClient: updated client id={}, clientId={}", 
                    apiClient.getId(), apiClient.getClientId());
            return apiClient;
        } else {
            log.error("updateApiClient: failed to update client");
            return null;
        }
    }

    @Override
    @Transactional
    public boolean deleteApiClient(Long id) {
        if (id == null) {
            log.warn("deleteApiClient: id is null");
            return false;
        }

        // 使用逻辑删除
        boolean deleted = this.removeById(id);
        log.info("deleteApiClient: id={}, deleted={}", id, deleted);
        return deleted;
    }

    @Override
    @Transactional
    public boolean updateStatus(Long id, Integer status) {
        if (id == null || status == null) {
            log.warn("updateStatus: id or status is null");
            return false;
        }

        LambdaUpdateWrapper<ApiClient> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ApiClient::getId, id)
                .set(ApiClient::getStatus, status)
                .set(ApiClient::getUpdateTime, LocalDateTime.now());

        boolean updated = this.update(wrapper);
        log.info("updateStatus: id={}, status={}, updated={}", id, status, updated);
        return updated;
    }

    @Override
    @Transactional
    public int batchUpdateStatus(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty() || status == null) {
            log.warn("batchUpdateStatus: ids is empty or status is null");
            return 0;
        }

        LambdaUpdateWrapper<ApiClient> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(ApiClient::getId, ids)
                .set(ApiClient::getStatus, status)
                .set(ApiClient::getUpdateTime, LocalDateTime.now());

        boolean updated = this.update(wrapper);
        int updatedCount = updated ? ids.size() : 0;
        log.info("batchUpdateStatus: ids={}, status={}, updated={}", ids, status, updatedCount);
        return updatedCount;
    }

    @Override
    public boolean validateCredentials(String clientId, String clientSecret) {
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            log.warn("validateCredentials: clientId or clientSecret is empty");
            return false;
        }

        LambdaQueryWrapper<ApiClient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiClient::getClientId, clientId)
                .eq(ApiClient::getClientSecret, clientSecret)
                .eq(ApiClient::getStatus, 1)
                .eq(ApiClient::getDeleted, false);

        long count = this.count(wrapper);
        boolean valid = count > 0;
        log.info("validateCredentials: clientId={}, valid={}", clientId, valid);
        return valid;
    }

    @Override
    public List<ApiClient> findByScope(String scope) {
        if (!StringUtils.hasText(scope)) {
            log.warn("findByScope: scope is empty");
            return List.of();
        }

        LambdaQueryWrapper<ApiClient> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(ApiClient::getScope, scope)
                .eq(ApiClient::getStatus, 1)
                .eq(ApiClient::getDeleted, false)
                .orderByDesc(ApiClient::getCreateTime);

        List<ApiClient> result = this.list(wrapper);
        log.info("findByScope: scope={}, count={}", scope, result.size());
        return result;
    }

    @Override
    public boolean supportsGrantType(String clientId, String grantType) {
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(grantType)) {
            log.warn("supportsGrantType: clientId or grantType is empty");
            return false;
        }

        ApiClient client = findEnabledByClientId(clientId);
        if (client == null) {
            log.warn("supportsGrantType: client not found: {}", clientId);
            return false;
        }

        String supportedGrantTypes = client.getGrantType();
        if (!StringUtils.hasText(supportedGrantTypes)) {
            log.warn("supportsGrantType: no grant types defined for client: {}", clientId);
            return false;
        }

        // 检查是否支持指定的授权类型（多个授权类型用逗号分隔）
        boolean supports = supportedGrantTypes.contains(grantType);
        log.info("supportsGrantType: clientId={}, grantType={}, supports={}", 
                clientId, grantType, supports);
        return supports;
    }
}
