package com.puti.code.repository.graph.file;

import com.puti.code.base.config.AppConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 本地图存储快照加载器。
 */
@Slf4j
public class LocalFileGraphStore {

    private final Path nodeFile;
    private final Path edgeFile;
    private volatile LocalFileGraphSnapshot cachedSnapshot;
    private volatile long cachedNodeModifiedAt = -1L;
    private volatile long cachedEdgeModifiedAt = -1L;

    public LocalFileGraphStore() {
        this(LocalFileGraphRepositorySupport.resolveNodeFile(AppConfig.getInstance()),
                LocalFileGraphRepositorySupport.resolveEdgeFile(AppConfig.getInstance()));
    }

    public LocalFileGraphStore(Path nodeFile, Path edgeFile) {
        this.nodeFile = nodeFile;
        this.edgeFile = edgeFile;
    }

    public synchronized LocalFileGraphSnapshot snapshot() {
        long nodeModifiedAt = lastModified(nodeFile);
        long edgeModifiedAt = lastModified(edgeFile);
        if (cachedSnapshot != null && cachedNodeModifiedAt == nodeModifiedAt && cachedEdgeModifiedAt == edgeModifiedAt) {
            return cachedSnapshot;
        }

        LocalFileGraphSnapshot snapshot = LocalFileGraphSnapshot.builder().build();
        loadNodes(snapshot, nodeFile);
        loadEdges(snapshot, edgeFile);
        cachedSnapshot = snapshot;
        cachedNodeModifiedAt = nodeModifiedAt;
        cachedEdgeModifiedAt = edgeModifiedAt;
        return snapshot;
    }

    private void loadNodes(LocalFileGraphSnapshot snapshot, Path nodeFile) {
        if (!Files.exists(nodeFile)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(nodeFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                LocalFileGraphNodeRecord node = LocalFileGraphRepositorySupport.OBJECT_MAPPER.readValue(line, LocalFileGraphNodeRecord.class);
                snapshot.getNodesById().put(node.getId(), node);
                if ("function".equals(node.getTag())) {
                    snapshot.getFunctionNodes().add(node);
                    Object fullName = node.getProperties().get("full_name");
                    if (fullName != null) {
                        snapshot.getFunctionsByFullName().put(String.valueOf(fullName), node);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load local graph nodes", e);
        }
    }

    private void loadEdges(LocalFileGraphSnapshot snapshot, Path edgeFile) {
        if (!Files.exists(edgeFile)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(edgeFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                LocalFileGraphEdgeRecord edge = LocalFileGraphRepositorySupport.OBJECT_MAPPER.readValue(line, LocalFileGraphEdgeRecord.class);
                snapshot.getEdges().add(edge);
                snapshot.getOutgoingEdges()
                        .computeIfAbsent(edge.getSource(), key -> new ArrayList<>())
                        .add(edge);
                snapshot.getIncomingEdges()
                        .computeIfAbsent(edge.getTarget(), key -> new ArrayList<>())
                        .add(edge);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load local graph edges", e);
        }
    }

    private long lastModified(Path path) {
        try {
            return Files.exists(path) ? Files.getLastModifiedTime(path).toMillis() : -1L;
        } catch (IOException e) {
            log.warn("Failed to read lastModified for {}", path, e);
            return -1L;
        }
    }
}
