package com.jiangyang.messages.saga.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiangyang.base.datasource.annotation.DataSource;
import com.jiangyang.messages.saga.entity.MessageSagaLog;

/**
 * 消息Saga日志服务接口
 * 提供Saga事务日志的增删改查服务
 */
@DataSource("slave")
public interface MessageSagaLogService extends IService<MessageSagaLog> {

    /**
     * 根据业务ID查询Saga日志
     * @param businessId 业务ID
     * @return Saga日志
     */
    MessageSagaLog getByBusinessId(String businessId);

    /**
     * 根据全局事务ID查询Saga日志
     * @param globalTransactionId 全局事务ID
     * @return Saga日志列表
     */
    java.util.List<MessageSagaLog> getByGlobalTransactionId(String globalTransactionId);

    /**
     * 根据操作类型查询Saga日志
     * @param operation 操作类型
     * @return Saga日志列表
     */
    java.util.List<MessageSagaLog> getByOperation(String operation);

    /**
     * 根据状态查询Saga日志
     * @param status 状态
     * @return Saga日志列表
     */
    java.util.List<MessageSagaLog> getByStatus(String status);

    /**
     * 更新Saga日志状态
     * @param businessId 业务ID
     * @param status 新状态
     * @param errorMessage 错误信息
     * @return 是否更新成功
     */
    boolean updateStatus(String businessId, String status, String errorMessage);

    /**
     * 批量保存Saga日志
     * @param sagaLogs Saga日志列表
     * @return 是否保存成功
     */
    boolean saveBatch(java.util.List<MessageSagaLog> sagaLogs);
}
