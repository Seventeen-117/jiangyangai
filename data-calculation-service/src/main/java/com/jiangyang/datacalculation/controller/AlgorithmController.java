package com.jiangyang.datacalculation.controller;

import com.jiangyang.datacalculation.model.algorithm.*;
import com.jiangyang.datacalculation.service.AlgorithmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 算法计算服务控制器
 * 提供各种理论算法和实际应用算法的REST API接口
 * 
 * @author jiangyang
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/algorithm")
@Validated
public class AlgorithmController {

    @Autowired
    private AlgorithmService algorithmService;

    /**
     * 自正则化大偏差理论计算
     */
    @PostMapping("/self-normalized-large-deviation")
    public ResponseEntity<AlgorithmResponse.SelfNormalizedLargeDeviationResult> calculateSelfNormalizedLargeDeviation(
            @Valid @RequestBody SelfNormalizedLargeDeviationRequest request) {
        log.info("收到自正则化大偏差理论计算请求，请求ID: {}, 应用场景: {}", 
                request.getRequestId(), request.getApplicationType());
        
        try {
            AlgorithmResponse.SelfNormalizedLargeDeviationResult result = 
                    algorithmService.calculateSelfNormalizedLargeDeviation(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("自正则化大偏差理论计算失败，请求ID: {}, 错误: {}", 
                    request.getRequestId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 蒙特卡洛方法计算
     */
    @PostMapping("/monte-carlo")
    public ResponseEntity<AlgorithmResponse.MonteCarloResult> calculateMonteCarlo(
            @Valid @RequestBody MonteCarloRequest request) {
        log.info("收到蒙特卡洛方法计算请求，请求ID: {}, 应用场景: {}, 模拟次数: {}", 
                request.getRequestId(), request.getApplicationType(), request.getSimulationCount());
        
        try {
            AlgorithmResponse.MonteCarloResult result = algorithmService.calculateMonteCarlo(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("蒙特卡洛方法计算失败，请求ID: {}, 错误: {}", 
                    request.getRequestId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 威尔逊置信区间排序计算
     */
    @PostMapping("/wilson-score")
    public ResponseEntity<AlgorithmResponse.WilsonScoreResult> calculateWilsonScore(
            @Valid @RequestBody WilsonScoreRequest request) {
        log.info("收到威尔逊置信区间排序计算请求，请求ID: {}, 应用场景: {}, 数据项数: {}", 
                request.getRequestId(), request.getApplicationType(), 
                request.getRatingDataList().size());
        
        try {
            AlgorithmResponse.WilsonScoreResult result = algorithmService.calculateWilsonScore(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("威尔逊置信区间排序计算失败，请求ID: {}, 错误: {}", 
                    request.getRequestId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 贝叶斯累积概率模型计算
     */
    @PostMapping("/bayesian-cumulative-probit")
    public ResponseEntity<AlgorithmResponse.BayesianCumulativeProbitResult> calculateBayesianCumulativeProbit(
            @Valid @RequestBody BayesianCumulativeProbitRequest request) {
        log.info("收到贝叶斯累积概率模型计算请求，请求ID: {}, 应用场景: {}, 比赛数: {}, 球队数: {}", 
                request.getRequestId(), request.getApplicationType(), 
                request.getMatchResults().size(), request.getTeamData().size());
        
        try {
            AlgorithmResponse.BayesianCumulativeProbitResult result = 
                    algorithmService.calculateBayesianCumulativeProbit(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("贝叶斯累积概率模型计算失败，请求ID: {}, 错误: {}", 
                    request.getRequestId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 算法服务健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "algorithm-service");
        health.put("timestamp", System.currentTimeMillis());
        health.put("version", "1.0.0");
        health.put("supportedAlgorithms", new String[]{
            "Self-Normalized Large Deviation",
            "Monte Carlo Method",
            "Wilson Score Interval",
            "Bayesian Cumulative Probit",
            "Stein's Method",
            "Derandomization"
        });
        
        return ResponseEntity.ok(health);
    }

    /**
     * 算法服务信息
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getServiceInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("serviceName", "algorithm-service");
        info.put("description", "理论算法与实用算法计算服务");
        info.put("version", "1.0.0");
        info.put("author", "jiangyang");
        info.put("theoreticalAlgorithms", new String[]{
            "自正则化大偏差理论",
            "斯坦因方法",
            "动态随机算法去随机化"
        });
        info.put("practicalAlgorithms", new String[]{
            "蒙特卡洛方法",
            "威尔逊置信区间排序",
            "贝叶斯累积概率模型"
        });
        info.put("applicationScenarios", new String[]{
            "基因筛选",
            "金融风险建模",
            "期权定价",
            "圆周率计算",
            "粒子物理模拟",
            "电商产品排名",
            "社区用户信誉",
            "体育比赛排名"
        });
        
        return ResponseEntity.ok(info);
    }
}
