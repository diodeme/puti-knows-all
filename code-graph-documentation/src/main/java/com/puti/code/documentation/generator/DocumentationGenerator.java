package com.puti.code.documentation.generator;

import com.puti.code.base.entity.rdb.Documentation;
import com.puti.code.base.entity.rdb.DocumentationTask;
import com.puti.code.base.model.MethodInfo;
import com.puti.code.documentation.dto.DocumentationGenerationDto;
import com.puti.code.documentation.repository.graph.EntryPointRepository;
import com.puti.code.documentation.service.DocumentationArchiveService;
import com.puti.code.documentation.service.DocumentationTaskService;
import com.puti.code.documentation.service.ProgressiveDocumentationService;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 文档生成器主类
 * 整个说明书生成系统的入口
 *
 * @author diodehe
 */
@Slf4j
@Component
public class DocumentationGenerator {

    // 服务组件
    private final EntryPointRepository entryPointRepository;
    private final DocumentationArchiveService archiveService;
    private final DocumentationTaskService taskService;
    private final ProgressiveDocumentationService progressiveService;

    @Autowired
    public DocumentationGenerator(
            EntryPointRepository entryPointRepository,
            DocumentationArchiveService archiveService,
            DocumentationTaskService taskService,
            ProgressiveDocumentationService progressiveService) {

        this.entryPointRepository = entryPointRepository;
        this.archiveService = archiveService;
        this.taskService = taskService;
        this.progressiveService = progressiveService;

        log.info("文档生成器初始化完成");
    }

    /**
     * 为所有入口点生成说明书
     *
     * @param dto 请求体
     * @return 生成任务的Future列表
     */
    @Async("codeDocumentTaskExecutor")
    public CompletableFuture<List<String>> generateDocumentationForAllEntryPoints(DocumentationGenerationDto dto) {
        try {
            Integer level = dto.getLevel();
            log.info("开始为所有入口点生成 {} 层级的说明书", level);

            // 1. 获取所有入口点
            List<MethodInfo> entryPoints = entryPointRepository.getAllEntryPoints(dto);
            log.info("找到 {} 个入口点", entryPoints.size());

            if (entryPoints.isEmpty()) {
                log.warn("没有找到任何入口点");
                return CompletableFuture.completedFuture(List.of());
            }

            // 2. 为每个入口点启动生成任务
            List<CompletableFuture<String>> futures = entryPoints.stream()
                    .map(entryPoint -> generateDocumentationForEntryPoint(dto, entryPoint.getMethodId()))
                    .toList();

            // 3. 等待所有任务完成
            CompletableFuture<Void> allTasks = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0]));

            return allTasks.thenApply(v ->
                    futures.stream()
                            .map(CompletableFuture::join)
                            .toList()
            );

        } catch (Exception e) {
            log.error("为所有入口点生成说明书时发生错误", e);
            return CompletableFuture.failedFuture(new RuntimeException("批量生成失败", e));
        }
    }

    /**
     * 为指定入口点生成说明书
     *
     * @param dto 请求体
     * @return 任务ID
     */
    @Async("codeDocumentTaskExecutor")
    public CompletableFuture<String> generateDocumentationForEntryPoint(DocumentationGenerationDto dto, String entryPointId) {
        Integer level = dto.getLevel();
        try {
            log.info("开始为入口点 {} 生成 {} 层级的说明书", entryPointId, level);

            // 检查入口点是否存在
            MethodInfo entryPoint = entryPointRepository.getEntryPointById(entryPointId);
            if (entryPoint == null) {
                throw new IllegalArgumentException("入口点不存在: " + entryPointId);
            }

            // 检查是否已有正在进行的任务
            DocumentationTask existingTask = taskService.getRunningTaskForEntryPoint(entryPointId);
            if (existingTask != null) {
                log.warn("入口点 {} 已有正在进行的生成任务: {}", entryPointId, existingTask.getId());
                return CompletableFuture.completedFuture(existingTask.getId());
            }

            // 启动渐进式生成
            return progressiveService.startProgressiveGeneration(dto, entryPoint);

        } catch (Exception e) {
            log.error("为入口点 {} 生成说明书时发生错误", entryPointId, e);
            return CompletableFuture.failedFuture(new RuntimeException("生成失败", e));
        }
    }

    /**
     * 获取生成任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态
     */
    public DocumentationTask getTaskStatus(String taskId) {
        return taskService.getTaskById(taskId);
    }

    /**
     * 获取入口点的说明书
     *
     * @param entryPointId 入口点ID
     * @param level        层级（可选，不指定则返回最高层级）
     * @return 说明书
     */
    public Documentation getDocumentation(String entryPointId, Integer level) {
        // TODO: 实现获取说明书的逻辑
        throw new UnsupportedOperationException("需要实现数据访问层");
    }

    /**
     * 取消生成任务
     *
     * @param taskId 任务ID
     * @return 是否成功取消
     */
    public boolean cancelTask(String taskId) {
        try {
            return taskService.cancelTask(taskId);
        } catch (Exception e) {
            log.error("取消任务 {} 时发生错误", taskId, e);
            return false;
        }
    }

    /**
     * 清理过期数据
     */
    @Async("codeDocumentTaskExecutor")
    public CompletableFuture<Void> cleanupExpiredData() {
        try {
            log.info("开始清理过期数据");

            // 清理过期的归档数据
            archiveService.cleanupExpiredArchives().join();

            // 清理过期的任务
            taskService.cleanupExpiredTasks().join();

            log.info("过期数据清理完成");
            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("清理过期数据时发生错误", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 获取系统统计信息
     */
    public DocumentationStatistics getStatistics() {
        try {
            return DocumentationStatistics.builder()
                    .totalEntryPoints(entryPointRepository.getTotalEntryPointsCount())
                    .totalDocumentations(getDocumentationCount())
                    .runningTasks(taskService.getRunningTasksCount())
                    .completedTasks(taskService.getCompletedTasksCount())
                    .failedTasks(taskService.getFailedTasksCount())
                    .build();
        } catch (Exception e) {
            log.error("获取统计信息时发生错误", e);
            return DocumentationStatistics.builder().build();
        }
    }

    private int getDocumentationCount() {
        // TODO: 实现获取文档总数的逻辑
        return 0;
    }

    @PreDestroy
    public void destroy() {
        try {
            log.info("正在关闭文档生成器...");

            // Spring会自动管理线程池和其他Bean的生命周期
            // 这里只需要做一些清理工作

            log.info("文档生成器已关闭");

        } catch (Exception e) {
            log.error("关闭文档生成器时发生错误", e);
        }
    }

    /**
     * 统计信息DTO
     */
    @Getter
    public static class DocumentationStatistics {
        // Getters
        private int totalEntryPoints;
        private int totalDocumentations;
        private int runningTasks;
        private int completedTasks;
        private int failedTasks;

        public static DocumentationStatisticsBuilder builder() {
            return new DocumentationStatisticsBuilder();
        }

        // Builder and getters implementation
        public static class DocumentationStatisticsBuilder {
            private int totalEntryPoints;
            private int totalDocumentations;
            private int runningTasks;
            private int completedTasks;
            private int failedTasks;

            public DocumentationStatisticsBuilder totalEntryPoints(int totalEntryPoints) {
                this.totalEntryPoints = totalEntryPoints;
                return this;
            }

            public DocumentationStatisticsBuilder totalDocumentations(int totalDocumentations) {
                this.totalDocumentations = totalDocumentations;
                return this;
            }

            public DocumentationStatisticsBuilder runningTasks(int runningTasks) {
                this.runningTasks = runningTasks;
                return this;
            }

            public DocumentationStatisticsBuilder completedTasks(int completedTasks) {
                this.completedTasks = completedTasks;
                return this;
            }

            public DocumentationStatisticsBuilder failedTasks(int failedTasks) {
                this.failedTasks = failedTasks;
                return this;
            }

            public DocumentationStatistics build() {
                DocumentationStatistics stats = new DocumentationStatistics();
                stats.totalEntryPoints = this.totalEntryPoints;
                stats.totalDocumentations = this.totalDocumentations;
                stats.runningTasks = this.runningTasks;
                stats.completedTasks = this.completedTasks;
                stats.failedTasks = this.failedTasks;
                return stats;
            }
        }

    }
}
