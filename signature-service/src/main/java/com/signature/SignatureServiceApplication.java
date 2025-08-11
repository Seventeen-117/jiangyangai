package com.signature;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 签名验证服务启动类
 * 
 * @author bgpay
 * @since 2024-01-01
 */
@SpringBootApplication(
    scanBasePackages = "com.signature",
    exclude = {
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration.class
        // 移除 MyBatis-Plus 自动配置排除项，启用 MyBatis-Plus 功能
    }
)
@EnableFeignClients(basePackages = "com.signature.feign")
@EnableDubbo  // 启用Dubbo
@MapperScan("com.signature.mapper")
public class SignatureServiceApplication {

    public static void main(String[] args) {
        // 解决 Java 17 模块化系统与 Hessian 序列化的兼容性问题
        addJava17Compatibility();
        
        SpringApplication.run(SignatureServiceApplication.class, args);
    }
    
    /**
     * 添加 Java 17 兼容性配置
     * 解决 Hessian 序列化在 Java 17 下的反射访问限制问题
     */
    private static void addJava17Compatibility() {
        // 检查是否已经设置了相关的系统属性
        if (System.getProperty("java.version").startsWith("17") || 
            System.getProperty("java.version").startsWith("18") || 
            System.getProperty("java.version").startsWith("19") ||
            System.getProperty("java.version").startsWith("20") ||
            System.getProperty("java.version").startsWith("21")) {
            
            // 为 Hessian 序列化设置系统属性，允许访问 java.math 模块
            System.setProperty("hessian.allowNonSerializable", "true");
            
            // 输出提示信息
            System.out.println("Java 17+ detected. If you encounter Hessian serialization issues, " +
                "please add JVM arguments: --add-opens java.base/java.math=ALL-UNNAMED " +
                "--add-opens java.base/java.lang=ALL-UNNAMED " +
                "--add-opens java.base/java.util=ALL-UNNAMED");
        }
    }
} 