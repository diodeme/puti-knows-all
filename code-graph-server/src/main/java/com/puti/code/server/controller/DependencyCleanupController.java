package com.puti.code.server.controller;

import com.puti.code.base.config.AppConfig;
import com.puti.code.repository.graph.GraphStorageRepository;
import com.puti.code.repository.graph.GraphStorageRepositoryFactory;
import com.puti.code.repository.graph.query.GraphQueryNode;
import com.puti.code.repository.graph.query.GraphQueryRepository;
import com.puti.code.repository.graph.query.GraphQueryRepositoryFactory;
import com.puti.code.repository.milvus.GraphVectorMilvusClient;
import com.puti.code.repository.tracker.DependencyTracker;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dependency")
@CrossOrigin(origins = "*")
@Slf4j
public class DependencyCleanupController {

    private static final int MAX_GAVS_PER_RUN = 50;

    @PostMapping("/cleanup")
    public ResponseEntity<?> cleanup(@RequestBody CleanupRequest request) {
        AppConfig config = AppConfig.getInstance();
        int protectionDays = request.getProtectionDays() > 0 ? request.getProtectionDays() : config.getTrackerOrphanProtectionDays();
        boolean dryRun = request.isDryRun();

        try (DependencyTracker tracker = new DependencyTracker()) {
            List<String> orphanedGavs = tracker.findOrphanedLibraryGraphs(protectionDays);
            int limit = Math.min(orphanedGavs.size(), MAX_GAVS_PER_RUN);
            List<String> targetGavs = orphanedGavs.subList(0, limit);

            if (dryRun) {
                return ResponseEntity.ok(Map.of(
                        "dry_run", true,
                        "orphaned_gavs", targetGavs,
                        "cleaned", 0,
                        "failed", 0
                ));
            }

            int cleaned = 0, failed = 0;
            try (GraphQueryRepository queryRepo = GraphQueryRepositoryFactory.create(config);
                 GraphStorageRepository storageRepo = GraphStorageRepositoryFactory.create(config);
                 GraphVectorMilvusClient vectorClient = new GraphVectorMilvusClient()) {

                for (String gav : targetGavs) {
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
                        tracker.deleteLibraryGraph(gav);
                        cleaned++;
                        log.info("[GC] Cleaned orphaned dependency: {} ({} nodes)", gav, nodeIds.size());
                    } catch (Exception e) {
                        failed++;
                        log.error("[GC] Failed to clean orphaned dependency: {}", gav, e);
                    }
                }
            }

            return ResponseEntity.ok(Map.of(
                    "dry_run", false,
                    "orphaned_gavs", targetGavs,
                    "cleaned", cleaned,
                    "failed", failed
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @Data
    public static class CleanupRequest {
        private int protectionDays;
        private boolean dryRun;
    }
}
