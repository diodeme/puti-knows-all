package com.puti.code.app.dependency;

import com.puti.code.base.config.AppConfig;
import com.puti.code.repository.tracker.DependencyTracker;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 孤立依赖清理定时任务调度器。
 * 使用 ScheduledExecutorService 实现简单的定时调度。
 */
@Slf4j
public class DependencyCleanupScheduler {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "dependency-cleanup");
        t.setDaemon(true);
        return t;
    });

    /**
     * 启动定时清理任务。
     * 默认每天凌晨 3 点执行（通过 cron 表达式配置）。
     * 简化实现：使用固定 24 小时间隔。
     */
    public void start() {
        AppConfig config = AppConfig.getInstance();
        long intervalHours = 24;
        // 初始延迟 1 小时，避免启动时立即执行
        long initialDelayHours = 1;

        scheduler.scheduleAtFixedRate(this::runCleanup,
                initialDelayHours, intervalHours, TimeUnit.HOURS);
        log.info("[CleanupScheduler] Scheduled dependency cleanup every {}h (initial delay: {}h)",
                intervalHours, initialDelayHours);
    }

    private void runCleanup() {
        try {
            int protectionDays = AppConfig.getInstance().getTrackerOrphanProtectionDays();
            log.info("[CleanupScheduler] Starting scheduled dependency cleanup (protectionDays={})", protectionDays);

            try (DependencyTracker tracker = new DependencyTracker()) {
                DependencyGarbageCollector collector = new DependencyGarbageCollector(tracker, protectionDays, false);
                DependencyGarbageCollector.CleanupResult result = collector.cleanup();
                log.info("[CleanupScheduler] Cleanup completed: cleaned={}, failed={}", result.cleaned(), result.failed());
            }
        } catch (Exception e) {
            log.error("[CleanupScheduler] Scheduled cleanup failed", e);
        }
    }

    public void stop() {
        scheduler.shutdown();
        log.info("[CleanupScheduler] Stopped");
    }
}
