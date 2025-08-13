package com.jiangyang.datacalculation.model.algorithm;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 算法计算通用响应模型
 * 
 * @author jiangyang
 * @since 1.0.0
 */
@Data
public class AlgorithmResponse {

    /**
     * 响应代码
     */
    private Integer code = 200;

    /**
     * 响应消息
     */
    private String message = "计算成功";

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 算法类型
     */
    private String algorithmType;

    /**
     * 计算耗时（毫秒）
     */
    private Long executionTime;

    /**
     * 计算结果
     */
    private Object result;

    /**
     * 扩展信息
     */
    private Map<String, Object> extraInfo;

    /**
     * 自正则化大偏差理论响应
     */
    @Data
    public static class SelfNormalizedLargeDeviationResult {
        /**
         * 大偏差概率
         */
        private Double largeDeviationProbability;

        /**
         * 置信区间下界
         */
        private Double confidenceIntervalLower;

        /**
         * 置信区间上界
         */
        private Double confidenceIntervalUpper;

        /**
         * 基因筛选结果（如果适用）
         */
        private GeneScreeningResult geneScreeningResult;

        /**
         * 金融风险结果（如果适用）
         */
        private FinancialRiskResult financialRiskResult;

        /**
         * 统计显著性
         */
        private Double statisticalSignificance;

        /**
         * 理论突破点
         */
        private String theoreticalBreakthrough;
    }

    /**
     * 基因筛选结果
     */
    @Data
    public static class GeneScreeningResult {
        /**
         * 初选基因数量
         */
        private Integer initialGeneCount;

        /**
         * 锁定致病基因数量
         */
        private Integer lockedPathogenicGeneCount;

        /**
         * 筛选效率提升百分比
         */
        private Double efficiencyImprovement;

        /**
         * 推荐基因列表
         */
        private List<GeneRecommendation> recommendedGenes;
    }

    /**
     * 基因推荐
     */
    @Data
    public static class GeneRecommendation {
        /**
         * 基因ID
         */
        private String geneId;

        /**
         * 基因名称
         */
        private String geneName;

        /**
         * 致病概率
         */
        private Double pathogenicProbability;

        /**
         * 置信度
         */
        private Double confidence;

        /**
         * 推荐理由
         */
        private String recommendationReason;
    }

    /**
     * 金融风险结果
     */
    @Data
    public static class FinancialRiskResult {
        /**
         * 破产概率
         */
        private Double bankruptcyProbability;

        /**
         * 风险等级
         */
        private String riskLevel;

        /**
         * 风险预警阈值
         */
        private Double riskWarningThreshold;

        /**
         * 建议措施
         */
        private List<String> recommendedActions;
    }

    /**
     * 蒙特卡洛方法响应
     */
    @Data
    public static class MonteCarloResult {
        /**
         * 模拟结果
         */
        private Double simulationResult;

        /**
         * 标准误差
         */
        private Double standardError;

        /**
         * 置信区间
         */
        private ConfidenceInterval confidenceInterval;

        /**
         * 收敛性指标
         */
        private ConvergenceMetrics convergenceMetrics;

        /**
         * 应用场景特定结果
         */
        private Object applicationSpecificResult;
    }

    /**
     * 置信区间
     */
    @Data
    public static class ConfidenceInterval {
        private Double lowerBound;
        private Double upperBound;
        private Double confidenceLevel;
    }

    /**
     * 收敛性指标
     */
    @Data
    public static class ConvergenceMetrics {
        private Boolean isConverged;
        private Integer iterationsToConvergence;
        private Double convergenceRate;
        private Double finalError;
    }

    /**
     * 威尔逊评分响应
     */
    @Data
    public static class WilsonScoreResult {
        /**
         * 排序后的评分列表
         */
        private List<RankedScore> rankedScores;

        /**
         * 置信区间调整后的评分
         */
        private List<AdjustedScore> adjustedScores;

        /**
         * 排序质量指标
         */
        private RankingQualityMetrics qualityMetrics;
    }

    /**
     * 排序后的评分
     */
    @Data
    public static class RankedScore {
        private String itemId;
        private String itemName;
        private Double originalScore;
        private Double adjustedScore;
        private Integer rank;
        private Double confidence;
        private Integer totalCount;
        private Integer positiveCount;
    }

    /**
     * 调整后的评分
     */
    @Data
    public static class AdjustedScore {
        private String itemId;
        private Double wilsonScore;
        private Double confidenceIntervalLower;
        private Double confidenceIntervalUpper;
        private Double timeDecayAdjustedScore;
    }

    /**
     * 排序质量指标
     */
    @Data
    public static class RankingQualityMetrics {
        private Double overallConfidence;
        private Double rankingStability;
        private Double sampleSizeAdequacy;
        private String recommendations;
    }

    /**
     * 贝叶斯累积概率模型响应
     */
    @Data
    public static class BayesianCumulativeProbitResult {
        /**
         * 球队实力排名
         */
        private List<TeamRanking> teamRankings;

        /**
         * 模型参数估计
         */
        private ModelParameterEstimates parameterEstimates;

        /**
         * 预测准确性
         */
        private PredictionAccuracy predictionAccuracy;

        /**
         * 收敛性信息
         */
        private MCMCConvergenceInfo mcmcConvergenceInfo;
    }

    /**
     * 球队排名
     */
    @Data
    public static class TeamRanking {
        private String teamId;
        private String teamName;
        private Double estimatedStrength;
        private Double strengthUncertainty;
        private Integer rank;
        private Double winProbability;
        private Double drawProbability;
        private Double lossProbability;
    }

    /**
     * 模型参数估计
     */
    @Data
    public static class ModelParameterEstimates {
        private List<Double> thresholdEstimates;
        private Double noiseParameter;
        private Map<String, Double> teamStrengthEstimates;
        private Double modelFit;
    }

    /**
     * 预测准确性
     */
    @Data
    public static class PredictionAccuracy {
        private Double overallAccuracy;
        private Double homeWinAccuracy;
        private Double awayWinAccuracy;
        private Double drawAccuracy;
        private List<MatchPrediction> matchPredictions;
    }

    /**
     * 比赛预测
     */
    @Data
    public static class MatchPrediction {
        private String matchId;
        private String homeTeamId;
        private String awayTeamId;
        private Double predictedHomeWinProb;
        private Double predictedAwayWinProb;
        private Double predictedDrawProb;
        private String predictedOutcome;
        private Double predictionConfidence;
    }

    /**
     * MCMC收敛信息
     */
    @Data
    public static class MCMCConvergenceInfo {
        private Boolean isConverged;
        private Double gelmanRubinStatistic;
        private Integer effectiveSampleSize;
        private Double acceptanceRate;
        private List<Double> tracePlotData;
    }
}
