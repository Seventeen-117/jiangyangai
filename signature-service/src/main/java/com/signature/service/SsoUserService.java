package com.signature.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.signature.entity.SsoUser;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SSO用户服务接口
 *
 * @author signature-service
 * @since 2025-01-01
 */
public interface SsoUserService extends IService<SsoUser> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return SSO用户
     */
    SsoUser findByUsername(String username);

    /**
     * 根据用户ID查询用户
     *
     * @param userId 用户ID
     * @return SSO用户
     */
    SsoUser findByUserId(String userId);

    /**
     * 根据邮箱查询用户
     *
     * @param email 邮箱
     * @return SSO用户
     */
    SsoUser findByEmail(String email);

    /**
     * 验证用户凭据
     *
     * @param username 用户名
     * @param password 密码
     * @return 是否有效
     */
    boolean validateCredentials(String username, String password);

    /**
     * 更新最后登录时间
     *
     * @param userId      用户ID
     * @param lastLoginAt 最后登录时间
     * @param lastLoginIp 最后登录IP
     * @return 是否更新成功
     */
    boolean updateLastLogin(String userId, LocalDateTime lastLoginAt, String lastLoginIp);

    /**
     * 创建SSO用户
     *
     * @param ssoUser SSO用户
     * @return 创建的SSO用户
     */
    SsoUser createSsoUser(SsoUser ssoUser);

    /**
     * 更新SSO用户
     *
     * @param ssoUser SSO用户
     * @return 更新后的SSO用户
     */
    SsoUser updateSsoUser(SsoUser ssoUser);

    /**
     * 删除SSO用户
     *
     * @param id 用户ID
     * @return 是否删除成功
     */
    boolean deleteSsoUser(Long id);

    /**
     * 启用/禁用用户
     *
     * @param id      用户ID
     * @param enabled 是否启用
     * @return 是否更新成功
     */
    boolean updateUserStatus(Long id, Boolean enabled);

    /**
     * 锁定/解锁用户
     *
     * @param id     用户ID
     * @param locked 是否锁定
     * @return 是否更新成功
     */
    boolean updateUserLockStatus(Long id, Boolean locked);

    /**
     * 根据部门查询用户
     *
     * @param department 部门
     * @return 用户列表
     */
    List<SsoUser> findByDepartment(String department);

    /**
     * 根据角色查询用户
     *
     * @param role 角色
     * @return 用户列表
     */
    List<SsoUser> findByRole(String role);

    /**
     * 查询所有启用的用户
     *
     * @return 用户列表
     */
    List<SsoUser> findAllEnabled();

    /**
     * 查询所有未锁定的用户
     *
     * @return 用户列表
     */
    List<SsoUser> findAllUnlocked();

    /**
     * 批量更新用户状态
     *
     * @param ids     用户ID列表
     * @param enabled 是否启用
     * @return 更新数量
     */
    int batchUpdateUserStatus(List<Long> ids, Boolean enabled);

    /**
     * 检查用户名是否已存在
     *
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否已存在
     *
     * @param email 邮箱
     * @return 是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 修改用户密码
     *
     * @param userId      用户ID
     * @param newPassword 新密码
     * @return 是否修改成功
     */
    boolean changePassword(String userId, String newPassword);
}
