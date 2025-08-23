package com.jiangyang.messages.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiangyang.messages.entity.MessageConsumerConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息消费配置Mapper接口
 */
@Mapper
public interface MessageConsumerConfigMapper extends BaseMapper<MessageConsumerConfig> {
}
