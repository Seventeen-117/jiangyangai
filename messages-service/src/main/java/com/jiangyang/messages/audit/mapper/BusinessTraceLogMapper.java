package com.jiangyang.messages.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiangyang.messages.audit.entity.BusinessTraceLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务轨迹日志Mapper接口
 */
@Mapper
public interface BusinessTraceLogMapper extends BaseMapper<BusinessTraceLog> {

    /**
     * 根据全局事务ID查询业务轨迹日志
     */
    List<BusinessTraceLog> selectByGlobalTransactionId(@Param("globalTransactionId") String globalTransactionId);

    /**
     * 根据业务事务ID查询业务轨迹日志
     */
    List<BusinessTraceLog> selectByBusinessTransactionId(@Param("businessTransactionId") String businessTransactionId);

    /**
     * 根据调用链ID查询业务轨迹日志
     */
    List<BusinessTraceLog> selectByTraceId(@Param("traceId") String traceId);

    /**
     * 根据Span ID查询业务轨迹日志
     */
    List<BusinessTraceLog> selectBySpanId(@Param("spanId") String spanId);

    /**
     * 根据父Span ID查询业务轨迹日志
     */
    List<BusinessTraceLog> selectByParentSpanId(@Param("parentSpanId") String parentSpanId);

    /**
     * 根据服务名称查询业务轨迹日志
     */
    List<BusinessTraceLog> selectByServiceName(@Param("serviceName") String serviceName);

    /**
     * 根据操作类型查询业务轨迹日志
     */
    List<BusinessTraceLog> selectByOperationType(@Param("operationType") String operationType);

    /**
     * 根据调用方向查询业务轨迹日志
     */
    List<BusinessTraceLog> selectByCallDirection(@Param("callDirection") String callDirection);

    /**
     * 根据时间范围查询业务轨迹日志
     */
    List<BusinessTraceLog> selectByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                            @Param("endTime") LocalDateTime endTime);

    /**
     * 根据调用状态查询业务轨迹日志
     */
    List<BusinessTraceLog> selectByCallStatus(@Param("callStatus") String callStatus);

    /**
     * 查询失败的调用
     */
    List<BusinessTraceLog> selectFailedCalls(@Param("startTime") LocalDateTime startTime, 
                                            @Param("endTime") LocalDateTime endTime);

    /**
     * 查询超时的调用
     */
    List<BusinessTraceLog> selectTimeoutCalls(@Param("startTime") LocalDateTime startTime, 
                                             @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定时间范围内的调用数量
     */
    Long countCallsByTimeRange(@Param("startTime") LocalDateTime startTime, 
                              @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定时间范围内的失败调用数量
     */
    Long countFailedCallsByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                    @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定时间范围内的超时调用数量
     */
    Long countTimeoutCallsByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 查询调用性能统计
     */
    List<BusinessTraceLog> selectCallPerformanceStats(@Param("startTime") LocalDateTime startTime, 
                                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 查询完整的调用链
     */
    List<BusinessTraceLog> selectCompleteCallChain(@Param("traceId") String traceId);
}
