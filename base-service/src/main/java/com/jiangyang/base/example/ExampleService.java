package com.jiangyang.base.example;

import com.jiangyang.base.datasource.annotation.DataSource;
import org.springframework.stereotype.Service;

/**
 * 示例服务类
 * 展示如何使用 @DataSource 注解进行数据源切换
 */
@Service
public class ExampleService {

    /**
     * 使用主数据源
     */
    @DataSource("master")
    public void useMasterDataSource() {
        System.out.println("使用主数据源执行操作");
        // 这里执行数据库操作，会自动使用 master 数据源
    }

    /**
     * 使用从数据源
     */
    @DataSource("slave")
    public void useSlaveDataSource() {
        System.out.println("使用从数据源执行操作");
        // 这里执行数据库操作，会自动使用 slave 数据源
    }

    /**
     * 使用审计数据源
     */
    @DataSource("audit")
    public void useAuditDataSource() {
        System.out.println("使用审计数据源执行操作");
        // 这里执行数据库操作，会自动使用 audit 数据源
    }

    /**
     * 使用用户数据库
     */
    @DataSource("user_db")
    public void useUserDatabase() {
        System.out.println("使用用户数据库执行操作");
        // 这里执行数据库操作，会自动使用 user_db 数据源
    }

    /**
     * 使用订单数据库
     */
    @DataSource("order_db")
    public void useOrderDatabase() {
        System.out.println("使用订单数据库执行操作");
        // 这里执行数据库操作，会自动使用 order_db 数据源
    }

    /**
     * 类级别的数据源注解
     * 整个类的所有方法都使用指定的数据源
     */
    @DataSource("master")
    public static class MasterDataService {
        
        public void method1() {
            System.out.println("MasterDataService.method1() - 使用 master 数据源");
        }
        
        public void method2() {
            System.out.println("MasterDataService.method2() - 使用 master 数据源");
        }
    }

    /**
     * 方法级别的数据源注解优先级高于类级别
     */
    @DataSource("slave")
    public static class MixedDataService {
        
        public void useSlaveDataSource() {
            System.out.println("MixedDataService.useSlaveDataSource() - 使用 slave 数据源");
        }
        
        @DataSource("audit")
        public void useAuditDataSource() {
            System.out.println("MixedDataService.useAuditDataSource() - 使用 audit 数据源");
        }
        
        @DataSource("user_db")
        public void useUserDatabase() {
            System.out.println("MixedDataService.useUserDatabase() - 使用 user_db 数据源");
        }
    }
}
