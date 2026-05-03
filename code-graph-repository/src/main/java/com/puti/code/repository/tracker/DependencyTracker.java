package com.puti.code.repository.tracker;

import com.puti.code.base.config.AppConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class DependencyTracker implements AutoCloseable {

    private static final int BUILDING_TIMEOUT_MINUTES = 60;
    private static final long WAIT_POLL_INTERVAL_MS = 5000;
    private static final int BATCH_SIZE = 3000;
    private final HikariDataSource dataSource;
    private final Path manifestDir;

    public DependencyTracker() {
        AppConfig config = AppConfig.getInstance();
        this.dataSource = createDataSource(config);
        this.manifestDir = Paths.get(config.getGlobalLibraryPath());
        ensureSchema();
    }

    private HikariDataSource createDataSource(AppConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(ensureRewriteBatchedStatements(config.getTrackerMysqlUrl()));
        hikariConfig.setUsername(config.getTrackerMysqlUsername());
        hikariConfig.setPassword(config.getTrackerMysqlPassword());
        hikariConfig.setMaximumPoolSize(5);
        hikariConfig.setMinimumIdle(1);
        return new HikariDataSource(hikariConfig);
    }

    private static String ensureRewriteBatchedStatements(String jdbcUrl) {
        if (jdbcUrl == null) return jdbcUrl;
        if (jdbcUrl.contains("rewriteBatchedStatements")) return jdbcUrl;
        String separator = jdbcUrl.contains("?") ? "&" : "?";
        return jdbcUrl + separator + "rewriteBatchedStatements=true";
    }

    private void ensureSchema() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'puti_project_info' LIMIT 1");
            if (rs.next()) {
                return;
            }
            log.info("[Tracker] Tables not found, executing schema.sql...");
            var resource = DependencyTracker.class.getClassLoader().getResource("schema.sql");
            if (resource != null) {
                String schemaSql;
                try (var is = resource.openStream()) {
                    schemaSql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
                for (String sql : schemaSql.split(";")) {
                    String trimmed = sql.trim();
                    if (!trimmed.isEmpty()) {
                        stmt.executeUpdate(trimmed);
                    }
                }
                log.info("[Tracker] Schema initialized successfully");
            } else {
                log.warn("[Tracker] schema.sql not found on classpath, skipping auto-init");
            }
        } catch (Exception e) {
            log.error("[Tracker] Failed to initialize schema", e);
        }
    }

    /**
     * 同步项目依赖信息：写入/更新 project_info + project_dependency_info（含 is_filtered）+ library_graph_info
     * 同时双写 dependency-manifest.json
     */
    public void syncDependencies(String projectId, String branch,
                                  Map<String, File> kept, Map<String, File> excluded) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Upsert project_info
                upsertProjectInfo(conn, projectId, branch);

                // 2. Delete old dependencies for this project+branch
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM puti_project_dependency_info WHERE project_id = ? AND branch_name = ?")) {
                    ps.setString(1, projectId);
                    ps.setString(2, branch);
                    ps.executeUpdate();
                }

                // 3. Insert kept dependencies (is_filtered = 0) + register library_graph_info
                for (Map.Entry<String, File> entry : kept.entrySet()) {
                    String gav = entry.getKey();
                    String jarPath = entry.getValue() != null ? entry.getValue().getAbsolutePath() : null;
                    insertDependency(conn, projectId, branch, gav, jarPath, 0);

                    // Register library_graph_info if not exists
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO puti_library_graph_info (gav, graph_status) VALUES (?, 'PENDING') " +
                            "ON DUPLICATE KEY UPDATE graph_status = graph_status")) {
                        ps.setString(1, gav);
                        ps.executeUpdate();
                    }
                }

                // 4. Insert excluded dependencies (is_filtered = 1)
                for (Map.Entry<String, File> entry : excluded.entrySet()) {
                    String gav = entry.getKey();
                    String jarPath = entry.getValue() != null ? entry.getValue().getAbsolutePath() : null;
                    insertDependency(conn, projectId, branch, gav, jarPath, 1);
                }

                conn.commit();
                log.info("[Tracker] Synced {} kept + {} excluded dependencies for project {} branch {}",
                        kept.size(), excluded.size(), projectId, branch);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            log.error("[Tracker] Failed to sync dependencies", e);
        }

        // Dual-write manifest (kept + excluded)
        Map<String, File> allDeps = new LinkedHashMap<>(kept);
        allDeps.putAll(excluded);
        writeManifest(projectId, branch, allDeps);
    }

    /**
     * 兼容旧签名：仅传入 kept 依赖，excluded 为空
     */
    public void syncDependencies(String projectId, String branch, Map<String, File> gavToJar) {
        syncDependencies(projectId, branch, gavToJar, Map.of());
    }

    /**
     * 仅确保项目信息注册到 puti_project_info，不触碰依赖记录。
     * 用于 dependencyAutoResolve=false 时的兜底注册。
     */
    public void ensureProjectRegistered(String projectId, String branch) {
        try (Connection conn = dataSource.getConnection()) {
            upsertProjectInfo(conn, projectId, branch);
            log.info("[Tracker] Ensured project {} branch {} registered", projectId, branch);
        } catch (SQLException e) {
            log.error("[Tracker] Failed to ensure project registered", e);
        }
    }

    private void insertDependency(Connection conn, String projectId, String branch,
                                   String gav, String jarPath, int isFiltered) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO puti_project_dependency_info (project_id, branch_name, gav, jar_path, is_filtered) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, projectId);
            ps.setString(2, branch);
            ps.setString(3, gav);
            ps.setString(4, jarPath);
            ps.setInt(5, isFiltered);
            ps.executeUpdate();
        }
    }

    private void upsertProjectInfo(Connection conn, String projectId, String branch) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO puti_project_info (project_id, branch_name, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP()) " +
                "ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP()")) {
            ps.setString(1, projectId);
            ps.setString(2, branch);
            ps.executeUpdate();
        }
    }

    private void writeManifest(String projectId, String branch, Map<String, File> gavToJar) {
        try {
            Files.createDirectories(manifestDir);
            Path manifestPath = manifestDir.resolve("dependency-manifest.json");
            List<Map<String, String>> deps = new ArrayList<>();
            for (Map.Entry<String, File> entry : gavToJar.entrySet()) {
                Map<String, String> dep = new LinkedHashMap<>();
                dep.put("gav", entry.getKey());
                dep.put("jar_path", entry.getValue() != null ? entry.getValue().getAbsolutePath() : "");
                deps.add(dep);
            }
            Map<String, Object> manifest = new LinkedHashMap<>();
            manifest.put("project_id", projectId);
            manifest.put("branch", branch);
            manifest.put("dependencies", deps);
            manifest.put("updated_at", Instant.now().toString());

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(manifestPath.toFile(), manifest);
            log.info("[Tracker] Wrote manifest to {}", manifestPath);
        } catch (Exception e) {
            log.warn("[Tracker] Failed to write manifest", e);
        }
    }

    /**
     * 批量写入 className → GAV 映射到 puti_dependency_class_index。
     * 使用 INSERT IGNORE 去重（同一 GAV+className 只保留第一条）。
     */
    public void syncClassIndex(Map<String, String> classIndex) {
        if (classIndex == null || classIndex.isEmpty()) return;
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                     "INSERT IGNORE INTO puti_dependency_class_index (gav, class_name) VALUES (?, ?)")) {
                int count = 0;
                for (Map.Entry<String, String> entry : classIndex.entrySet()) {
                    ps.setString(1, entry.getValue()); // gav
                    ps.setString(2, entry.getKey());   // className
                    ps.addBatch();
                    count++;
                    if (count % BATCH_SIZE == 0) {
                        ps.executeBatch();
                    }
                }
                ps.executeBatch();
                conn.commit();
                log.info("[Tracker] Synced {} class→GAV entries into dependency_class_index", count);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            log.error("[Tracker] Failed to sync class index", e);
        }
    }

    /**
     * 加载当前项目所有依赖（含 is_filtered=1）的 className → GAV 映射。
     * 分析开始前调用，加载到内存供 IdGenerator 使用。
     *
     * @param excludeGavs 需要从结果中排除的 GAV 集合（如项目内部模块），
     *                    这些 GAV 对应的类不应进入 classIndex，否则 IdGenerator
     *                    会为它们生成 lib 风格 ID 而非项目风格 ID，导致图谱断裂
     */
    public Map<String, String> loadClassIndex(String projectId, String branch, Set<String> excludeGavs) {
        Map<String, String> result = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT c.class_name, c.gav FROM puti_dependency_class_index c " +
                     "INNER JOIN puti_project_dependency_info p ON c.gav = p.gav " +
                     "WHERE p.project_id = ? AND p.branch_name = ?")) {
            ps.setString(1, projectId);
            ps.setString(2, branch);
            ResultSet rs = ps.executeQuery();
            int excludedCount = 0;
            while (rs.next()) {
                String gav = rs.getString("gav");
                if (excludeGavs != null && excludeGavs.contains(gav)) {
                    excludedCount++;
                    continue;
                }
                result.putIfAbsent(rs.getString("class_name"), gav);
            }
            log.info("[Tracker] Loaded {} class→GAV mappings for project {} branch {} (excluded {} project module GAVs)",
                    result.size(), projectId, branch, excludedCount);
        } catch (SQLException e) {
            log.error("[Tracker] Failed to load class index", e);
        }
        return result;
    }

    /**
     * 删除指定 GAV 列表对应的 class_index 条目（级联清理）。
     */
    public int deleteClassIndexByGav(List<String> orphanedGavs) {
        if (orphanedGavs == null || orphanedGavs.isEmpty()) return 0;
        int total = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM puti_dependency_class_index WHERE gav = ?")) {
            for (String gav : orphanedGavs) {
                ps.setString(1, gav);
                ps.addBatch();
            }
            int[] counts = ps.executeBatch();
            total = Arrays.stream(counts).sum();
            log.info("[Tracker] Deleted {} class_index entries for {} orphaned GAVs", total, orphanedGavs.size());
        } catch (SQLException e) {
            log.error("[Tracker] Failed to delete class index by GAV", e);
        }
        return total;
    }

    /**
     * 尝试标记库图谱为 BUILDING 状态。含超时回退，支持抢占 FAILED。
     *
     * @return true 如果成功标记为 BUILDING
     */
    public boolean tryMarkLibraryBuilding(String gav) {
        // First: rollback stale BUILDING entries
        rollbackStaleBuilding(gav);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE puti_library_graph_info SET graph_status = 'BUILDING', started_at = CURRENT_TIMESTAMP() " +
                     "WHERE gav = ? AND graph_status IN ('PENDING', 'FAILED')")) {
            ps.setString(1, gav);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                log.info("[Tracker] Marked library {} as BUILDING", gav);
                return true;
            }
            log.info("[Tracker] Cannot mark library {} as BUILDING - not in PENDING/FAILED state", gav);
            return false;
        } catch (SQLException e) {
            log.error("[Tracker] Failed to mark library {} as BUILDING", gav, e);
            return false;
        }
    }

    private void rollbackStaleBuilding(String gav) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE puti_library_graph_info SET graph_status = 'FAILED' " +
                     "WHERE gav = ? AND graph_status = 'BUILDING' " +
                     "AND started_at < DATE_SUB(CURRENT_TIMESTAMP(), INTERVAL " + BUILDING_TIMEOUT_MINUTES + " MINUTE)")) {
            ps.setString(1, gav);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                log.warn("[Tracker] Rolled back stale BUILDING status for library {}", gav);
            }
        } catch (SQLException e) {
            log.warn("[Tracker] Failed to rollback stale BUILDING for {}", gav, e);
        }
    }

    public void markLibraryBuilt(String gav, int nodeCount, int edgeCount) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE puti_library_graph_info SET graph_status = 'BUILT', node_count = ?, edge_count = ?, " +
                     "built_at = CURRENT_TIMESTAMP() WHERE gav = ?")) {
            ps.setInt(1, nodeCount);
            ps.setInt(2, edgeCount);
            ps.setString(3, gav);
            ps.executeUpdate();
            log.info("[Tracker] Marked library {} as BUILT ({} nodes, {} edges)", gav, nodeCount, edgeCount);
        } catch (SQLException e) {
            log.error("[Tracker] Failed to mark library {} as BUILT", gav, e);
        }
    }

    public void markLibraryFailed(String gav) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE puti_library_graph_info SET graph_status = 'FAILED' WHERE gav = ?")) {
            ps.setString(1, gav);
            ps.executeUpdate();
            log.info("[Tracker] Marked library {} as FAILED", gav);
        } catch (SQLException e) {
            log.error("[Tracker] Failed to mark library {} as FAILED", gav, e);
        }
    }

    /**
     * 查找孤立库图谱：library_graph_info LEFT JOIN project_dependency_info(is_filtered=0) 无引用的 GAV
     */
    public List<String> findOrphanedLibraryGraphs(int protectionDays) {
        List<String> orphans = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT lg.gav FROM puti_library_graph_info lg " +
                     "LEFT JOIN puti_project_dependency_info pd ON lg.gav = pd.gav AND pd.is_filtered = 0 " +
                     "WHERE pd.gav IS NULL AND lg.built_at < DATE_SUB(CURRENT_TIMESTAMP(), INTERVAL ? DAY) " +
                     "AND lg.graph_status = 'BUILT' ORDER BY lg.built_at LIMIT 50")) {
            ps.setInt(1, protectionDays);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                orphans.add(rs.getString("gav"));
            }
        } catch (SQLException e) {
            log.error("[Tracker] Failed to find orphaned library graphs", e);
        }
        return orphans;
    }

    public void deleteLibraryGraph(String gav) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM puti_library_graph_info WHERE gav = ?")) {
            ps.setString(1, gav);
            ps.executeUpdate();
            log.info("[Tracker] Deleted library graph record for {}", gav);
        } catch (SQLException e) {
            log.error("[Tracker] Failed to delete library graph record for {}", gav, e);
        }
    }

    public String getLibraryGraphStatus(String gav) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT graph_status FROM puti_library_graph_info WHERE gav = ?")) {
            ps.setString(1, gav);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("graph_status");
            }
        } catch (SQLException e) {
            log.error("[Tracker] Failed to get library graph status for {}", gav, e);
        }
        return null;
    }

    /**
     * 轮询等待库图谱构建完成
     */
    public boolean waitForLibraryBuilt(String gav, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            String status = getLibraryGraphStatus(gav);
            if ("BUILT".equals(status)) {
                return true;
            }
            if ("FAILED".equals(status)) {
                return false;
            }
            try {
                Thread.sleep(WAIT_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        log.warn("[Tracker] Timeout waiting for library {} to be built ({}ms)", gav, timeoutMs);
        return false;
    }

    /**
     * 更新项目最近分析的 commit hash
     */
    public void updateLastAnalyzedCommit(String projectId, String branch, String commitHash) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE puti_project_info SET last_analyzed_commit = ?, updated_at = CURRENT_TIMESTAMP() " +
                     "WHERE project_id = ? AND branch_name = ?")) {
            ps.setString(1, commitHash);
            ps.setString(2, projectId);
            ps.setString(3, branch);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("[Tracker] Failed to update last analyzed commit", e);
        }
    }

    public String getLastAnalyzedCommit(String projectId, String branch) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT last_analyzed_commit FROM puti_project_info " +
                     "WHERE project_id = ? AND branch_name = ?")) {
            ps.setString(1, projectId);
            ps.setString(2, branch);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("last_analyzed_commit");
            }
        } catch (SQLException e) {
            log.error("[Tracker] Failed to get last analyzed commit", e);
        }
        return null;
    }

    /**
     * 查询所有已注册的项目信息
     */
    public List<Map<String, String>> listProjects() {
        List<Map<String, String>> projects = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT project_id, branch_name, last_analyzed_commit, updated_at FROM puti_project_info ORDER BY updated_at DESC");
            while (rs.next()) {
                Map<String, String> project = new LinkedHashMap<>();
                project.put("projectId", rs.getString("project_id"));
                project.put("branchName", rs.getString("branch_name"));
                project.put("lastAnalyzedCommit", rs.getString("last_analyzed_commit"));
                Timestamp updatedAt = rs.getTimestamp("updated_at");
                project.put("updatedAt", updatedAt != null ? updatedAt.toInstant().toString() : null);
                projects.add(project);
            }
        } catch (SQLException e) {
            log.error("[Tracker] Failed to list projects", e);
        }
        return projects;
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("[Tracker] DataSource closed");
        }
    }
}
