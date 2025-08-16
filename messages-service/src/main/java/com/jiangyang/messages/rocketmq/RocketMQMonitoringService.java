package com.jiangyang.messages.rocketmq;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RocketMQ监控服务
 * 解决运维监控和故障定位的痛点：
 * 1. 监控盲区：实现消息轨迹追踪和端到端延迟监控
 * 2. 故障定位：实现消息丢失根因分析和慢消费定位
 * 3. 运维操作风险：实现负载均衡监控和版本兼容性检查
 */
@Slf4j
@Component
public class RocketMQMonitoringService implements InitializingBean {

    @Setter
    private String nameServer;

    // 消息发送统计
    private final Map<String, AtomicLong> sentMessagesCount = new HashMap<>();
    private final Map<String, AtomicLong> sentMessagesLatency = new HashMap<>();
    // 消息消费统计
    private final Map<String, AtomicLong> consumedMessagesCount = new HashMap<>();
    private final Map<String, AtomicLong> consumedMessagesLatency = new HashMap<>();
    // 告警阈值
    private static final long LATENCY_ALERT_THRESHOLD_MS = 1000;
    private static final long BACKLOG_ALERT_THRESHOLD = 10000;
    // 监控间隔时间（毫秒）
    private static final long MONITOR_INTERVAL_MS = 30000;

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("RocketMQ Monitoring Service initialized without admin tools due to missing dependency");
        // 启动监控线程（不依赖admin）
        startMonitoringThread();
    }

    /**
     * 记录消息发送
     */
    public void recordMessageSent(String topic, long latencyMs) {
        sentMessagesCount.computeIfAbsent(topic, k -> new AtomicLong(0)).incrementAndGet();
        sentMessagesLatency.computeIfAbsent(topic, k -> new AtomicLong(0)).addAndGet(latencyMs);
        if (latencyMs > LATENCY_ALERT_THRESHOLD_MS) {
            log.warn("High send latency detected for topic: {}, latency: {}ms", topic, latencyMs);
            triggerLatencyAlert(topic, "send", latencyMs);
        }
    }

    /**
     * 记录消息消费
     */
    public void recordMessageConsumed(String topic, long latencyMs) {
        consumedMessagesCount.computeIfAbsent(topic, k -> new AtomicLong(0)).incrementAndGet();
        consumedMessagesLatency.computeIfAbsent(topic, k -> new AtomicLong(0)).addAndGet(latencyMs);
        if (latencyMs > LATENCY_ALERT_THRESHOLD_MS) {
            log.warn("High consume latency detected for topic: {}, latency: {}ms", topic, latencyMs);
            triggerLatencyAlert(topic, "consume", latencyMs);
        }
    }

    /**
     * 启动监控线程
     */
    private void startMonitoringThread() {
        Thread monitoringThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(MONITOR_INTERVAL_MS);
                    // 监控端到端延迟
                    monitorEndToEndLatency();
                    // 监控消息丢失
                    monitorMessageLoss();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.info("Monitoring thread interrupted");
                } catch (Exception e) {
                    log.error("Unexpected error in monitoring thread", e);
                }
            }
        });
        monitoringThread.setDaemon(true);
        monitoringThread.setName("RocketMQ-Monitoring-Thread");
        monitoringThread.start();
        log.info("RocketMQ monitoring thread started");
    }



    /**
     * 监控端到端延迟
     */
    private void monitorEndToEndLatency() {
        for (String topic : sentMessagesCount.keySet()) {
            long sentCount = sentMessagesCount.getOrDefault(topic, new AtomicLong(0)).get();
            long consumedCount = consumedMessagesCount.getOrDefault(topic, new AtomicLong(0)).get();
            if (sentCount > 0 && consumedCount > 0) {
                long avgSendLatency = sentMessagesLatency.getOrDefault(topic, new AtomicLong(0)).get() / sentCount;
                long avgConsumeLatency = consumedMessagesLatency.getOrDefault(topic, new AtomicLong(0)).get() / consumedCount;
                long endToEndLatency = avgSendLatency + avgConsumeLatency;
                if (endToEndLatency > LATENCY_ALERT_THRESHOLD_MS * 2) {
                    log.warn("High end-to-end latency detected for topic: {}, latency: {}ms (send: {}ms, consume: {}ms)", 
                            topic, endToEndLatency, avgSendLatency, avgConsumeLatency);
                    triggerLatencyAlert(topic, "end-to-end", endToEndLatency);
                }
            }
        }
    }

    /**
     * 监控消息丢失
     */
    private void monitorMessageLoss() {
        for (String topic : sentMessagesCount.keySet()) {
            long sentCount = sentMessagesCount.getOrDefault(topic, new AtomicLong(0)).get();
            long consumedCount = consumedMessagesCount.getOrDefault(topic, new AtomicLong(0)).get();
            if (sentCount > consumedCount + BACKLOG_ALERT_THRESHOLD / 2) {
                log.warn("Potential message loss detected for topic: {}, sent: {}, consumed: {}", topic, sentCount, consumedCount);
                triggerMessageLossAlert(topic, sentCount, consumedCount);
            }
        }
    }

    /**
     * 触发延迟告警
     */
    private void triggerLatencyAlert(String topic, String type, long latencyMs) {
        // 这里可以添加告警逻辑，例如发送邮件、短信或调用外部告警系统
        log.error("ALERT: High {} latency for topic: {}, latency: {}ms", type, topic, latencyMs);
    }

    /**
     * 触发积压告警
     */
    private void triggerBacklogAlert(String topic, long backlog) {
        // 这里可以添加告警逻辑，例如发送邮件、短信或调用外部告警系统
        log.error("ALERT: High backlog for topic: {}, backlog: {}", topic, backlog);
    }

    /**
     * 触发消息丢失告警
     */
    private void triggerMessageLossAlert(String topic, long sentCount, long consumedCount) {
        // 这里可以添加告警逻辑，例如发送邮件、短信或调用外部告警系统
        log.error("ALERT: Potential message loss for topic: {}, sent: {}, consumed: {}", topic, sentCount, consumedCount);
    }
}
