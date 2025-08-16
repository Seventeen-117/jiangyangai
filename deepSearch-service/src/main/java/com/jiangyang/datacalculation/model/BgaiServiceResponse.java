package com.jiangyang.datacalculation.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * BGAI服务响应模型
 * 
 * @author jiangyang
 * @since 1.0.0
 */
@Data
public class BgaiServiceResponse {

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
    private BgaiData data;

    /**
     * BGAI数据
     */
    @Data
    public static class BgaiData {
        
        /**
         * 计算基础数据
         */
        private Map<String, Object> calculationBasis;
        
        /**
         * 计算公式
         */
        private List<CalculationFormula> formulas;
        
        /**
         * 数据源信息
         */
        private List<DataSourceInfo> dataSources;
        
        /**
         * 计算规则
         */
        private List<CalculationRule> calculationRules;
        
        /**
         * 验证规则
         */
        private List<ValidationRule> validationRules;
        
        /**
         * 输出格式
         */
        private OutputFormat outputFormat;
        
        /**
         * 执行建议
         */
        private ExecutionAdvice executionAdvice;
    }

    /**
     * 计算公式
     */
    @Data
    public static class CalculationFormula {
        
        /**
         * 公式ID
         */
        private String formulaId;
        
        /**
         * 公式名称
         */
        private String formulaName;
        
        /**
         * 公式表达式
         */
        private String expression;
        
        /**
         * 公式描述
         */
        private String description;
        
        /**
         * 适用条件
         */
        private String conditions;
        
        /**
         * 优先级
         */
        private Integer priority;
    }

    /**
     * 数据源信息
     */
    @Data
    public static class DataSourceInfo {
        
        /**
         * 数据源ID
         */
        private String dataSourceId;
        
        /**
         * 数据源名称
         */
        private String dataSourceName;
        
        /**
         * 数据源类型
         */
        private String dataSourceType;
        
        /**
         * 数据源地址
         */
        private String dataSourceUrl;
        
        /**
         * 数据格式
         */
        private String dataFormat;
        
        /**
         * 更新频率
         */
        private String updateFrequency;
        
        /**
         * 数据质量
         */
        private String dataQuality;
    }

    /**
     * 计算规则
     */
    @Data
    public static class CalculationRule {
        
        /**
         * 规则ID
         */
        private String ruleId;
        
        /**
         * 规则名称
         */
        private String ruleName;
        
        /**
         * 规则类型
         */
        private String ruleType;
        
        /**
         * 规则描述
         */
        private String description;
        
        /**
         * 规则参数
         */
        private Map<String, Object> parameters;
        
        /**
         * 执行顺序
         */
        private Integer executionOrder;
    }

    /**
     * 验证规则
     */
    @Data
    public static class ValidationRule {
        
        /**
         * 验证规则ID
         */
        private String validationRuleId;
        
        /**
         * 验证规则名称
         */
        private String validationRuleName;
        
        /**
         * 验证类型
         */
        private String validationType;
        
        /**
         * 验证表达式
         */
        private String validationExpression;
        
        /**
         * 错误消息
         */
        private String errorMessage;
        
        /**
         * 严重程度
         */
        private String severity;
    }

    /**
     * 输出格式
     */
    @Data
    public static class OutputFormat {
        
        /**
         * 输出类型
         */
        private String outputType;
        
        /**
         * 输出结构
         */
        private Map<String, Object> outputStructure;
        
        /**
         * 数据精度
         */
        private Integer precision;
        
        /**
         * 单位
         */
        private String unit;
        
        /**
         * 格式化规则
         */
        private String formatRule;
    }

    /**
     * 执行建议
     */
    @Data
    public static class ExecutionAdvice {
        
        /**
         * 建议的执行方式
         */
        private String executionMode;
        
        /**
         * 建议的并发数
         */
        private Integer recommendedConcurrency;
        
        /**
         * 建议的超时时间
         */
        private Long recommendedTimeout;
        
        /**
         * 性能优化建议
         */
        private List<String> performanceOptimizations;
        
        /**
         * 风险提示
         */
        private List<String> riskWarnings;
    }
}
