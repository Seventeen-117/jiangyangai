package com.jiangyang.base.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.lang.Nullable;
import java.util.Map;

/**
 * 动态数据源
 * 继承AbstractRoutingDataSource，实现动态数据源切换
 */
public class DynamicDataSource extends AbstractRoutingDataSource {
    
    private boolean initialized = false;
    private Object defaultTargetDataSourceRef;
    
    @Nullable
    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.getDataSourceKey();
    }

    @Override
    public void setTargetDataSources(Map<Object, Object> targetDataSources) {
        super.setTargetDataSources(targetDataSources);
        this.initialized = true;
    }

    @Override
    public void setDefaultTargetDataSource(Object defaultTargetDataSource) {
        super.setDefaultTargetDataSource(defaultTargetDataSource);
        this.defaultTargetDataSourceRef = defaultTargetDataSource;
        this.initialized = true;
    }

    @Override
    public void afterPropertiesSet() {
        if (!this.initialized) {
            throw new IllegalStateException("DataSource router not initialized - call setTargetDataSources() and setDefaultTargetDataSource() first");
        }
        super.afterPropertiesSet();
    }

    /**
     * 暴露 defaultTargetDataSource 供 SeataResourceManagerListener 获取
     */
    public Object resolveDefaultTargetDataSource() {
        return super.resolveSpecifiedDataSource(this.defaultTargetDataSourceRef);
    }
}
