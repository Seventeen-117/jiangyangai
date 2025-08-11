package com.jiangyang.messages.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiangyang.messages.audit.entity.MessageLifecycleLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息生命周期日志Mapper接口
 */
@Mapper
public interface MessageLifecycleLogMapper extends BaseMapper<MessageLifecycleLog> {

    /**
     * 根据消息ID查询生命周期日志
     */
    List<MessageLifecycleLog> selectByMessageId(@Param("messageId") String messageId);

    /**
     * 根据业务消息ID查询生命周期日志
     */
    List<MessageLifecycleLog> selectByBusinessMessageId(@Param("businessMessageId") String businessMessageId);

    /**
     * 根据消息类型和主题查询生命周期日志
     */
    List<MessageLifecycleLog> selectByMessageTypeAndTopic(@Param("messageType") String messageType, 
                                                         @Param("topic") String topic);

    /**
     * 根据生命周期阶段查询日志
     */
    List<MessageLifecycleLog> selectByLifecycleStage(@Param("lifecycleStage") String lifecycleStage);

    /**
     * 根据时间范围查询生命周期日志
     */
    List<MessageLifecycleLog> selectByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                               @Param("endTime") LocalDateTime endTime);

    /**
     * 根据消费者组查询生命周期日志
     */
    List<MessageLifecycleLog> selectByConsumerGroup(@Param("consumerGroup") String consumerGroup);

    /**
     * 查询死信消息
     */
    List<MessageLifecycleLog> selectDeadLetterMessages(@Param("startTime") LocalDateTime startTime, 
                                                      @Param("endTime") LocalDateTime endTime);

    /**
     * 查询超时消息
     */
    List<MessageLifecycleLog> selectTimeoutMessages(@Param("startTime") LocalDateTime startTime, 
                                                   @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定时间范围内的消息数量
     */
    Long countMessagesByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                 @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定时间范围内的死信消息数量
     */
    Long countDeadLetterMessagesByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定时间范围内的超时消息数量
     */
    Long countTimeoutMessagesByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 查询消息处理性能统计
     */
    List<MessageLifecycleLog> selectPerformanceStats(@Param("startTime") LocalDateTime startTime, 
                                                    @Param("endTime") LocalDateTime endTime);
}
