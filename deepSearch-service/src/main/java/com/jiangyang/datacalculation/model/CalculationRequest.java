package com.jiangyang.datacalculation.model;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * 数据计算请求模型
 * 
 * @author jiangyang
 * @since 1.0.0
 */
@Data
public class CalculationRequest {

    /**
     * 请求ID
     */
    @NotBlank(message = "请求ID不能为空")
    private String requestId;

    /**
     * 业务类型
     */
    @NotBlank(message = "业务类型不能为空")
    private String businessType;

    /**
     * 计算参数
     */
    @NotNull(message = "计算参数不能为空")
    private Map<String, Object> parameters;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 请求来源
     */
    private String source;

    /**
     * 扩展参数
     */
    private Map<String, Object> extraParams;
}
