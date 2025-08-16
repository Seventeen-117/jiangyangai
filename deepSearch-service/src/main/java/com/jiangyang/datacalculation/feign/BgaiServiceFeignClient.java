package com.jiangyang.datacalculation.feign;

import com.jiangyang.datacalculation.model.BgaiServiceRequest;
import com.jiangyang.datacalculation.model.BgaiServiceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * BGAI服务Feign客户端
 * 
 * @author jiangyang
 * @since 1.0.0
 */
@FeignClient(name = "bgai-service", url = "${service.bgai.base-url}")
public interface BgaiServiceFeignClient {

    /**
     * 提交逻辑流程图和文字，获取计算基础数据
     * 
     * @param request BGAI服务请求
     * @return BGAI服务响应
     */
    @PostMapping("/api/chat/process")
    BgaiServiceResponse processCalculationRequest(@RequestBody BgaiServiceRequest request);

    /**
     * 获取计算规则和建议
     * 
     * @param request BGAI服务请求
     * @return BGAI服务响应
     */
    @PostMapping("/api/calculation/rules")
    BgaiServiceResponse getCalculationRules(@RequestBody BgaiServiceRequest request);

    /**
     * 验证计算参数
     * 
     * @param request BGAI服务请求
     * @return BGAI服务响应
     */
    @PostMapping("/api/calculation/validate")
    BgaiServiceResponse validateCalculationParameters(@RequestBody BgaiServiceRequest request);
}
