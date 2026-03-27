package com.puti.code.repository.nebula;

import com.puti.code.base.config.AppConfig;
import com.puti.code.base.model.Edge;
import com.puti.code.base.model.Node;
import com.puti.code.repository.graph.dialect.GraphDialect;
import com.vesoft.nebula.client.graph.NebulaPoolConfig;
import com.vesoft.nebula.client.graph.data.HostAddress;
import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.exception.IOErrorException;
import com.vesoft.nebula.client.graph.net.NebulaPool;
import com.vesoft.nebula.client.graph.net.Session;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * NebulaGraph 客户端。
 */
@Slf4j
public class NebulaGraphClient implements AutoCloseable {

    private final NebulaPool pool;
    private final AppConfig config;
    private final GraphDialect graphDialect;
    private Session session;

    public NebulaGraphClient() {
        this(new NebulaGraphDialect());
    }

    public NebulaGraphClient(GraphDialect graphDialect) {
        this(AppConfig.getInstance(), graphDialect);
    }

    NebulaGraphClient(AppConfig config, GraphDialect graphDialect) {
        this.config = config;
        this.graphDialect = graphDialect;
        this.pool = new NebulaPool();
        init();
    }

    private void init() {
        try {
            NebulaPoolConfig nebulaPoolConfig = new NebulaPoolConfig();
            nebulaPoolConfig.setMaxConnSize(config.getNebulaConnectionPoolSize());
            nebulaPoolConfig.setTimeout(config.getNebulaTimeout());

            List<HostAddress> addresses = parseHostAddresses(config.getNebulaHosts());
            pool.init(addresses, nebulaPoolConfig);

            session = pool.getSession(config.getNebulaUsername(), config.getNebulaPassword(), false);
            session.execute(graphDialect.buildUseSpaceStatement(config.getNebulaSpace()));
            log.info("Connected to NebulaGraph successfully");
        } catch (Exception e) {
            log.error("Failed to initialize NebulaGraph client", e);
            throw new RuntimeException("Failed to initialize NebulaGraph client", e);
        }
    }

    private List<HostAddress> parseHostAddresses(String hosts) {
        if (hosts == null || hosts.isBlank()) {
            throw new IllegalStateException("nebula.hosts must not be blank");
        }
        List<HostAddress> addresses = new ArrayList<>();
        for (String host : hosts.split(",")) {
            String[] parts = host.split(":");
            if (parts.length == 2) {
                addresses.add(new HostAddress(parts[0], Integer.parseInt(parts[1])));
            }
        }
        if (addresses.isEmpty()) {
            throw new IllegalStateException("No valid Nebula host configured: " + hosts);
        }
        return addresses;
    }

    /**
     * 执行查询。
     */
    public ResultSet execute(String query) {
        try {
            log.debug("Executing query: {}", query);
            return session.execute(query);
        } catch (IOErrorException e) {
            log.error("Failed to execute query: {}", query, e);
            throw new RuntimeException("Failed to execute query", e);
        }
    }

    /**
     * 插入节点。
     */
    public void insertNode(Node node) {
        String query = graphDialect.buildInsertNodeStatement(node);
        if (query == null || query.isBlank()) {
            return;
        }
        ResultSet resultSet = execute(query);
        if (!resultSet.isSucceeded()) {
            log.error("Failed to insert node: {} query:{} message:{}",
                    node != null ? node.getId() : null, query, resultSet.getErrorMessage());
            return;
        }
        log.info("Inserted node: {} fullName:{}", node.getId(), node.getFullName());
    }

    /**
     * 插入边。
     */
    public void insertEdge(Edge edge) {
        String query = graphDialect.buildInsertEdgeStatement(edge);
        if (query == null || query.isBlank()) {
            return;
        }
        ResultSet resultSet = execute(query);
        if (!resultSet.isSucceeded()) {
            log.error("Failed to insert edge: {} -> {} query:{} message:{}",
                    edge != null ? edge.getSrcId() : null,
                    edge != null ? edge.getDstId() : null,
                    query,
                    resultSet.getErrorMessage());
            return;
        }
        log.info("Inserted edge: {} -> {}", edge.getSrcId(), edge.getDstId());
    }

    /**
     * 批量插入边。
     */
    public void batchInsertEdges(List<Edge> edges) {
        if (edges == null || edges.isEmpty()) {
            return;
        }

        Map<String, List<Edge>> edgesByType = edges.stream()
                .filter(Objects::nonNull)
                .filter(edge -> edge.getTypeName() != null && !edge.getTypeName().isBlank())
                .collect(Collectors.groupingBy(Edge::getTypeName, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<Edge>> entry : edgesByType.entrySet()) {
            String edgeTypeName = entry.getKey();
            List<Edge> typeEdges = entry.getValue();
            if (typeEdges.isEmpty()) {
                continue;
            }

            Set<String> allPropertyNames = new LinkedHashSet<>();
            for (Edge edge : typeEdges) {
                allPropertyNames.addAll(edge.resolvedProperties().keySet());
            }

            String query = graphDialect.buildBatchInsertEdgesStatement(
                    edgeTypeName,
                    new ArrayList<>(allPropertyNames),
                    typeEdges);
            if (query == null || query.isBlank()) {
                continue;
            }

            ResultSet resultSet = execute(query);
            if (!resultSet.isSucceeded()) {
                log.error("Failed to batch insert edges for type {}: {}", edgeTypeName, resultSet.getErrorMessage());
            } else {
                log.info("Batch inserted {} edges with type {}", typeEdges.size(), edgeTypeName);
            }
        }
    }

    /**
     * 批量插入节点。
     */
    public void batchInsertNodes(List<Node> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        Map<String, List<Node>> nodesByTag = nodes.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Node::getTag, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<Node>> entry : nodesByTag.entrySet()) {
            String tag = entry.getKey();
            List<Node> tagNodes = entry.getValue();
            if (tagNodes.isEmpty()) {
                continue;
            }

            String query = graphDialect.buildBatchInsertNodesStatement(tag, tagNodes);
            if (query == null || query.isBlank()) {
                continue;
            }

            ResultSet resultSet = execute(query);
            if (!resultSet.isSucceeded()) {
                log.error("Failed to batch insert nodes for tag {}: {}", tag, resultSet.getErrorMessage());
            } else {
                log.info("Batch inserted {} nodes with tag {}", tagNodes.size(), tag);
            }
        }
    }

    @Override
    public void close() {
        try {
            if (session != null) {
                session.release();
            }
            pool.close();
            log.info("NebulaGraph client closed");
        } catch (Exception e) {
            log.error("Failed to close NebulaGraph client", e);
        }
    }
}
