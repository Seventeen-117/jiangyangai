package com.jiangyang.base.datasource;

/**
 * 数据源上下文持有者
 * 使用ThreadLocal存储当前线程的数据源标识
 */
public class DataSourceContextHolder {

    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 设置数据源标识
     */
    public static void setDataSourceKey(String key) {
        CONTEXT_HOLDER.set(key);
    }

    /**
     * 获取数据源标识
     */
    public static String getDataSourceKey() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 清除数据源标识
     */
    public static void clearDataSourceKey() {
        CONTEXT_HOLDER.remove();
    }
}
