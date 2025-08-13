package com.jiangyang.datacalculation.model.algorithm;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.List;
import java.util.Map;

/**
 * 威尔逊置信区间排序请求模型
 * 
 * @author jiangyang
 * @since 1.0.0
 */
@Data
public class WilsonScoreRequest {

    /**
     * 请求ID
     */
    @NotBlank(message = "请求ID不能为空")
    private String requestId;

    /**
     * 应用场景类型
     */
    @NotBlank(message = "应用场景类型不能为空")
    private String applicationType; // ECOMMERCE_RANKING, COMMUNITY_REPUTATION

    /**
     * 评分数据列表
     */
    @NotNull(message = "评分数据不能为空")
    private List<RatingData> ratingDataList;

    /**
     * 置信水平
     */
    @Min(value = 0.5, message = "置信水平必须大于等于0.5")
    @Max(value = 0.999, message = "置信水平必须小于0.999")
    private Double confidenceLevel = 0.95;

    /**
     * 扩展参数
     */
    private Map<String, Object> extraParams;

    /**
     * 评分数据
     */
    @Data
    public static class RatingData {
        /**
         * 项目ID
         */
        @NotBlank(message = "项目ID不能为空")
        private String itemId;

        /**
         * 项目名称
         */
        private String itemName;

        /**
         * 正面评价数量
         */
        @Min(value = 0, message = "正面评价数量不能为负数")
        private Integer positiveCount;

        /**
         * 总评价数量
         */
        @Min(value = 1, message = "总评价数量必须大于0")
        private Integer totalCount;

        /**
         * 原始评分（可选）
         */
        private Double originalScore;

        /**
         * 评分权重
         */
        private Double weight = 1.0;

        /**
         * 时间衰减因子
         */
        private Double timeDecayFactor = 1.0;

        /**
         * 最后更新时间
         */
        private Long lastUpdateTime;
    }

    /**
     * 电商排名特定参数
     */
    @Data
    public static class EcommerceRankingParams {
        /**
         * 是否考虑评价时间
         */
        private Boolean considerTimeDecay = true;

        /**
         * 时间衰减天数
         */
        private Integer timeDecayDays = 365;

        /**
         * 最小评价数量阈值
         */
        private Integer minReviewThreshold = 5;

        /**
         * 是否使用加权评分
         */
        private Boolean useWeightedScore = true;

        /**
         * 评分标准化方法
         */
        private String normalizationMethod = "Z_SCORE"; // Z_SCORE, MIN_MAX, DECIMAL
    }

    /**
     * 社区信誉系统参数
     */
    @Data
    public static class CommunityReputationParams {
        /**
         * 信誉等级数量
         */
        private Integer reputationLevels = 5;

        /**
         * 是否考虑用户活跃度
         */
        private Boolean considerUserActivity = true;

        /**
         * 活跃度权重
         */
        private Double activityWeight = 0.3;

        /**
         * 信誉更新频率（天）
         */
        private Integer reputationUpdateFrequency = 7;
    }
}
