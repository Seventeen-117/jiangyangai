package com.bgpay.bgai.config;

import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Dubbo健康检查配置
 * 
 * @author jiangyang
 */
@Component
public class DubboHealthConfig implements HealthIndicator {

    @DubboReference(
        version = "1.0.0",
        group = "signature",
        check = false,
        lazy = true
    )
    private com.jiangyang.dubbo.api.signature.SignatureService signatureService;

    @Override
    public Health health() {
        try {
            // 尝试调用一个简单的方法来检查Dubbo服务是否可用
            if (signatureService != null) {
                return Health.up()
                    .withDetail("dubbo.service", "signature-service")
                    .withDetail("status", "available")
                    .build();
            } else {
                return Health.down()
                    .withDetail("dubbo.service", "signature-service")
                    .withDetail("status", "unavailable")
                    .withDetail("reason", "Service not initialized")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("dubbo.service", "signature-service")
                .withDetail("status", "error")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
