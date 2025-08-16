package com.jiangyang.base.datasource.annotation;

import java.lang.annotation.*;

/**
 * 数据源切换注解
 * 支持任意字符串值，可以指定任意数据源名称
 * 使用方式：@DataSource("dataSourceName")
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DataSource {
    /**
     * 数据源名称，支持任意字符串值
     * 例如：@DataSource("master"), @DataSource("slave"), @DataSource("audit"), @DataSource("user_db") 等
     */
    String value() default "";
}
