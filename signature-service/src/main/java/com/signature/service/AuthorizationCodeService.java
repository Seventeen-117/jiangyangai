package com.signature.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.signature.entity.AuthorizationCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 授权码服务接口
 *
 * @author signature-service
 * @since 2025-01-01
 */
public interface AuthorizationCodeService extends IService<AuthorizationCode> {

    /**
     * 根据授权码查询有效的授权码
     *
     * @param code 授权码
     * @return 授权码信息
     */
    AuthorizationCode findValidByCode(String code);

    /**
     * 根据授权码查询（包括已过期的）
     *
     * @param code 授权码
     * @return 授权码信息
     */
    AuthorizationCode findByCode(String code);

    /**
     * 标记授权码为已使用
     *
     * @param code 授权码
     * @return 是否更新成功
     */
    boolean markAsUsed(String code);

    /**
     * 清理过期的授权码
     *
     * @return 清理数量
     */
    int cleanupExpired();

    /**
     * 创建授权码
     *
     * @param authorizationCode 授权码信息
     * @return 创建的授权码
     */
    AuthorizationCode createAuthorizationCode(AuthorizationCode authorizationCode);

    /**
     * 生成新的授权码
     *
     * @param clientId    客户端ID
     * @param userId      用户ID
     * @param redirectUri 重定向URI
     * @param scope       作用域
     * @param state       状态参数
     * @param expiresIn   过期时间（秒）
     * @return 生成的授权码
     */
    AuthorizationCode generateAuthorizationCode(String clientId, String userId, 
                                              String redirectUri, String scope, 
                                              String state, int expiresIn);

    /**
     * 验证授权码是否有效
     *
     * @param code     授权码
     * @param clientId 客户端ID
     * @return 是否有效
     */
    boolean validateAuthorizationCode(String code, String clientId);

    /**
     * 根据客户端ID查询授权码
     *
     * @param clientId 客户端ID
     * @return 授权码列表
     */
    List<AuthorizationCode> findByClientId(String clientId);

    /**
     * 根据用户ID查询授权码
     *
     * @param userId 用户ID
     * @return 授权码列表
     */
    List<AuthorizationCode> findByUserId(String userId);

    /**
     * 查询未使用的授权码
     *
     * @return 授权码列表
     */
    List<AuthorizationCode> findUnused();

    /**
     * 查询已过期的授权码
     *
     * @return 授权码列表
     */
    List<AuthorizationCode> findExpired();

    /**
     * 批量清理指定客户端的授权码
     *
     * @param clientId 客户端ID
     * @return 清理数量
     */
    int cleanupByClientId(String clientId);

    /**
     * 获取授权码统计信息
     *
     * @return 统计信息
     */
    Object getAuthorizationCodeStatistics();
}
