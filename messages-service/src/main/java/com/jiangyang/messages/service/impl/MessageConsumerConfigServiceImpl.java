package com.jiangyang.messages.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiangyang.base.datasource.annotation.DataSource;
import com.jiangyang.messages.entity.MessageConsumerConfig;
import com.jiangyang.messages.mapper.MessageConsumerConfigMapper;
import com.jiangyang.messages.service.MessageConsumerConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息消费配置服务实现类
 */
@Slf4j
@Service
@DataSource("master")
public class MessageConsumerConfigServiceImpl extends ServiceImpl<MessageConsumerConfigMapper, MessageConsumerConfig> 
        implements MessageConsumerConfigService {

    @Override
    public List<MessageConsumerConfig> getAllConfigs() {
        return list();
    }

    @Override
    public List<MessageConsumerConfig> getConfigsByService(String serviceName) {
        QueryWrapper<MessageConsumerConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("service_name", serviceName);
        return list(queryWrapper);
    }

    @Override
    public List<MessageConsumerConfig> getEnabledConfigs() {
        QueryWrapper<MessageConsumerConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("enabled", true);
        return list(queryWrapper);
    }

    @Override
    public MessageConsumerConfig getConfigById(Long id) {
        return getById(id);
    }

    @Override
    public boolean saveConfig(MessageConsumerConfig config) {
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        return save(config);
    }

    @Override
    public boolean updateConfig(MessageConsumerConfig config) {
        config.setUpdateTime(LocalDateTime.now());
        return updateById(config);
    }

    @Override
    public boolean deleteConfig(Long id) {
        return removeById(id);
    }

    @Override
    public boolean enableConfig(Long id) {
        MessageConsumerConfig config = getById(id);
        if (config != null) {
            config.setEnabled(true);
            config.setUpdateTime(LocalDateTime.now());
            return updateById(config);
        }
        return false;
    }

    @Override
    public boolean disableConfig(Long id) {
        MessageConsumerConfig config = getById(id);
        if (config != null) {
            config.setEnabled(false);
            config.setUpdateTime(LocalDateTime.now());
            return updateById(config);
        }
        return false;
    }

    @Override
    public boolean hasConfigChanged() {
        // 这里可以实现配置变更检测逻辑
        // 比如检查配置文件的修改时间，或者数据库中的配置版本号
        // 暂时返回false，表示没有变更
        return false;
    }

    @Override
    public List<MessageConsumerConfig> getConfigsByMessageQueueType(String messageQueueType) {
        QueryWrapper<MessageConsumerConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("message_queue_type", messageQueueType);
        return list(queryWrapper);
    }

    @Override
    public List<MessageConsumerConfig> getConfigsByConsumeMode(String consumeMode) {
        QueryWrapper<MessageConsumerConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("consume_mode", consumeMode);
        return list(queryWrapper);
    }

    @Override
    public List<MessageConsumerConfig> getConfigsByConsumeType(String consumeType) {
        QueryWrapper<MessageConsumerConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("consume_type", consumeType);
        return list(queryWrapper);
    }

    @Override
    public List<MessageConsumerConfig> getConfigsByConsumeOrder(String consumeOrder) {
        QueryWrapper<MessageConsumerConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("consume_order", consumeOrder);
        return list(queryWrapper);
    }
}
