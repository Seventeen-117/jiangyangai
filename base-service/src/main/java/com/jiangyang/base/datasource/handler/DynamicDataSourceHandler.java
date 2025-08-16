package com.jiangyang.base.datasource.handler;

import com.jiangyang.base.datasource.DataSourceContextHolder;
import com.jiangyang.base.datasource.annotation.DataSource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 动态数据源处理器
 * 使用AOP实现数据源切换
 */
@Aspect
@Order(1)
@Component
public class DynamicDataSourceHandler {
    private static final Logger log = LoggerFactory.getLogger(DynamicDataSourceHandler.class);

    @Pointcut("@annotation(com.jiangyang.base.datasource.annotation.DataSource)"
            + "|| @within(com.jiangyang.base.datasource.annotation.DataSource)")
    public void dsPointCut() {
    }

    @Around("dsPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        DataSource dataSource = getDataSource(point);

        if (Objects.nonNull(dataSource) && !dataSource.value().isEmpty()) {
            DataSourceContextHolder.setDataSourceKey(dataSource.value());
            log.debug("Set data source to: {}", dataSource.value());
        }

        try {
            return point.proceed();
        } finally {
            DataSourceContextHolder.clearDataSourceKey();
            log.debug("Clear data source");
        }
    }

    private DataSource getDataSource(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        DataSource dataSource = AnnotationUtils.findAnnotation(method, DataSource.class);
        if (Objects.nonNull(dataSource)) {
            return dataSource;
        }

        return AnnotationUtils.findAnnotation(signature.getDeclaringType(), DataSource.class);
    }
}
