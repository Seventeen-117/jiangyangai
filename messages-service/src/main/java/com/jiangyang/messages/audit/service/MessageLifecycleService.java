package com.jiangyang.messages.audit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiangyang.base.datasource.annotation.DataSource;
import com.jiangyang.messages.audit.entity.MessageLifecycleLog;

/**
 * 消息生命周期服务接口
 * 提供消息生命周期日志的增删改查服务
 */
@DataSource("master")
public interface MessageLifecycleService extends IService<MessageLifecycleLog> {

    /**
     * 根据消息ID查询生命周期日志
     * @param messageId 消息ID
     * @return 生命周期日志
     */
    MessageLifecycleLog getByMessageId(String messageId);

    /**
     * 根据业务消息ID查询生命周期日志
     * @param businessMessageId 业务消息ID
     * @return 生命周期日志列表
     */
    java.util.List<MessageLifecycleLog> getByBusinessMessageId(String businessMessageId);

    /**
     * 根据生命周期阶段查询日志
     * @param lifecycleStage 生命周期阶段
     * @return 生命周期日志列表
     */
    java.util.List<MessageLifecycleLog> getByLifecycleStage(String lifecycleStage);

    /**
     * 根据状态查询生命周期日志
     * @param stageStatus 状态
     * @return 生命周期日志列表
     */
    java.util.List<MessageLifecycleLog> getByStageStatus(String stageStatus);

    /**
     * 更新生命周期日志状态
     * @param messageId 消息ID
     * @param stageStatus 新状态
     * @param errorMessage 错误信息
     * @return 是否更新成功
     */
    boolean updateStageStatus(String messageId, String stageStatus, String errorMessage);

    /**
     * 批量保存生命周期日志
     * @param lifecycleLogs 生命周期日志列表
     * @return 是否保存成功
     */
    boolean saveBatch(java.util.List<MessageLifecycleLog> lifecycleLogs);
}
