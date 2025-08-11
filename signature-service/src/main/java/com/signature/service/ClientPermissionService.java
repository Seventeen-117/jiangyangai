package com.signature.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.signature.entity.ClientPermission;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 客户端权限Service接口
 *
 * @author signature-service
 * @since 2025-01-01
 */
public interface ClientPermissionService extends IService<ClientPermission> {

    /**
     * 根据客户端ID查询权限关联
     *
     * @param clientId 客户端ID
     * @return 客户端权限关联列表
     */
    List<ClientPermission> findByClientId(String clientId);

    /**
     * 根据客户端ID查询有效的权限关联
     *
     * @param clientId 客户端ID
     * @param currentTime 当前时间
     * @return 客户端权限关联列表
     */
    List<ClientPermission> findByClientId(String clientId, LocalDateTime currentTime);

    /**
     * 根据权限ID查询权限关联
     *
     * @param permissionId 权限ID
     * @return 客户端权限关联列表
     */
    List<ClientPermission> findByPermissionId(Long permissionId);

    /**
     * 检查客户端是否有指定权限
     *
     * @param clientId 客户端ID
     * @param permissionId 权限ID
     * @return 是否有权限
     */
    boolean hasPermission(String clientId, Long permissionId);

    /**
     * 检查客户端是否有指定权限（考虑过期时间）
     *
     * @param clientId 客户端ID
     * @param permissionId 权限ID
     * @param currentTime 当前时间
     * @return 是否有权限
     */
    boolean hasPermission(String clientId, Long permissionId, LocalDateTime currentTime);

    /**
     * 授予客户端权限
     *
     * @param clientId 客户端ID
     * @param permissionId 权限ID
     * @param grantedBy 授权人
     * @param expiresAt 过期时间
     * @return 是否授权成功
     */
    boolean grantPermission(String clientId, Long permissionId, String grantedBy, LocalDateTime expiresAt);

    /**
     * 撤销客户端权限
     *
     * @param clientId 客户端ID
     * @param permissionId 权限ID
     * @return 是否撤销成功
     */
    boolean revokePermission(String clientId, Long permissionId);

    /**
     * 批量授予客户端权限
     *
     * @param clientId 客户端ID
     * @param permissionIds 权限ID列表
     * @param grantedBy 授权人
     * @param expiresAt 过期时间
     * @return 是否授权成功
     */
    boolean grantPermissions(String clientId, List<Long> permissionIds, String grantedBy, LocalDateTime expiresAt);

    /**
     * 批量撤销客户端权限
     *
     * @param clientId 客户端ID
     * @param permissionIds 权限ID列表
     * @return 是否撤销成功
     */
    boolean revokePermissions(String clientId, List<Long> permissionIds);

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
     * @return 客户端权限关联列表
     */
    List<ClientPermission> findByGrantedBy(String grantedBy);

    /**
     * 统计客户端的权限数量
     *
     * @param clientId 客户端ID
     * @return 权限数量
     */
    long countByClientId(String clientId);

    /**
     * 统计权限的关联数量
     *
     * @param permissionId 权限ID
     * @return 关联数量
     */
    long countByPermissionId(Long permissionId);

    /**
     * 根据客户端ID列表批量查询权限
     *
     * @param clientIds 客户端ID列表
     * @return 客户端权限关联列表
     */
    List<ClientPermission> findByClientIds(List<String> clientIds);
}
