package com.signature.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.signature.entity.ApiPermission;
import com.signature.mapper.ApiPermissionMapper;
import com.signature.service.ApiPermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API权限Service实现类
 *
 * @author signature-service
 * @since 2025-01-01
 */
@Slf4j
@Service
public class ApiPermissionServiceImpl extends ServiceImpl<ApiPermissionMapper, ApiPermission> 
        implements ApiPermissionService {

    @Override
    public ApiPermission findByPermissionCode(String permissionCode) {
        if (!StringUtils.hasText(permissionCode)) {
            log.warn("findByPermissionCode: permissionCode is empty");
            return null;
        }

        LambdaQueryWrapper<ApiPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiPermission::getPermissionCode, permissionCode)
               .orderByDesc(ApiPermission::getCreatedAt)
               .last("LIMIT 1");

        ApiPermission result = this.getOne(wrapper);
        log.info("findByPermissionCode: permissionCode={}, found={}", permissionCode, result != null);
        return result;
    }

    @Override
    public ApiPermission findEnabledByPermissionCode(String permissionCode) {
        if (!StringUtils.hasText(permissionCode)) {
            log.warn("findEnabledByPermissionCode: permissionCode is empty");
            return null;
        }

        LambdaQueryWrapper<ApiPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiPermission::getPermissionCode, permissionCode)
               .eq(ApiPermission::getStatus, 1)
               .orderByDesc(ApiPermission::getCreatedAt)
               .last("LIMIT 1");

        ApiPermission result = this.getOne(wrapper);
        log.info("findEnabledByPermissionCode: permissionCode={}, found={}", permissionCode, result != null);
        return result;
    }

    @Override
    public List<ApiPermission> findByPermissionType(String permissionType) {
        if (!StringUtils.hasText(permissionType)) {
            log.warn("findByPermissionType: permissionType is empty");
            return List.of();
        }

        LambdaQueryWrapper<ApiPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiPermission::getPermissionType, permissionType)
               .orderByAsc(ApiPermission::getPermissionCode)
               .orderByDesc(ApiPermission::getCreatedAt);

        List<ApiPermission> result = this.list(wrapper);
        log.info("findByPermissionType: permissionType={}, count={}", permissionType, result.size());
        return result;
    }

    @Override
    public List<ApiPermission> findEnabledByPermissionType(String permissionType) {
        if (!StringUtils.hasText(permissionType)) {
            log.warn("findEnabledByPermissionType: permissionType is empty");
            return List.of();
        }

        LambdaQueryWrapper<ApiPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiPermission::getPermissionType, permissionType)
               .eq(ApiPermission::getStatus, 1)
               .orderByAsc(ApiPermission::getPermissionCode)
               .orderByDesc(ApiPermission::getCreatedAt);

        List<ApiPermission> result = this.list(wrapper);
        log.info("findEnabledByPermissionType: permissionType={}, count={}", permissionType, result.size());
        return result;
    }

    @Override
    public List<ApiPermission> findAllEnabled() {
        LambdaQueryWrapper<ApiPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiPermission::getStatus, 1)
               .orderByAsc(ApiPermission::getPermissionCode)
               .orderByDesc(ApiPermission::getCreatedAt);

        List<ApiPermission> result = this.list(wrapper);
        log.info("findAllEnabled: count={}", result.size());
        return result;
    }

    @Override
    public List<ApiPermission> findByStatus(Integer status) {
        if (status == null) {
            log.warn("findByStatus: status is null");
            return List.of();
        }

        LambdaQueryWrapper<ApiPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiPermission::getStatus, status)
               .orderByAsc(ApiPermission::getPermissionCode)
               .orderByDesc(ApiPermission::getCreatedAt);

        List<ApiPermission> result = this.list(wrapper);
        log.info("findByStatus: status={}, count={}", status, result.size());
        return result;
    }

    @Override
    public List<ApiPermission> findByParentId(Long parentId) {
        // 如果ApiPermission没有parentId字段，这个方法返回空列表
        log.warn("findByParentId: ApiPermission entity does not have parentId field");
        return List.of();
    }

    @Override
    public List<ApiPermission> findEnabledByParentId(Long parentId) {
        // 如果ApiPermission没有parentId字段，这个方法返回空列表
        log.warn("findEnabledByParentId: ApiPermission entity does not have parentId field");
        return List.of();
    }

    @Override
    @Transactional
    public ApiPermission createPermission(ApiPermission permission) {
        if (permission == null) {
            throw new IllegalArgumentException("ApiPermission cannot be null");
        }

        // 验证权限编码唯一性
        if (StringUtils.hasText(permission.getPermissionCode())) {
            ApiPermission existing = findByPermissionCode(permission.getPermissionCode());
            if (existing != null) {
                throw new RuntimeException("权限编码已存在: " + permission.getPermissionCode());
            }
        }

        // 设置默认值
        permission.setCreatedAt(LocalDateTime.now());
        permission.setUpdatedAt(LocalDateTime.now());
        if (permission.getStatus() == null) {
            permission.setStatus(1);
        }

        boolean saved = this.save(permission);
        if (saved) {
            log.info("createPermission: Created permission with ID {}", permission.getId());
        }
        return saved ? permission : null;
    }

    @Override
    @Transactional
    public ApiPermission updatePermission(ApiPermission permission) {
        if (permission == null || permission.getId() == null) {
            throw new IllegalArgumentException("ApiPermission and ID cannot be null");
        }

        // 验证权限编码唯一性（排除自身）
        if (StringUtils.hasText(permission.getPermissionCode())) {
            ApiPermission existing = findByPermissionCode(permission.getPermissionCode());
            if (existing != null && !existing.getId().equals(permission.getId())) {
                throw new RuntimeException("权限编码已存在: " + permission.getPermissionCode());
            }
        }

        permission.setUpdatedAt(LocalDateTime.now());
        boolean updated = this.updateById(permission);
        if (updated) {
            log.info("updatePermission: Updated permission with ID {}", permission.getId());
        }
        return updated ? permission : null;
    }

    @Override
    @Transactional
    public boolean deletePermission(Long id) {
        if (id == null) {
            log.warn("deletePermission: id is null");
            return false;
        }

        boolean deleted = this.removeById(id);
        log.info("deletePermission: id={}, deleted={}", id, deleted);
        return deleted;
    }

    @Override
    @Transactional
    public boolean enablePermission(Long id) {
        if (id == null) {
            log.warn("enablePermission: id is null");
            return false;
        }

        LambdaUpdateWrapper<ApiPermission> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ApiPermission::getId, id)
               .set(ApiPermission::getStatus, 1)
               .set(ApiPermission::getUpdatedAt, LocalDateTime.now());

        boolean enabled = this.update(wrapper);
        log.info("enablePermission: id={}, enabled={}", id, enabled);
        return enabled;
    }

    @Override
    @Transactional
    public boolean disablePermission(Long id) {
        if (id == null) {
            log.warn("disablePermission: id is null");
            return false;
        }

        LambdaUpdateWrapper<ApiPermission> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ApiPermission::getId, id)
               .set(ApiPermission::getStatus, 0)
               .set(ApiPermission::getUpdatedAt, LocalDateTime.now());

        boolean disabled = this.update(wrapper);
        log.info("disablePermission: id={}, disabled={}", id, disabled);
        return disabled;
    }

    @Override
    public List<ApiPermission> findByPermissionCodes(List<String> permissionCodes) {
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            log.warn("findByPermissionCodes: permissionCodes is empty");
            return List.of();
        }

        LambdaQueryWrapper<ApiPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ApiPermission::getPermissionCode, permissionCodes)
               .orderByAsc(ApiPermission::getPermissionCode)
               .orderByDesc(ApiPermission::getCreatedAt);

        List<ApiPermission> result = this.list(wrapper);
        log.info("findByPermissionCodes: count={}", result.size());
        return result;
    }

    @Override
    public Page<ApiPermission> findPage(Integer page, Integer size, String permissionType, Integer status) {
        Page<ApiPermission> pageParam = new Page<>(page != null ? page : 1, size != null ? size : 10);
        
        LambdaQueryWrapper<ApiPermission> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(permissionType)) {
            wrapper.eq(ApiPermission::getPermissionType, permissionType);
        }
        if (status != null) {
            wrapper.eq(ApiPermission::getStatus, status);
        }
        
        wrapper.orderByAsc(ApiPermission::getPermissionCode)
               .orderByDesc(ApiPermission::getCreatedAt);

        Page<ApiPermission> result = this.page(pageParam, wrapper);
        log.info("findPage: page={}, size={}, permissionType={}, status={}, total={}", 
                page, size, permissionType, status, result.getTotal());
        return result;
    }

    @Override
    public boolean existsByPermissionCode(String permissionCode) {
        if (!StringUtils.hasText(permissionCode)) {
            return false;
        }

        LambdaQueryWrapper<ApiPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiPermission::getPermissionCode, permissionCode);

        long count = this.count(wrapper);
        boolean exists = count > 0;
        log.info("existsByPermissionCode: permissionCode={}, exists={}", permissionCode, exists);
        return exists;
    }

    @Override
    public boolean isPermissionEnabled(String permissionCode) {
        ApiPermission permission = findByPermissionCode(permissionCode);
        boolean enabled = permission != null && permission.getStatus() == 1;
        log.info("isPermissionEnabled: permissionCode={}, enabled={}", permissionCode, enabled);
        return enabled;
    }

    @Override
    public Integer getPermissionLevel(String permissionCode) {
        // 如果ApiPermission没有level字段，根据权限编码计算级别
        if (!StringUtils.hasText(permissionCode)) {
            return null;
        }
        
        // 根据权限编码的点号分隔数量计算级别
        int level = permissionCode.split("\\.").length;
        log.info("getPermissionLevel: permissionCode={}, level={}", permissionCode, level);
        return level;
    }

    @Override
    public List<ApiPermission> findByApiPathAndMethod(String apiPath, String httpMethod) {
        if (!StringUtils.hasText(apiPath)) {
            log.warn("findByApiPathAndMethod: apiPath is empty");
            return List.of();
        }

        // 如果ApiPermission没有apiPath和httpMethod字段，使用permissionCode进行模糊匹配
        LambdaQueryWrapper<ApiPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiPermission::getStatus, 1);
        
        if (StringUtils.hasText(httpMethod)) {
            wrapper.like(ApiPermission::getPermissionCode, httpMethod.toLowerCase() + ":");
        }
        wrapper.like(ApiPermission::getPermissionCode, apiPath);

        List<ApiPermission> result = this.list(wrapper);
        log.info("findByApiPathAndMethod: apiPath={}, httpMethod={}, count={}", apiPath, httpMethod, result.size());
        return result;
    }

    @Override
    public boolean isWildcardPermission(String permissionCode) {
        if (!StringUtils.hasText(permissionCode)) {
            return false;
        }
        
        boolean isWildcard = permissionCode.contains("*") || permissionCode.contains("**") || 
                           permissionCode.endsWith(":*") || permissionCode.equals("*");
        log.info("isWildcardPermission: permissionCode={}, isWildcard={}", permissionCode, isWildcard);
        return isWildcard;
    }

    @Override
    public String getPermissionDescription(String permissionCode) {
        ApiPermission permission = findByPermissionCode(permissionCode);
        String description = permission != null ? permission.getDescription() : null;
        log.info("getPermissionDescription: permissionCode={}, description={}", permissionCode, 
                description != null ? "found" : "not found");
        return description;
    }
}
