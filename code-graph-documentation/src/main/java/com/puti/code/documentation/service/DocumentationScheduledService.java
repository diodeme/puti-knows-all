package com.puti.code.documentation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 说明书生成定时任务服务
 * 
 * @author diodehe
 */
@Slf4j
@Service
public class DocumentationScheduledService {
    
    @Autowired
    private DocumentationTaskService taskService;
    
    @Autowired
    private DocumentationArchiveService archiveService;
    
    /**
     * 定时清理过期任务
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredTasks() {
        try {
            log.info("开始定时清理过期任务");
            taskService.cleanupExpiredTasks().join();
            log.info("定时清理过期任务完成");
        } catch (Exception e) {
            log.error("定时清理过期任务时发生错误", e);
        }
    }
    
    /**
     * 定时清理过期归档数据
     * 每周日凌晨3点执行
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    public void cleanupExpiredArchives() {
        try {
            log.info("开始定时清理过期归档数据");
            archiveService.cleanupExpiredArchives().join();
            log.info("定时清理过期归档数据完成");
        } catch (Exception e) {
            log.error("定时清理过期归档数据时发生错误", e);
        }
    }
    
    /**
     * 定时统计系统状态
     * 每小时执行一次
     */
    @Scheduled(fixedRate = 3600000) // 1小时 = 3600000毫秒
    public void logSystemStatistics() {
        try {
            int runningTasks = taskService.getRunningTasksCount();
            int completedTasks = taskService.getCompletedTasksCount();
            int failedTasks = taskService.getFailedTasksCount();
            
            log.info("系统状态统计 - 运行中任务: {}, 已完成任务: {}, 失败任务: {}", 
                    runningTasks, completedTasks, failedTasks);
                    
        } catch (Exception e) {
            log.error("统计系统状态时发生错误", e);
        }
    }
}
