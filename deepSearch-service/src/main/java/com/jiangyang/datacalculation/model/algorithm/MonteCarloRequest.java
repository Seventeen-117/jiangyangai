package com.jiangyang.datacalculation.model.algorithm;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;

/**
 * 蒙特卡洛方法请求模型
 * 
 * @author jiangyang
 * @since 1.0.0
 */
@Data
public class MonteCarloRequest {

    /**
     * 请求ID
     */
    @NotBlank(message = "请求ID不能为空")
    private String requestId;

    /**
     * 应用场景类型
     */
    @NotBlank(message = "应用场景类型不能为空")
    private String applicationType; // FINANCIAL_PRICING, PARTICLE_PHYSICS, PI_CALCULATION

    /**
     * 模拟次数
     */
    @Min(value = 1000, message = "模拟次数必须大于等于1000")
    private Integer simulationCount = 10000;

    /**
     * 随机种子
     */
    private Long randomSeed;

    /**
     * 扩展参数
     */
    private Map<String, Object> extraParams;

    /**
     * 金融衍生品定价参数
     */
    @Data
    public static class FinancialPricingParams {
        /**
         * 期权类型
         */
        private String optionType; // CALL, PUT
        
        /**
         * 标的资产价格
         */
        private Double underlyingPrice;
        
        /**
         * 执行价格
         */
        private Double strikePrice;
        
        /**
         * 无风险利率
         */
        private Double riskFreeRate;
        
        /**
         * 波动率
         */
        private Double volatility;
        
        /**
         * 到期时间（年）
         */
        private Double timeToMaturity;
        
        /**
         * 股息率
         */
        private Double dividendYield = 0.0;
    }

    /**
     * 粒子物理模拟参数
     */
    @Data
    public static class ParticlePhysicsParams {
        /**
         * 初始粒子数量
         */
        private Integer initialParticleCount;
        
        /**
         * 模拟时间步长
         */
        private Double timeStep;
        
        /**
         * 总模拟时间
         */
        private Double totalSimulationTime;
        
        /**
         * 核反应截面
         */
        private Double crossSection;
        
        /**
         * 材料密度
         */
        private Double materialDensity;
        
        /**
         * 粒子能量
         */
        private Double particleEnergy;
    }

    /**
     * 圆周率计算参数
     */
    @Data
    public static class PiCalculationParams {
        /**
         * 计算精度
         */
        private Integer precision;
        
        /**
         * 是否使用并行计算
         */
        private Boolean useParallel = true;
        
        /**
         * 并行线程数
         */
        private Integer threadCount = Runtime.getRuntime().availableProcessors();
    }
}
