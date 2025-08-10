package com.signature.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.signature.entity.ApiKeyPermission;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API密钥权限关联Service接口
 *
 * @author signature-service
 * @since 2025-01-01
 */
public interface ApiKeyPermissionService extends IService<ApiKeyPermission> {

    /**
     * 根据API密钥ID查询权限关联
     *
     * @param apiKeyId API密钥ID
     * @return API密钥权限关联列表
     */
    List<ApiKeyPermission> findByApiKeyId(Long apiKeyId);

    /**
     * 根据API密钥ID查询有效的权限关联
     *
     * @param apiKeyId API密钥ID
     * @param currentTime 当前时间
     * @return API密钥权限关联列表
     */
    List<ApiKeyPermission> findByApiKeyId(Long apiKeyId, LocalDateTime currentTime);

    /**
     * 根据权限ID查询权限关联
     *
     * @param permissionId 权限ID
     * @return API密钥权限关联列表
     */
    List<ApiKeyPermission> findByPermissionId(Long permissionId);

    /**
     * 检查API密钥是否有指定权限
     *
     * @param apiKeyId API密钥ID
     * @param permissionId 权限ID
     * @return 是否有权限
     */
    boolean hasPermission(Long apiKeyId, Long permissionId);

    /**
     * 检查API密钥是否有指定权限（考虑过期时间）
     *
     * @param apiKeyId API密钥ID
     * @param permissionId 权限ID
     * @param currentTime 当前时间
     * @return 是否有权限
     */
    boolean hasPermission(Long apiKeyId, Long permissionId, LocalDateTime currentTime);

    /**
     * 授予API密钥权限
     *
     * @param apiKeyId API密钥ID
     * @param permissionId 权限ID
     * @param grantedBy 授权人
     * @param expiresAt 过期时间
     * @return 是否授权成功
     */
    boolean grantPermission(Long apiKeyId, Long permissionId, String grantedBy, LocalDateTime expiresAt);

    /**
     * 撤销API密钥权限
     *
     * @param apiKeyId API密钥ID
     * @param permissionId 权限ID
     * @return 是否撤销成功
     */
    boolean revokePermission(Long apiKeyId, Long permissionId);

    /**
     * 批量授予API密钥权限
     *
     * @param apiKeyId API密钥ID
     * @param permissionIds 权限ID列表
     * @param grantedBy 授权人
     * @param expiresAt 过期时间
     * @return 是否授权成功
     */
    boolean grantPermissions(Long apiKeyId, List<Long> permissionIds, String grantedBy, LocalDateTime expiresAt);

    /**
     * 批量撤销API密钥权限
     *
     * @param apiKeyId API密钥ID
     * @param permissionIds 权限ID列表
     * @return 是否撤销成功
     */
    boolean revokePermissions(Long apiKeyId, List<Long> permissionIds);

    /**
     * 清理过期的权限关联
     *
     * @return 清理的数量
     */
    int cleanupExpiredPermissions();

    /**
     * 根据授权人查询权限关联
     *
     * @param grantedBy 授权人
     * @return API密钥权限关联列表
     */
    List<ApiKeyPermission> findByGrantedBy(String grantedBy);

    /**
     * 统计API密钥的权限数量
     *
     * @param apiKeyId API密钥ID
     * @return 权限数量
     */
    long countByApiKeyId(Long apiKeyId);

    /**
     * 统计权限的关联数量
     *
     * @param permissionId 权限ID
     * @return 关联数量
     */
    long countByPermissionId(Long permissionId);
}
