package com.jiangyang.messages;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Spring Cloud上下文可用性检查条件
 * 确保在Spring Cloud环境下才启用消息服务自动配置
 */
public class SpringCloudContextCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        try {
            // 检查Spring Cloud相关类是否在类路径中
            Class.forName("org.springframework.cloud.context.refresh.ContextRefresher");
            Class.forName("org.springframework.cloud.context.config.annotation.RefreshScope");
            
            return ConditionOutcome.match(
                ConditionMessage.of("Spring Cloud context available")
            );
        } catch (ClassNotFoundException e) {
            return ConditionOutcome.noMatch(
                ConditionMessage.of("Spring Cloud context not available")
            );
        }
    }
}
