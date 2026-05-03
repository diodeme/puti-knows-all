package com.puti.code.app.dependency;

import com.puti.code.base.config.AppConfig;
import com.puti.code.repository.graph.GraphStorageRepository;
import com.puti.code.repository.graph.GraphStorageRepositoryFactory;
import com.puti.code.repository.graph.query.GraphQueryNode;
import com.puti.code.repository.graph.query.GraphQueryRepository;
import com.puti.code.repository.graph.query.GraphQueryRepositoryFactory;
import com.puti.code.repository.milvus.GraphVectorMilvusClient;
import com.puti.code.repository.tracker.DependencyTracker;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 孤立依赖清理器：清理无项目引用的依赖图谱。
 * 由定时任务或 API 触发。
 */
@Slf4j
public class DependencyGarbageCollector {

    private static final int MAX_GAVS_PER_RUN = 50;

    private final DependencyTracker dependencyTracker;
    private final int orphanProtectionDays;
    private final boolean dryRun;

    public DependencyGarbageCollector(DependencyTracker dependencyTracker, int orphanProtectionDays, boolean dryRun) {
        this.dependencyTracker = dependencyTracker;
        this.orphanProtectionDays = orphanProtectionDays;
        this.dryRun = dryRun;
    }

    public CleanupResult cleanup() {
        List<String> orphanedGavs = dependencyTracker.findOrphanedLibraryGraphs(orphanProtectionDays);
        int limit = Math.min(orphanedGavs.size(), MAX_GAVS_PER_RUN);
        log.info("[GC] Found {} orphaned dependency graphs (processing up to {}, dryRun={})",
                orphanedGavs.size(), limit, dryRun);

        if (dryRun) {
            return new CleanupResult(orphanedGavs.subList(0, limit), 0, 0);
        }

        AppConfig appConfig = AppConfig.getInstance();
        int cleaned = 0;
        int failed = 0;

        try (GraphQueryRepository queryRepo = GraphQueryRepositoryFactory.create(appConfig);
             GraphStorageRepository storageRepo = GraphStorageRepositoryFactory.create(appConfig);
             GraphVectorMilvusClient vectorClient = new GraphVectorMilvusClient()) {

            for (int i = 0; i < limit; i++) {
                String gav = orphanedGavs.get(i);
                String repoId = "lib:" + gav;
                try {
                    List<String> nodeIds = new ArrayList<>();
                    for (GraphQueryNode node : queryRepo.findNodesByRepoId(repoId)) {
                        nodeIds.add(node.getId());
                    }
                    if (!nodeIds.isEmpty()) {
                        storageRepo.deleteNodes(nodeIds);
                    }

                    vectorClient.deleteByRepoId(repoId);
                    dependencyTracker.deleteLibraryGraph(gav);

                    // 级联清理 class_index 条目
                    dependencyTracker.deleteClassIndexByGav(List.of(gav));

                    cleaned++;
                    log.info("[GC] Cleaned orphaned dependency: {} ({} nodes)", gav, nodeIds.size());
                } catch (Exception e) {
                    failed++;
                    log.error("[GC] Failed to clean orphaned dependency: {}", gav, e);
                }
            }
        } catch (Exception e) {
            log.error("[GC] Failed to initialize resources for cleanup", e);
        }

        return new CleanupResult(orphanedGavs.subList(0, limit), cleaned, failed);
    }

    public record CleanupResult(List<String> orphanedGavs, int cleaned, int failed) {}
}
