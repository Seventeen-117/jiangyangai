package com.signature.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.signature.entity.ApiPermission;

import java.util.List;

/**
 * API权限Service接口
 *
 * @author signature-service
 * @since 2025-01-01
 */
public interface ApiPermissionService extends IService<ApiPermission> {

    /**
     * 根据权限编码查询权限
     *
     * @param permissionCode 权限编码
     * @return API权限
     */
    ApiPermission findByPermissionCode(String permissionCode);

    /**
     * 根据权限编码查询启用的权限
     *
     * @param permissionCode 权限编码
     * @return API权限
     */
    ApiPermission findEnabledByPermissionCode(String permissionCode);

    /**
     * 根据权限类型查询权限列表
     *
     * @param permissionType 权限类型
     * @return API权限列表
     */
    List<ApiPermission> findByPermissionType(String permissionType);

    /**
     * 根据权限类型查询启用的权限列表
     *
     * @param permissionType 权限类型
     * @return API权限列表
     */
    List<ApiPermission> findEnabledByPermissionType(String permissionType);

    /**
     * 查询所有启用的权限
     *
     * @return API权限列表
     */
    List<ApiPermission> findAllEnabled();

    /**
     * 根据状态查询权限
     *
     * @param status 状态
     * @return API权限列表
     */
    List<ApiPermission> findByStatus(Integer status);

    /**
     * 根据父权限ID查询子权限
     *
     * @param parentId 父权限ID
     * @return API权限列表
     */
    List<ApiPermission> findByParentId(Long parentId);

    /**
     * 根据父权限ID查询启用的子权限
     *
     * @param parentId 父权限ID
     * @return API权限列表
     */
    List<ApiPermission> findEnabledByParentId(Long parentId);

    /**
     * 创建API权限
     *
     * @param permission API权限
     * @return 创建后的API权限
     */
    ApiPermission createPermission(ApiPermission permission);

    /**
     * 更新API权限
     *
     * @param permission API权限
     * @return 更新后的API权限
     */
    ApiPermission updatePermission(ApiPermission permission);

    /**
     * 删除API权限
     *
     * @param id 权限ID
     * @return 是否删除成功
     */
    boolean deletePermission(Long id);

    /**
     * 启用API权限
     *
     * @param id 权限ID
     * @return 是否启用成功
     */
    boolean enablePermission(Long id);

    /**
     * 禁用API权限
     *
     * @param id 权限ID
     * @return 是否禁用成功
     */
    boolean disablePermission(Long id);

    /**
     * 根据权限编码批量查询
     *
     * @param permissionCodes 权限编码列表
     * @return API权限列表
     */
    List<ApiPermission> findByPermissionCodes(List<String> permissionCodes);

    /**
     * 分页查询API权限
     *
     * @param page 页码
     * @param size 页大小
     * @param permissionType 权限类型（可选）
     * @param status 状态（可选）
     * @return 分页结果
     */
    Page<ApiPermission> findPage(Integer page, Integer size, String permissionType, Integer status);

    /**
     * 检查权限编码是否存在
     *
     * @param permissionCode 权限编码
     * @return 是否存在
     */
    boolean existsByPermissionCode(String permissionCode);

    /**
     * 检查权限是否启用
     *
     * @param permissionCode 权限编码
     * @return 是否启用
     */
    boolean isPermissionEnabled(String permissionCode);

    /**
     * 获取权限的级别
     *
     * @param permissionCode 权限编码
     * @return 权限级别，如果不存在返回null
     */
    Integer getPermissionLevel(String permissionCode);

    /**
     * 根据API路径和HTTP方法查询权限
     *
     * @param apiPath API路径
     * @param httpMethod HTTP方法
     * @return API权限列表
     */
    List<ApiPermission> findByApiPathAndMethod(String apiPath, String httpMethod);

    /**
     * 检查是否为通配符权限
     *
     * @param permissionCode 权限编码
     * @return 是否为通配符权限
     */
    boolean isWildcardPermission(String permissionCode);

    /**
     * 获取权限的描述信息
     *
     * @param permissionCode 权限编码
     * @return 权限描述
     */
    String getPermissionDescription(String permissionCode);
}
