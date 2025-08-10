package com.signature.service;

import com.signature.entity.ApiPermission;
import com.signature.entity.ApiKeyPermission;
import com.signature.entity.ClientPermission;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 权限管理服务接口
 */
public interface PermissionService {

    /**
     * 为API密钥授予权限
     *
     * @param apiKeyId API密钥ID
     * @param permissionCode 权限代码
     * @param grantedBy 授权人
     * @param expiresAt 过期时间
     * @return 是否成功
     */
    boolean grantApiKeyPermission(Long apiKeyId, String permissionCode, String grantedBy, LocalDateTime expiresAt);

    /**
     * 为客户端授予权限
     *
     * @param clientId 客户端ID
     * @param permissionCode 权限代码
     * @param grantedBy 授权人
     * @param expiresAt 过期时间
     * @return 是否成功
     */
    boolean grantClientPermission(String clientId, String permissionCode, String grantedBy, LocalDateTime expiresAt);

    /**
     * 撤销API密钥权限
     *
     * @param apiKeyId API密钥ID
     * @param permissionCode 权限代码
     * @return 是否成功
     */
    boolean revokeApiKeyPermission(Long apiKeyId, String permissionCode);

    /**
     * 撤销客户端权限
     *
     * @param clientId 客户端ID
     * @param permissionCode 权限代码
     * @return 是否成功
     */
    boolean revokeClientPermission(String clientId, String permissionCode);

    /**
     * 获取API密钥的所有权限
     *
     * @param apiKeyId API密钥ID
     * @return 权限列表
     */
    List<ApiPermission> getApiKeyPermissions(Long apiKeyId);

    /**
     * 获取客户端的所有权限
     *
     * @param clientId 客户端ID
     * @return 权限列表
     */
    List<ApiPermission> getClientPermissions(String clientId);

    /**
     * 获取所有权限
     *
     * @return 权限列表
     */
    List<ApiPermission> getAllPermissions();

    /**
     * 创建新权限
     *
     * @param permission 权限信息
     * @return 是否成功
     */
    boolean createPermission(ApiPermission permission);

    /**
     * 更新权限
     *
     * @param permission 权限信息
     * @return 是否成功
     */
    boolean updatePermission(ApiPermission permission);

    /**
     * 删除权限
     *
     * @param permissionCode 权限代码
     * @return 是否成功
     */
    boolean deletePermission(String permissionCode);

    /**
     * 批量授予API密钥权限
     *
     * @param apiKeyId API密钥ID
     * @param permissionCodes 权限代码列表
     * @param grantedBy 授权人
     * @param expiresAt 过期时间
     * @return 成功数量
     */
    int batchGrantApiKeyPermissions(Long apiKeyId, List<String> permissionCodes, String grantedBy, LocalDateTime expiresAt);

    /**
     * 批量授予客户端权限
     *
     * @param clientId 客户端ID
     * @param permissionCodes 权限代码列表
     * @param grantedBy 授权人
     * @param expiresAt 过期时间
     * @return 成功数量
     */
    int batchGrantClientPermissions(String clientId, List<String> permissionCodes, String grantedBy, LocalDateTime expiresAt);
}
