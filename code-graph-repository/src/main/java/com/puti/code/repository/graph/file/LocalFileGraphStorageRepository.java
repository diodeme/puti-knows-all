package com.puti.code.repository.graph.file;

import com.fasterxml.jackson.core.type.TypeReference;
import com.puti.code.base.config.AppConfig;
import com.puti.code.base.model.Edge;
import com.puti.code.base.model.Node;
import com.puti.code.repository.graph.GraphStorageRepository;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 本地文件图存储实现，便于调试和离线导出。
 */
@Slf4j
public class LocalFileGraphStorageRepository implements GraphStorageRepository {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final Path storageDir;
    private final BufferedWriter nodeWriter;
    private final BufferedWriter edgeWriter;

    public LocalFileGraphStorageRepository() {
        this(LocalFileGraphRepositorySupport.resolveStorageDir(AppConfig.getInstance()));
    }

    public LocalFileGraphStorageRepository(Path storageDir) {
        try {
            this.storageDir = storageDir;
            Files.createDirectories(storageDir);
            Path nodePath = storageDir.resolve("nodes.jsonl");
            Path edgePath = storageDir.resolve("edges.jsonl");
            Files.writeString(nodePath, "", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.writeString(edgePath, "", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            nodeWriter = Files.newBufferedWriter(nodePath, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            edgeWriter = Files.newBufferedWriter(edgePath, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            log.info("Initialized local file graph storage at {}", storageDir.toAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize local file graph storage", e);
        }
    }

    @Override
    public void insertNode(Node node) {
        writeLine(nodeWriter, toNodeRecord(node));
    }

    @Override
    public void insertEdge(Edge edge) {
        writeLine(edgeWriter, toEdgeRecord(edge));
    }

    @Override
    public void batchInsertNodes(List<Node> nodes) {
        if (nodes == null) {
            return;
        }
        for (Node node : nodes) {
            insertNode(node);
        }
    }

    @Override
    public void batchInsertEdges(List<Edge> edges) {
        if (edges == null) {
            return;
        }
        for (Edge edge : edges) {
            insertEdge(edge);
        }
    }

    @Override
    public void close() {
        closeWriter(nodeWriter, "nodes");
        closeWriter(edgeWriter, "edges");
    }

    private void writeLine(BufferedWriter writer, Map<String, Object> record) {
        if (record == null) {
            return;
        }
        synchronized (writer) {
            try {
                writer.write(LocalFileGraphRepositorySupport.OBJECT_MAPPER.writeValueAsString(record));
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to write graph record to local file storage", e);
            }
        }
    }

    private Map<String, Object> toNodeRecord(Node node) {
        if (node == null) {
            return null;
        }
        return LocalFileGraphRepositorySupport.OBJECT_MAPPER.convertValue(LocalFileGraphNodeRecord.builder()
                .id(node.getId())
                .nodeType(node.getNodeType() != null ? node.getNodeType().getValue() : null)
                .tag(node.getTag())
                .fullName(node.getFullName())
                .properties(toPropertyMap(node.getPropertyNames(), node.getProperties()))
                .build(), MAP_TYPE);
    }

    private Map<String, Object> toEdgeRecord(Edge edge) {
        if (edge == null) {
            return null;
        }
        return LocalFileGraphRepositorySupport.OBJECT_MAPPER.convertValue(LocalFileGraphEdgeRecord.builder()
                .source(edge.getSrcId())
                .target(edge.getDstId())
                .type(edge.getTypeName())
                .category(edge.getCategory() != null ? edge.getCategory().name() : null)
                .properties(edge.resolvedProperties())
                .build(), MAP_TYPE);
    }

    private Map<String, Object> toPropertyMap(String[] propertyNames, Object[] properties) {
        Map<String, Object> propertyMap = new LinkedHashMap<>();
        if (propertyNames == null || properties == null) {
            return propertyMap;
        }
        int size = Math.min(propertyNames.length, properties.length);
        for (int i = 0; i < size; i++) {
            propertyMap.put(propertyNames[i], properties[i]);
        }
        return propertyMap;
    }

    private void closeWriter(BufferedWriter writer, String label) {
        try {
            writer.close();
        } catch (IOException e) {
            log.warn("Failed to close local file graph storage {} writer", label, e);
        }
    }
}
