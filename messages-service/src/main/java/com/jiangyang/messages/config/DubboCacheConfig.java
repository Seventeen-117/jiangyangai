package com.jiangyang.messages.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

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
            // 禁用Dubbo文件缓存
            System.setProperty("dubbo.cache.file.enabled", "false");
            System.setProperty("dubbo.metadata.cache.file.enabled", "false");
            System.setProperty("dubbo.registry.cache.file.enabled", "false");
            System.setProperty("dubbo.service.name.mapping.enabled", "false");
            System.setProperty("dubbo.mapping.cache.file.enabled", "false");
            System.setProperty("dubbo.metadata.mapping.cache.file.enabled", "false");
            
            // 为当前应用设置独立的缓存目录，避免与其他服务冲突
            String tempDir = System.getProperty("java.io.tmpdir");
            String appCacheDir = tempDir + File.separator + "dubbo" + File.separator + "messages-service";
            File cacheDirectory = new File(appCacheDir);
            if (!cacheDirectory.exists()) {
                // 尽量创建目录，避免后续文件锁定发生在用户主目录
                cacheDirectory.mkdirs();
            }
            System.setProperty("dubbo.cache.file.directory", appCacheDir);
            
            // 禁用服务发现缓存
            System.setProperty("dubbo.registry.use-as-config-center", "false");
            System.setProperty("dubbo.registry.use-as-metadata-center", "false");
            
            log.info("已设置Dubbo文件缓存禁用属性");
        } catch (Exception e) {
            log.error("设置Dubbo缓存属性时发生错误: {}", e.getMessage(), e);
        }
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            // 清理可能存在的Dubbo缓存文件（仅清理当前应用目录下）
            cleanDubboCache();
        } catch (Exception e) {
            log.warn("配置Dubbo缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 清理Dubbo缓存文件
     */
    private void cleanDubboCache() {
        try {
            // 仅清理当前应用设置的缓存目录
            String configuredDir = System.getProperty("dubbo.cache.file.directory");
            if (configuredDir == null || configuredDir.isEmpty()) {
                configuredDir = System.getProperty("java.io.tmpdir") + File.separator + "dubbo" + File.separator + "messages-service";
            }
            Path dubboCachePath = Paths.get(configuredDir);
            
            if (Files.exists(dubboCachePath)) {
                log.info("发现Dubbo缓存目录: {}", dubboCachePath);
                
                // 删除可能冲突的缓存文件
                File dubboDir = dubboCachePath.toFile();
                File[] files = dubboDir.listFiles();
                
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().contains(".mapping.") ||
                            file.getName().contains(".metadata.")) {
                            log.info("删除冲突的缓存文件: {}", file.getName());
                            if (!file.delete()) {
                                log.warn("无法删除文件: {}", file.getAbsolutePath());
                            }
                        }
                    }
                }
                
                log.info("Dubbo缓存清理完成");
            } else {
                log.info("Dubbo缓存目录不存在: {}", dubboCachePath);
            }
            
        } catch (Exception e) {
            log.error("清理Dubbo缓存时发生错误: {}", e.getMessage(), e);
        }
    }
}
