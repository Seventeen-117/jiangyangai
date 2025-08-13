package com.jiangyang.datacalculation.model.algorithm;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;

/**
 * 自正则化大偏差理论请求模型
 * 
 * @author jiangyang
 * @since 1.0.0
 */
@Data
public class SelfNormalizedLargeDeviationRequest {

    /**
     * 请求ID
     */
    @NotBlank(message = "请求ID不能为空")
    private String requestId;

    /**
     * 应用场景类型
     */
    @NotBlank(message = "应用场景类型不能为空")
    private String applicationType; // GENE_SCREENING, FINANCIAL_RISK, INSURANCE_MODELING

    /**
     * 数据样本
     */
    @NotNull(message = "数据样本不能为空")
    private List<Double> dataSamples;

    /**
     * 样本数量
     */
    @Min(value = 1, message = "样本数量必须大于0")
    private Integer sampleSize;

    /**
     * 目标基因数量（基因筛选场景）
     */
    private Integer targetGeneCount;

    /**
     * 初选基因数量（基因筛选场景）
     */
    private Integer initialGeneCount;

    /**
     * 置信水平
     */
    private Double confidenceLevel = 0.95;

    /**
     * 阈值参数
     */
    private Double threshold;

    /**
     * 扩展参数
     */
    private Map<String, Object> extraParams;

    /**
     * 基因筛选特定参数
     */
    @Data
    public static class GeneScreeningParams {
        /**
         * 基因表达数据
         */
        private List<GeneExpressionData> geneExpressions;
        
        /**
         * 疾病关联度阈值
         */
        private Double diseaseAssociationThreshold;
        
        /**
         * 最小致病基因数量
         */
        private Integer minPathogenicGeneCount;
    }

    /**
     * 基因表达数据
     */
    @Data
    public static class GeneExpressionData {
        /**
         * 基因ID
         */
        private String geneId;
        
        /**
         * 基因名称
         */
        private String geneName;
        
        /**
         * 表达值
         */
        private Double expressionValue;
        
        /**
         * 标准差
         */
        private Double standardDeviation;
        
        /**
         * 样本数量
         */
        private Integer sampleCount;
    }
}
