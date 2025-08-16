package com.jiangyang.datacalculation.model;

import lombok.Data;
import java.util.List;

/**
 * BGAI服务请求模型
 * 
 * @author jiangyang
 * @since 1.0.0
 */
@Data
public class BgaiServiceRequest {

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 逻辑流程图
     */
    private String logicFlowChart;

    /**
     * 逻辑思路描述
     */
    private String logicDescription;

    /**
     * 计算步骤
     */
    private List<AiAgentResponse.CalculationStep> calculationSteps;

    /**
     * 输入参数
     */
    private List<AiAgentResponse.ParameterDescription> inputParameters;

    /**
     * 输出参数
     */
    private List<AiAgentResponse.ParameterDescription> outputParameters;

    /**
     * 计算复杂度
     */
    private String complexity;

    /**
     * 预估执行时间
     */
    private Long estimatedExecutionTime;

    /**
     * 原始请求参数
     */
    private Object originalParameters;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 请求来源
     */
    private String source;
}
