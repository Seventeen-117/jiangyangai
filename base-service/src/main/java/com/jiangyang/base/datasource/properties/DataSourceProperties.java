package com.jiangyang.base.datasource.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据源配置属性
 * 支持动态数量的数据源配置
 */
@Data
@ConfigurationProperties(prefix = "spring.datasource")
public class DataSourceProperties {
    
    /**
     * 动态数据源配置
     */
    private Dynamic dynamic = new Dynamic();
    
    /**
     * 数据源类型
     */
    private String type = "com.alibaba.druid.pool.DruidDataSource";
    
    /**
     * 动态数据源配置类
     */
    @Data
    public static class Dynamic {
        /**
         * 主数据源名称
         */
        private String primary = "master";
        
        /**
         * 是否严格模式
         */
        private boolean strict = false;
        
        /**
         * 是否启用Seata
         */
        private boolean seata = true;
        
        /**
         * 数据源配置映射
         * 支持任意数量的数据源，例如：
         * datasource:
         *   master: {...}
         *   slave: {...}
         *   audit: {...}
         *   user_db: {...}
         *   order_db: {...}
         */
        private Map<String, DataSourceConfig> datasource = new HashMap<>();
    }
    
    /**
     * 单个数据源配置
     */
    @Data
    public static class DataSourceConfig {
        /**
         * 驱动类名
         */
        private String driverClassName;
        
        /**
         * 数据库URL
         */
        private String url;
        
        /**
         * 用户名
         */
        private String username;
        
        /**
         * 密码
         */
        private String password;
        
        /**
         * 数据源类型
         */
        private String type;
        
        /**
         * 连接池配置
         */
        private Pool pool = new Pool();
        
        /**
         * 连接池配置
         */
        @Data
        public static class Pool {
            /**
             * 初始连接数
             */
            private int initialSize = 5;
            
            /**
             * 最小空闲连接数
             */
            private int minIdle = 10;
            
            /**
             * 最大活跃连接数
             */
            private int maxActive = 20;
            
            /**
             * 获取连接等待超时时间（毫秒）
             */
            private long maxWait = 60000;
            
            /**
             * 空闲连接检测间隔（毫秒）
             */
            private long timeBetweenEvictionRunsMillis = 60000;
            
            /**
             * 连接最小空闲时间（毫秒）
             */
            private long minEvictableIdleTimeMillis = 300000;
            
            /**
             * 验证查询SQL
             */
            private String validationQuery = "SELECT 1";
            
            /**
             * 空闲时是否验证
             */
            private boolean testWhileIdle = true;
            
            /**
             * 借用连接时是否验证
             */
            private boolean testOnBorrow = false;
            
            /**
             * 归还连接时是否验证
             */
            private boolean testOnReturn = false;
            
            /**
             * 是否缓存预处理语句
             */
            private boolean poolPreparedStatements = true;
            
            /**
             * 每个连接最大预处理语句数
             */
            private int maxPoolPreparedStatementPerConnectionSize = 20;
            
            /**
             * 过滤器配置
             */
            private String filters = "stat,wall";
            
            /**
             * 连接属性
             */
            private String connectionProperties = "druid.stat.mergeSql=true;druid.stat.slowSqlMillis=5000";
        }
    }
}
