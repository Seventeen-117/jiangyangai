package com.jiangyang.base.seata.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Seata配置属性
 * 所有配置都从外部配置文件读取，不设置硬编码默认值
 */
@Data
@ConfigurationProperties(prefix = "seata")
public class SeataProperties {

    /**
     * 是否启用Seata
     */
    private Boolean enabled;

    /**
     * 应用ID
     */
    private String applicationId;

    /**
     * 事务服务组
     */
    private String txServiceGroup;

    /**
     * 事务超时时间（毫秒）
     */
    private Integer timeout;

    /**
     * 是否启用数据源代理
     */
    private Boolean enableAutoDataSourceProxy;

    /**
     * 数据源代理模式
     */
    private String dataSourceProxyMode;

    /**
     * 是否启用Saga
     */
    private Boolean sagaEnabled;

    /**
     * Saga状态机自动注册
     */
    private Boolean sagaStateMachineAutoRegister;

    /**
     * 注册中心配置
     */
    private Registry registry = new Registry();

    /**
     * 配置中心配置
     */
    private Config config = new Config();

    /**
     * 事务存储配置
     */
    private Store store = new Store();

    /**
     * 注册中心配置
     */
    @Data
    public static class Registry {
        /**
         * 注册中心类型
         */
        private String type;

        /**
         * 注册中心地址
         */
        private String serverAddr;

        /**
         * 命名空间
         */
        private String namespace;

        /**
         * 分组
         */
        private String group;

        /**
         * 集群名称
         */
        private String cluster;

        /**
         * 用户名
         */
        private String username;

        /**
         * 密码
         */
        private String password;
    }

    /**
     * 配置中心配置
     */
    @Data
    public static class Config {
        /**
         * 配置中心类型
         */
        private String type;

        /**
         * 配置中心地址
         */
        private String serverAddr;

        /**
         * 命名空间
         */
        private String namespace;

        /**
         * 分组
         */
        private String group;

        /**
         * 用户名
         */
        private String username;

        /**
         * 密码
         */
        private String password;
    }

    /**
     * 事务存储配置
     */
    @Data
    public static class Store {
        /**
         * 存储模式
         */
        private String mode;

        /**
         * 文件存储路径
         */
        private String fileDir;

        /**
         * 数据库存储配置
         */
        private Database database = new Database();

        /**
         * 数据库存储配置
         */
        @Data
        public static class Database {
            /**
             * 数据源类型
             */
            private String datasource;

            /**
             * 数据库类型
             */
            private String dbType;

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
             * 最小连接数
             */
            private Integer minConn;

            /**
             * 最大连接数
             */
            private Integer maxConn;

            /**
             * 全局事务表
             */
            private String globalTable;

            /**
             * 分支事务表
             */
            private String branchTable;

            /**
             * 全局锁表
             */
            private String lockTable;

            /**
             * 查询超时时间
             */
            private Integer queryTimeout;
        }
    }
}
