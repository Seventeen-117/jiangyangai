package com.jiangyang.datacalculation.model;

import lombok.Data;
import java.util.List;

/**
 * AI代理响应模型
 * 
 * @author jiangyang
 * @since 1.0.0
 */
@Data
public class AiAgentResponse {

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
    private AiAgentData data;

    /**
     * AI代理数据
     */
    @Data
    public static class AiAgentData {
        
        /**
         * 逻辑流程图（Base64编码的图片或SVG内容）
         */
        private String logicFlowChart;
        
        /**
         * 逻辑思路文字描述
         */
        private String logicDescription;
        
        /**
         * 计算步骤列表
         */
        private List<CalculationStep> calculationSteps;
        
        /**
         * 输入参数说明
         */
        private List<ParameterDescription> inputParameters;
        
        /**
         * 输出结果说明
         */
        private List<ParameterDescription> outputParameters;
        
        /**
         * 计算复杂度
         */
        private String complexity;
        
        /**
         * 预估执行时间（毫秒）
         */
        private Long estimatedExecutionTime;
    }

    /**
     * 计算步骤
     */
    @Data
    public static class CalculationStep {
        
        /**
         * 步骤序号
         */
        private Integer stepNumber;
        
        /**
         * 步骤名称
         */
        private String stepName;
        
        /**
         * 步骤描述
         */
        private String stepDescription;
        
        /**
         * 步骤类型
         */
        private String stepType;
        
        /**
         * 依赖步骤
         */
        private List<Integer> dependencies;
        
        /**
         * 计算公式
         */
        private String formula;
    }

    /**
     * 参数描述
     */
    @Data
    public static class ParameterDescription {
        
        /**
         * 参数名称
         */
        private String parameterName;
        
        /**
         * 参数类型
         */
        private String parameterType;
        
        /**
         * 参数描述
         */
        private String parameterDescription;
        
        /**
         * 是否必填
         */
        private Boolean required;
        
        /**
         * 默认值
         */
        private Object defaultValue;
        
        /**
         * 取值范围
         */
        private String valueRange;
    }
}
