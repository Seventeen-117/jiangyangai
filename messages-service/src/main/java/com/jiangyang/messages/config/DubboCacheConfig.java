package com.jiangyang.messages.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Dubbo缓存配置
 * 解决多实例启动时的文件锁冲突问题
 */
@Slf4j
@Component
public class DubboCacheConfig implements CommandLineRunner {

    // 静态代码块，在类加载时就设置系统属性
    static {
        try {
            // 禁用Dubbo文件缓存 - 核心配置
            System.setProperty("dubbo.cache.file.enabled", "false");
            System.setProperty("dubbo.metadata.cache.file.enabled", "false");
            System.setProperty("dubbo.registry.cache.file.enabled", "false");
            System.setProperty("dubbo.service.name.mapping.enabled", "false");
            System.setProperty("dubbo.mapping.cache.file.enabled", "false");
            System.setProperty("dubbo.metadata.mapping.cache.file.enabled", "false");
            
            // 禁用Dubbo服务发现缓存
            System.setProperty("dubbo.registry.use-as-config-center", "false");
            System.setProperty("dubbo.registry.use-as-metadata-center", "false");
            
            // 禁用Dubbo监控和追踪
            System.setProperty("dubbo.monitor.enabled", "false");
            System.setProperty("dubbo.tracing.enabled", "false");
            System.setProperty("dubbo.observability.enabled", "false");
            
            // 设置Dubbo元数据类型为本地
            System.setProperty("dubbo.metadata.type", "local");
            
            // 禁用Dubbo配置中心文件缓存
            System.setProperty("dubbo.config-center.file", "false");
            
            // 为当前应用设置独立的缓存目录，避免与其他服务冲突
            String tempDir = System.getProperty("java.io.tmpdir");
            String appCacheDir = tempDir + File.separator + "dubbo" + File.separator + "messages-service";
            File cacheDirectory = new File(appCacheDir);
            if (!cacheDirectory.exists()) {
                // 尽量创建目录，避免后续文件锁定发生在用户主目录
                cacheDirectory.mkdirs();
            }
            System.setProperty("dubbo.cache.file.directory", appCacheDir);
            
            // 设置Dubbo应用环境变量，确保唯一性
            System.setProperty("dubbo.application.environment", "dev");
            System.setProperty("dubbo.application.qos.enable", "false");
            
            log.info("已设置Dubbo文件缓存禁用属性");
        } catch (Exception e) {
            log.error("设置Dubbo缓存属性时发生错误: {}", e.getMessage(), e);
        }
    }

    @PostConstruct
    public void init() {
        try {
            // 在Bean初始化后再次确保系统属性设置
            setDubboSystemProperties();
            
            // 清理可能存在的Dubbo缓存文件
            cleanDubboCache();
            
            log.info("Dubbo缓存配置初始化完成");
        } catch (Exception e) {
            log.error("Dubbo缓存配置初始化失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 设置Dubbo系统属性
     */
    private void setDubboSystemProperties() {
        try {
            // 再次确保所有必要的系统属性都已设置
            System.setProperty("dubbo.cache.file.enabled", "false");
            System.setProperty("dubbo.metadata.cache.file.enabled", "false");
            System.setProperty("dubbo.registry.cache.file.enabled", "false");
            System.setProperty("dubbo.service.name.mapping.enabled", "false");
            System.setProperty("dubbo.mapping.cache.file.enabled", "false");
            System.setProperty("dubbo.metadata.mapping.cache.file.enabled", "false");
            
            // 设置Dubbo应用名称，确保唯一性
            String appName = "messages-service-" + System.currentTimeMillis();
            System.setProperty("dubbo.application.name", appName);
            
            log.info("Dubbo系统属性设置完成，应用名称: {}", appName);
        } catch (Exception e) {
            log.error("设置Dubbo系统属性时发生错误: {}", e.getMessage(), e);
        }
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            // 在应用启动完成后再次清理缓存
            cleanDubboCache();
            log.info("Dubbo缓存配置启动完成");
        } catch (Exception e) {
            log.warn("配置Dubbo缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 清理Dubbo缓存文件
     */
    private void cleanDubboCache() {
        try {
            // 清理用户主目录下的Dubbo缓存
            String userHome = System.getProperty("user.home");
            String userDubboCache = userHome + File.separator + ".dubbo";
            cleanDirectory(userDubboCache, "用户主目录");
            
            // 清理临时目录下的Dubbo缓存
            String tempDir = System.getProperty("java.io.tmpdir");
            String tempDubboCache = tempDir + File.separator + "dubbo";
            cleanDirectory(tempDubboCache, "临时目录");
            
            // 清理当前应用设置的缓存目录
            String configuredDir = System.getProperty("dubbo.cache.file.directory");
            if (configuredDir != null && !configuredDir.isEmpty()) {
                cleanDirectory(configuredDir, "配置目录");
            }
            
            log.info("Dubbo缓存清理完成");
        } catch (Exception e) {
            log.error("清理Dubbo缓存时发生错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 清理指定目录下的Dubbo缓存文件
     */
    private void cleanDirectory(String directoryPath, String directoryType) {
        try {
            Path dubboCachePath = Paths.get(directoryPath);
            
            if (Files.exists(dubboCachePath)) {
                log.info("发现{}下的Dubbo缓存目录: {}", directoryType, dubboCachePath);
                
                // 删除可能冲突的缓存文件
                File dubboDir = dubboCachePath.toFile();
                File[] files = dubboDir.listFiles();
                
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().contains(".mapping.") ||
                            file.getName().contains(".metadata.") ||
                            file.getName().contains(".discovery.") ||
                            file.getName().contains(".registry.") ||
                            file.getName().contains("bgai-service") ||  // 特别关注bgai-service的缓存
                            file.getName().contains("messages-service")) {
                            
                            log.info("删除冲突的缓存文件: {} from {}", file.getName(), directoryType);
                            if (!file.delete()) {
                                log.warn("无法删除文件: {}", file.getAbsolutePath());
                            }
                        }
                    }
                }
            } else {
                log.debug("{}下的Dubbo缓存目录不存在: {}", directoryType, dubboCachePath);
            }
            
        } catch (Exception e) {
            log.warn("清理{}下的Dubbo缓存时发生错误: {}", directoryType, e.getMessage());
        }
    }
}
