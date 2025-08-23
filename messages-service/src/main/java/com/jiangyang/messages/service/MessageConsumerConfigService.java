package com.jiangyang.messages.service;

import com.jiangyang.base.datasource.annotation.DataSource;
import com.jiangyang.messages.entity.MessageConsumerConfig;

import java.util.List;

/**
 * 消息消费配置服务接口
 * 管理消费配置的CRUD操作
 */
@DataSource("master")
public interface MessageConsumerConfigService {

    /**
     * 获取所有消费配置
     * 
     * @return 消费配置列表
     */
    List<MessageConsumerConfig> getAllConfigs();

    /**
     * 根据服务名称获取消费配置
     * 
     * @param serviceName 服务名称
     * @return 消费配置列表
     */
    List<MessageConsumerConfig> getConfigsByService(String serviceName);

    /**
     * 获取所有启用的消费配置
     * 
     * @return 启用的消费配置列表
     */
    List<MessageConsumerConfig> getEnabledConfigs();

    /**
     * 根据ID获取消费配置
     * 
     * @param id 配置ID
     * @return 消费配置
     */
    MessageConsumerConfig getConfigById(Long id);

    /**
     * 保存消费配置
     * 
     * @param config 消费配置
     * @return 是否保存成功
     */
    boolean saveConfig(MessageConsumerConfig config);

    /**
     * 更新消费配置
     * 
     * @param config 消费配置
     * @return 是否更新成功
     */
    boolean updateConfig(MessageConsumerConfig config);

    /**
     * 删除消费配置
     * 
     * @param id 配置ID
     * @return 是否删除成功
     */
    boolean deleteConfig(Long id);

    /**
     * 启用消费配置
     * 
     * @param id 配置ID
     * @return 是否启用成功
     */
    boolean enableConfig(Long id);

    /**
     * 禁用消费配置
     * 
     * @param id 配置ID
     * @return 是否禁用成功
     */
    boolean disableConfig(Long id);

    /**
     * 检查配置是否有变更
     * 
     * @return 是否有变更
     */
    boolean hasConfigChanged();

    /**
     * 根据消息中间件类型获取消费配置
     * 
     * @param messageQueueType 消息中间件类型
     * @return 消费配置列表
     */
    List<MessageConsumerConfig> getConfigsByMessageQueueType(String messageQueueType);

    /**
     * 根据消费模式获取消费配置
     * 
     * @param consumeMode 消费模式
     * @return 消费配置列表
     */
    List<MessageConsumerConfig> getConfigsByConsumeMode(String consumeMode);

    /**
     * 根据消费类型获取消费配置
     * 
     * @param consumeType 消费类型
     * @return 消费配置列表
     */
    List<MessageConsumerConfig> getConfigsByConsumeType(String consumeType);

    /**
     * 根据消费顺序性获取消费配置
     * 
     * @param consumeOrder 消费顺序性
     * @return 消费配置列表
     */
    List<MessageConsumerConfig> getConfigsByConsumeOrder(String consumeOrder);
}
