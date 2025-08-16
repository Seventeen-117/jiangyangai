package com.jiangyang.datacalculation.service;
import com.jiangyang.base.datasource.annotation.DataSource;
import com.jiangyang.datacalculation.model.algorithm.*;

import java.util.List;
import java.util.Map;

/**
 * 算法服务接口
 * 提供各种理论算法和实际应用算法的计算服务
 * 
 * @author jiangyang
 * @since 1.0.0
 */
@DataSource("master")
public interface AlgorithmService {

    /**
     * 自正则化大偏差理论计算
     * 
     * @param request 请求参数
     * @return 计算结果
     */
    AlgorithmResponse.SelfNormalizedLargeDeviationResult calculateSelfNormalizedLargeDeviation(
            SelfNormalizedLargeDeviationRequest request);

    /**
     * 蒙特卡洛方法计算
     * 
     * @param request 请求参数
     * @return 计算结果
     */
    AlgorithmResponse.MonteCarloResult calculateMonteCarlo(MonteCarloRequest request);

    /**
     * 威尔逊置信区间排序计算
     * 
     * @param request 请求参数
     * @return 计算结果
     */
    AlgorithmResponse.WilsonScoreResult calculateWilsonScore(WilsonScoreRequest request);

    /**
     * 贝叶斯累积概率模型计算
     * 
     * @param request 请求参数
     * @return 计算结果
     */
    AlgorithmResponse.BayesianCumulativeProbitResult calculateBayesianCumulativeProbit(
            BayesianCumulativeProbitRequest request);

    /**
     * 斯坦因方法计算
     * 通过构造微分方程关联目标分布与已知分布
     * 
     * @param data 输入数据
     * @param targetDistribution 目标分布类型
     * @param parameters 分布参数
     * @return 逼近结果
     */
    Object calculateSteinMethod(List<Double> data, String targetDistribution, Map<String, Object> parameters);

    /**
     * 动态随机算法去随机化
     * 将随机算法转换为确定性算法
     * 
     * @param algorithmType 算法类型
     * @param inputData 输入数据
     * @param parameters 算法参数
     * @return 确定性算法结果
     */
    Object derandomizeAlgorithm(String algorithmType, Object inputData, Map<String, Object> parameters);

    /**
     * 基因筛选算法
     * 基于自正则化大偏差理论的基因筛选
     * 
     * @param geneData 基因数据
     * @param threshold 筛选阈值
     * @param confidenceLevel 置信水平
     * @return 筛选结果
     */
    AlgorithmResponse.GeneScreeningResult screenGenes(List<SelfNormalizedLargeDeviationRequest.GeneExpressionData> geneData,
                                                     Double threshold, Double confidenceLevel);

    /**
     * 金融风险建模
     * 基于自正则化大偏差理论的金融风险计算
     * 
     * @param financialData 金融数据
     * @param riskType 风险类型
     * @param parameters 风险参数
     * @return 风险结果
     */
    AlgorithmResponse.FinancialRiskResult calculateFinancialRisk(List<Double> financialData, String riskType,
                                                               Map<String, Object> parameters);

    /**
     * 期权定价计算
     * 基于蒙特卡洛方法的金融衍生品定价
     * 
     * @param pricingParams 定价参数
     * @param simulationCount 模拟次数
     * @return 定价结果
     */
    Double calculateOptionPrice(MonteCarloRequest.FinancialPricingParams pricingParams, Integer simulationCount);

    /**
     * 圆周率计算
     * 基于蒙特卡洛方法的圆周率近似计算
     * 
     * @param precision 计算精度
     * @param simulationCount 模拟次数
     * @return 圆周率近似值
     */
    Double calculatePi(Integer precision, Integer simulationCount);

    /**
     * 粒子物理模拟
     * 基于蒙特卡洛方法的核反应粒子轨迹模拟
     * 
     * @param physicsParams 物理参数
     * @param simulationCount 模拟次数
     * @return 模拟结果
     */
    Object simulateParticlePhysics(MonteCarloRequest.ParticlePhysicsParams physicsParams, Integer simulationCount);

    /**
     * 电商产品排名
     * 基于威尔逊置信区间的产品评分排序
     * 
     * @param ratingData 评分数据
     * @param rankingParams 排名参数
     * @return 排名结果
     */
    AlgorithmResponse.WilsonScoreResult rankEcommerceProducts(List<WilsonScoreRequest.RatingData> ratingData,
                                                             WilsonScoreRequest.EcommerceRankingParams rankingParams);

    /**
     * 社区用户信誉评估
     * 基于威尔逊置信区间的用户信誉系统
     * 
     * @param ratingData 评分数据
     * @param reputationParams 信誉参数
     * @return 信誉评估结果
     */
    AlgorithmResponse.WilsonScoreResult evaluateCommunityReputation(List<WilsonScoreRequest.RatingData> ratingData,
                                                                  WilsonScoreRequest.CommunityReputationParams reputationParams);

    /**
     * 体育比赛排名预测
     * 基于贝叶斯累积概率模型的球队实力评估
     * 
     * @param matchData 比赛数据
     * @param teamData 球队数据
     * @param modelParams 模型参数
     * @return 排名预测结果
     */
    AlgorithmResponse.BayesianCumulativeProbitResult predictSportsRanking(List<BayesianCumulativeProbitRequest.MatchResult> matchData,
                                                                         List<BayesianCumulativeProbitRequest.TeamData> teamData,
                                                                         BayesianCumulativeProbitRequest.ModelParameters modelParams);

    /**
     * 批量算法计算
     * 支持多种算法的批量计算
     * 
     * @param algorithmRequests 算法请求列表
     * @return 批量计算结果
     */
    List<AlgorithmResponse> batchCalculate(List<Object> algorithmRequests);

    /**
     * 算法性能评估
     * 评估各种算法的计算性能和准确性
     * 
     * @param algorithmType 算法类型
     * @param testData 测试数据
     * @return 性能评估结果
     */
    Object evaluateAlgorithmPerformance(String algorithmType, Object testData);

    /**
     * 算法参数优化
     * 自动优化算法参数以获得最佳性能
     * 
     * @param algorithmType 算法类型
     * @param trainingData 训练数据
     * @param optimizationCriteria 优化标准
     * @return 优化后的参数
     */
    Map<String, Object> optimizeAlgorithmParameters(String algorithmType, Object trainingData, Map<String, Object> optimizationCriteria);
}
