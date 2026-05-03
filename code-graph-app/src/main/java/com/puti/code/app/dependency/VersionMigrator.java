package com.puti.code.app.dependency;

import com.puti.code.base.config.AppConfig;
import com.puti.code.base.model.Edge;
import com.puti.code.base.model.EdgeType;
import com.puti.code.repository.graph.GraphStorageRepository;
import com.puti.code.repository.graph.GraphStorageRepositoryFactory;
import com.puti.code.repository.graph.query.GraphQueryEdge;
import com.puti.code.repository.graph.query.GraphQueryNode;
import com.puti.code.repository.graph.query.GraphQueryRepository;
import com.puti.code.repository.graph.query.GraphQueryRepositoryFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 版本迁移器：处理依赖版本升级时的边迁移。
 * 根据项目源码是否变更，选择增量迁移或全量重建策略。
 */
@Slf4j
public class VersionMigrator {

    public MigrationResult migrate(String projectId, String branch,
                                    Map<String, String> changedGavs,
                                    boolean projectSourceChanged) {
        if (changedGavs == null || changedGavs.isEmpty()) {
            return new MigrationResult(0, 0, "none");
        }

        log.info("[Migration] Starting migration: {} GAVs changed, projectSourceChanged={}",
                changedGavs.size(), projectSourceChanged);

        // 新版本库图谱已在 ProjectHandler.resolveDependencies() 中通过 LibraryHandler 处理

        if (projectSourceChanged) {
            return fullRebuild(projectId, branch, changedGavs);
        } else {
            return incrementalMigrate(projectId, branch, changedGavs);
        }
    }

    /**
     * 增量迁移：项目源码未变更，按 full_name 重定向边。
     */
    private MigrationResult incrementalMigrate(String projectId, String branch,
                                                Map<String, String> changedGavs) {
        AppConfig appConfig = AppConfig.getInstance();
        int totalMigrated = 0, totalRemoved = 0;

        try (GraphQueryRepository queryRepo = GraphQueryRepositoryFactory.create(appConfig);
             GraphStorageRepository storageRepo = GraphStorageRepositoryFactory.create(appConfig)) {

            for (Map.Entry<String, String> entry : changedGavs.entrySet()) {
                String oldGav = entry.getKey();
                String newGav = entry.getValue();
                String oldRepoId = "lib:" + oldGav;
                String newRepoId = "lib:" + newGav;

                Map<String, String> newNodeIds = queryNodeFullNamesByRepoId(queryRepo, newRepoId);
                Set<String> changedMethods = findChangedMethods(queryRepo, oldRepoId, newRepoId);

                // 查询旧版本库的入边（项目源码→旧库）
                List<GraphQueryEdge> edgesToMigrate = findEdgesToLib(queryRepo, oldRepoId);

                int migrated = 0, removed = 0;
                for (GraphQueryEdge edge : edgesToMigrate) {
                    String dstNodeId = edge.getTarget();
                    Optional<GraphQueryNode> dstNode = queryRepo.findNodeById(dstNodeId);
                    if (dstNode.isEmpty()) continue;

                    String dstFullName = (String) dstNode.get().getProperties().get("full_name");
                    if (dstFullName == null) continue;

                    String newNodeId = newNodeIds.get(dstFullName);
                    if (newNodeId != null) {
                        Edge newEdge = Edge.builder()
                                .srcId(edge.getSource())
                                .dstId(newNodeId)
                                .type(EdgeType.CALLS)
                                .property("migrated_from", oldGav)
                                .build();
                        storageRepo.insertEdge(newEdge);
                        migrated++;

                        if (changedMethods.contains(dstFullName)) {
                            log.warn("[Migration] API compatibility risk: {} changed between {} and {}",
                                    dstFullName, oldGav, newGav);
                        }
                    } else {
                        log.info("[Migration] Method removed in new version: {} ({} → {})", dstFullName, oldGav, newGav);
                        removed++;
                    }
                }

                // 删除旧边
                deleteEdgesToLib(storageRepo, queryRepo, oldRepoId);

                totalMigrated += migrated;
                totalRemoved += removed;
                log.info("[Migration] Migrated {} edges, removed {} for {} → {}", migrated, removed, oldGav, newGav);
            }
        } catch (Exception e) {
            log.error("[Migration] Incremental migration failed", e);
        }

        return new MigrationResult(totalMigrated, totalRemoved, "incremental");
    }

    /**
     * 全量重建：项目源码有变更，删除旧边后重跑 Phase 3。
     */
    private MigrationResult fullRebuild(String projectId, String branch,
                                         Map<String, String> changedGavs) {
        AppConfig appConfig = AppConfig.getInstance();

        try (GraphQueryRepository queryRepo = GraphQueryRepositoryFactory.create(appConfig);
             GraphStorageRepository storageRepo = GraphStorageRepositoryFactory.create(appConfig)) {

            for (String oldGav : changedGavs.keySet()) {
                String oldRepoId = "lib:" + oldGav;
                deleteEdgesToLib(storageRepo, queryRepo, oldRepoId);
            }

            log.info("[Migration] Full rebuild: old edges deleted, Phase 3 will regenerate edges");
        } catch (Exception e) {
            log.error("[Migration] Full rebuild failed", e);
        }

        return new MigrationResult(0, 0, "full_rebuild");
    }

    private Map<String, String> queryNodeFullNamesByRepoId(GraphQueryRepository queryRepo, String repoId) {
        Map<String, String> result = new HashMap<>();
        try {
            for (GraphQueryNode node : queryRepo.findNodesByRepoId(repoId)) {
                Object fullName = node.getProperties().get("full_name");
                if (fullName != null) {
                    result.put(String.valueOf(fullName), node.getId());
                }
            }
        } catch (Exception e) {
            log.error("[Migration] Failed to query nodes by repoId: {}", repoId, e);
        }
        return result;
    }

    private Set<String> findChangedMethods(GraphQueryRepository queryRepo, String oldRepoId, String newRepoId) {
        Set<String> changed = new HashSet<>();
        try {
            Map<String, String> oldHashes = new HashMap<>();
            for (GraphQueryNode node : queryRepo.findNodesByRepoId(oldRepoId)) {
                Object fullName = node.getProperties().get("full_name");
                Object sourceHash = node.getProperties().get("source_hash");
                if (fullName != null && sourceHash != null) {
                    oldHashes.put(String.valueOf(fullName), String.valueOf(sourceHash));
                }
            }
            for (GraphQueryNode node : queryRepo.findNodesByRepoId(newRepoId)) {
                Object fullName = node.getProperties().get("full_name");
                Object sourceHash = node.getProperties().get("source_hash");
                if (fullName != null && sourceHash != null) {
                    String oldHash = oldHashes.get(String.valueOf(fullName));
                    if (oldHash != null && !oldHash.equals(String.valueOf(sourceHash))) {
                        changed.add(String.valueOf(fullName));
                    }
                }
            }
        } catch (Exception e) {
            log.error("[Migration] Failed to compare sourceHash", e);
        }
        return changed;
    }

    /**
     * 查找指向指定 lib repoId 的所有入边。
     */
    private List<GraphQueryEdge> findEdgesToLib(GraphQueryRepository queryRepo, String libRepoId) {
        List<GraphQueryEdge> edges = new ArrayList<>();
        try {
            List<GraphQueryNode> libNodes = queryRepo.findNodesByRepoId(libRepoId);
            for (GraphQueryNode node : libNodes) {
                var subgraph = queryRepo.getSubgraph(node.getId(), 1,
                        com.puti.code.repository.graph.query.GraphDirection.IN, null);
                for (GraphQueryEdge edge : subgraph.getEdges()) {
                    // 只保留非 lib 来源的入边（即项目源码→lib 的边）
                    Optional<GraphQueryNode> srcNode = queryRepo.findNodeById(edge.getSource());
                    if (srcNode.isPresent()) {
                        Object srcRepoId = srcNode.get().getProperties().get("repo_id");
                        if (srcRepoId != null && !String.valueOf(srcRepoId).startsWith("lib:")) {
                            edges.add(edge);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("[Migration] Failed to find edges to lib: {}", libRepoId, e);
        }
        return edges;
    }

    private void deleteEdgesToLib(GraphStorageRepository storageRepo, GraphQueryRepository queryRepo,
                                   String libRepoId) {
        try {
            List<GraphQueryEdge> edges = findEdgesToLib(queryRepo, libRepoId);
            List<String> srcIds = edges.stream().map(GraphQueryEdge::getSource).toList();
            List<String> dstIds = edges.stream().map(GraphQueryEdge::getTarget).toList();
            for (int i = 0; i < srcIds.size(); i++) {
                String edgeType = edges.get(i).getType();
                if (edgeType != null) {
                    storageRepo.deleteEdges(List.of(srcIds.get(i)), List.of(dstIds.get(i)), edgeType);
                }
            }
            log.info("[Migration] Deleted {} edges to lib: {}", edges.size(), libRepoId);
        } catch (Exception e) {
            log.error("[Migration] Failed to delete edges to lib: {}", libRepoId, e);
        }
    }

    public record MigrationResult(int migratedEdges, int removedEdges, String strategy) {}
}
