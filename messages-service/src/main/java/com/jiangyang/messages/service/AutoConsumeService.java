package com.jiangyang.messages.service;

import com.jiangyang.base.datasource.annotation.DataSource;
import com.jiangyang.messages.entity.MessageConsumerConfig;

import java.util.List;

/**
 * 自动消费服务接口
 * 根据配置自动消费消息
 */
@DataSource("master")
public interface AutoConsumeService {

    /**
     * 启动自动消费
     * 根据配置自动启动消息消费
     */
    void startAutoConsume();

    /**
     * 停止自动消费
     */
    void stopAutoConsume();

    /**
     * 重新加载消费配置
     */
    void reloadConsumeConfig();

    /**
     * 根据服务名称启动消费
     * 
     * @param serviceName 服务名称
     */
    void startConsumeByService(String serviceName);

    /**
     * 根据服务名称停止消费
     * 
     * @param serviceName 服务名称
     */
    void stopConsumeByService(String serviceName);

    /**
     * 获取所有消费配置
     * 
     * @return 消费配置列表
     */
    List<MessageConsumerConfig> getAllConsumeConfigs();

    /**
     * 获取指定服务的消费配置
     * 
     * @param serviceName 服务名称
     * @return 消费配置列表
     */
    List<MessageConsumerConfig> getConsumeConfigsByService(String serviceName);

    /**
     * 检查消费状态
     * 
     * @param serviceName 服务名称
     * @return 是否正在消费
     */
    boolean isConsuming(String serviceName);

    /**
     * 获取消费统计信息
     * 
     * @param serviceName 服务名称
     * @return 统计信息
     */
    Object getConsumeStatistics(String serviceName);
}
