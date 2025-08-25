package com.jiangyang.messages;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;

/**
 * 测试应用程序启动类
 */
@SpringBootApplication(
    scanBasePackages = {"com.jiangyang.messages", "com.jiangyang.base"},
    exclude = {
        SqlInitializationAutoConfiguration.class, MybatisPlusAutoConfiguration.class
    }
)
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
