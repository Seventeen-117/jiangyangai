package com.jiangyang.datacalculation.service;

import com.jiangyang.base.datasource.annotation.DataSource;
import com.jiangyang.datacalculation.model.CalculationRequest;
import com.jiangyang.datacalculation.model.CalculationResponse;

/**
 * 数据计算服务接口
 * 
 * @author jiangyang
 * @since 1.0.0
 */
@DataSource("master")
public interface DataCalculationService {

    /**
     * 执行数据计算
     * 
     * @param request 计算请求
     * @return 计算响应
     */
    CalculationResponse executeCalculation(CalculationRequest request);

    /**
     * 异步执行数据计算
     * 
     * @param request 计算请求
     * @return 任务ID
     */
    String executeCalculationAsync(CalculationRequest request);

    /**
     * 获取计算状态
     * 
     * @param requestId 请求ID
     * @return 计算状态
     */
    CalculationResponse getCalculationStatus(String requestId);

    /**
     * 取消计算任务
     * 
     * @param requestId 请求ID
     * @return 是否取消成功
     */
    boolean cancelCalculation(String requestId);
}
