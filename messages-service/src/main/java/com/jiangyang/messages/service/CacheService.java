package com.jiangyang.messages.service;

import com.jiangyang.base.datasource.annotation.DataSource;

/**
 * 缓存服务接口
 * 
 * @author jiangyang
 * @since 1.0.0
 */
@DataSource("master")
public interface CacheService {

    /**
     * 更新业务状态缓存
     * 
     * @param messageId 消息ID
     * @param status 业务状态
     * @return 是否更新成功
     */
    boolean updateBusinessStatus(String messageId, String status);

    /**
     * 获取业务状态缓存
     * 
     * @param messageId 消息ID
     * @return 业务状态，如果不存在返回null
     */
    String getBusinessStatus(String messageId);

    /**
     * 删除业务状态缓存
     * 
     * @param messageId 消息ID
     * @return 是否删除成功
     */
    boolean deleteBusinessStatus(String messageId);

    /**
     * 设置缓存过期时间
     * 
     * @param messageId 消息ID
     * @param expireSeconds 过期时间（秒）
     * @return 是否设置成功
     */
    boolean expireBusinessStatus(String messageId, long expireSeconds);

    /**
     * 检查业务状态缓存是否存在
     * 
     * @param messageId 消息ID
     * @return 是否存在
     */
    boolean existsBusinessStatus(String messageId);
}
