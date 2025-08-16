package com.jiangyang.datacalculation.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 数据计算响应模型
 * 
 * @author jiangyang
 * @since 1.0.0
 */
@Data
public class CalculationResponse {

    /**
     * 响应代码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private CalculationData data;

    /**
     * 计算数据
     */
    @Data
    public static class CalculationData {
        
        /**
         * 请求ID
         */
        private String requestId;
        
        /**
         * 业务类型
         */
        private String businessType;
        
        /**
         * 计算状态
         */
        private String calculationStatus;
        
        /**
         * 计算开始时间
         */
        private Long calculationStartTime;
        
        /**
         * 计算完成时间
         */
        private Long calculationEndTime;
        
        /**
         * 计算耗时（毫秒）
         */
        private Long calculationDuration;
        
        /**
         * 计算结果
         */
        private Map<String, Object> calculationResult;
        
        /**
         * 计算过程日志
         */
        private List<CalculationLog> calculationLogs;
        
        /**
         * 错误信息
         */
        private List<String> errorMessages;
        
        /**
         * 警告信息
         */
        private List<String> warningMessages;
        
        /**
         * 元数据信息
         */
        private Map<String, Object> metadata;
    }

    /**
     * 计算日志
     */
    @Data
    public static class CalculationLog {
        
        /**
         * 日志时间戳
         */
        private Long timestamp;
        
        /**
         * 日志级别
         */
        private String level;
        
        /**
         * 日志消息
         */
        private String message;
        
        /**
         * 步骤名称
         */
        private String stepName;
        
        /**
         * 输入数据
         */
        private Object inputData;
        
        /**
         * 输出数据
         */
        private Object outputData;
        
        /**
         * 执行时间（毫秒）
         */
        private Long executionTime;
    }
}
