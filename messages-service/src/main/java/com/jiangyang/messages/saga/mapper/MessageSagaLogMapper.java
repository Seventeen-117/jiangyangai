package com.jiangyang.messages.saga.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiangyang.messages.saga.entity.MessageSagaLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息Saga日志Mapper接口
 */
@Mapper
public interface MessageSagaLogMapper extends BaseMapper<MessageSagaLog> {
}
