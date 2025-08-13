package com.jiangyang.datacalculation.model.algorithm;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.List;
import java.util.Map;

/**
 * 贝叶斯累积概率模型请求模型
 * 
 * @author jiangyang
 * @since 1.0.0
 */
@Data
public class BayesianCumulativeProbitRequest {

    /**
     * 请求ID
     */
    @NotBlank(message = "请求ID不能为空")
    private String requestId;

    /**
     * 应用场景类型
     */
    @NotBlank(message = "应用场景类型不能为空")
    private String applicationType; // SPORTS_RANKING, TEAM_EVALUATION

    /**
     * 比赛结果数据
     */
    @NotNull(message = "比赛结果数据不能为空")
    private List<MatchResult> matchResults;

    /**
     * 球队数据
     */
    @NotNull(message = "球队数据不能为空")
    private List<TeamData> teamData;

    /**
     * 先验参数
     */
    private PriorParameters priorParameters;

    /**
     * 模型参数
     */
    private ModelParameters modelParameters;

    /**
     * 扩展参数
     */
    private Map<String, Object> extraParams;

    /**
     * 比赛结果
     */
    @Data
    public static class MatchResult {
        /**
         * 比赛ID
         */
        @NotBlank(message = "比赛ID不能为空")
        private String matchId;

        /**
         * 主队ID
         */
        @NotBlank(message = "主队ID不能为空")
        private String homeTeamId;

        /**
         * 客队ID
         */
        @NotBlank(message = "客队ID不能为空")
        private String awayTeamId;

        /**
         * 比赛结果
         */
        @NotNull(message = "比赛结果不能为空")
        private MatchOutcome outcome;

        /**
         * 比赛时间
         */
        private Long matchTime;

        /**
         * 比赛权重
         */
        private Double weight = 1.0;

        /**
         * 比赛类型
         */
        private String matchType; // LEAGUE, CUP, FRIENDLY
    }

    /**
     * 比赛结果枚举
     */
    public enum MatchOutcome {
        HOME_WIN,    // 主队胜
        AWAY_WIN,    // 客队胜
        DRAW         // 平局
    }

    /**
     * 球队数据
     */
    @Data
    public static class TeamData {
        /**
         * 球队ID
         */
        @NotBlank(message = "球队ID不能为空")
        private String teamId;

        /**
         * 球队名称
         */
        private String teamName;

        /**
         * 外部评级
         */
        private Double externalRating;

        /**
         * 评级标准差
         */
        private Double ratingStandardDeviation = 1.0;

        /**
         * 历史表现权重
         */
        private Double historicalPerformanceWeight = 0.7;

        /**
         * 最近表现权重
         */
        private Double recentPerformanceWeight = 0.3;

        /**
         * 主场优势因子
         */
        private Double homeAdvantageFactor = 0.1;
    }

    /**
     * 先验参数
     */
    @Data
    public static class PriorParameters {
        /**
         * 球队实力先验均值
         */
        private Double teamStrengthPriorMean = 0.0;

        /**
         * 球队实力先验标准差
         */
        private Double teamStrengthPriorStd = 2.0;

        /**
         * 阈值参数先验均值
         */
        private List<Double> thresholdPriorMeans;

        /**
         * 阈值参数先验标准差
         */
        private Double thresholdPriorStd = 1.0;

        /**
         * 噪声参数先验形状
         */
        private Double noisePriorShape = 2.0;

        /**
         * 噪声参数先验尺度
         */
        private Double noisePriorScale = 1.0;
    }

    /**
     * 模型参数
     */
    @Data
    public static class ModelParameters {
        /**
         * 最大迭代次数
         */
        @Min(value = 100, message = "最大迭代次数必须大于等于100")
        private Integer maxIterations = 1000;

        /**
         * 收敛阈值
         */
        @Min(value = 1e-6, message = "收敛阈值必须大于等于1e-6")
        private Double convergenceThreshold = 1e-4;

        /**
         * 是否使用马尔可夫链蒙特卡洛
         */
        private Boolean useMCMC = true;

        /**
         * MCMC链数
         */
        @Min(value = 1, message = "MCMC链数必须大于等于1")
        private Integer mcmcChains = 4;

        /**
         * MCMC预热期长度
         */
        @Min(value = 100, message = "MCMC预热期长度必须大于等于100")
        private Integer warmupLength = 500;

        /**
         * 是否使用并行计算
         */
        private Boolean useParallel = true;
    }
}
