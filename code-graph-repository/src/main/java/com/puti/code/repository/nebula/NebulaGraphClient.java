package com.puti.code.repository.nebula;

import com.vesoft.nebula.client.graph.NebulaPoolConfig;
import com.vesoft.nebula.client.graph.data.HostAddress;
import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.exception.IOErrorException;
import com.vesoft.nebula.client.graph.net.NebulaPool;
import com.vesoft.nebula.client.graph.net.Session;
import com.puti.code.base.config.AppConfig;
import com.puti.code.base.model.Edge;
import com.puti.code.base.model.EdgeCategory;
import com.puti.code.base.model.EdgePropertySchema;
import com.puti.code.base.model.EdgeSchemaRegistry;
import com.puti.code.base.model.EdgeType;
import com.puti.code.base.model.Node;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * NebulaGraph客户端
 */
@Slf4j
public class NebulaGraphClient implements AutoCloseable {
    
    private final NebulaPool pool;
    private final AppConfig config;
    private final EdgeSchemaRegistry edgeSchemaRegistry;
    private NebulaSchemaManager schemaManager;
    private Session session;

    public NebulaGraphClient() {
        config = AppConfig.getInstance();
        pool = new NebulaPool();
        edgeSchemaRegistry = EdgeSchemaRegistry.getInstance();
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
            session.execute("USE " + config.getNebulaSpace());
            schemaManager = new NebulaSchemaManager(this::execute, edgeSchemaRegistry);
            schemaManager.ensureRegisteredEdges();

            log.info("Connected to NebulaGraph successfully");
        } catch (Exception e) {
            log.error("Failed to initialize NebulaGraph client", e);
            throw new RuntimeException("Failed to initialize NebulaGraph client", e);
        }
    }

    private List<HostAddress> parseHostAddresses(String hosts) {
        List<HostAddress> addresses = new ArrayList<>();
        for (String host : hosts.split(",")) {
            String[] parts = host.split(":");
            if (parts.length == 2) {
                addresses.add(new HostAddress(parts[0], Integer.parseInt(parts[1])));
            }
        }
        return addresses;
    }

    /**
     * 执行查询
     *
     * @param query 查询语句
     * @return 查询结果
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
     * 插入节点
     *
     * @param node 节点
     */
    public void insertNode(Node node) {
        String tag = node.getTag();
        String id = node.getId();
        String[] propertyNames = node.getPropertyNames();
        Object[] properties = node.getProperties();

        StringBuilder query = new StringBuilder();
        query.append("INSERT VERTEX ").append(tag).append(" (");

        // 添加属性名
        query.append(String.join(", ", propertyNames));

        query.append(") VALUES \"").append(id).append("\":(");

        // 添加属性值
        List<String> formattedValues = new ArrayList<>();
        for (Object property : properties) {
            formattedValues.add(formatPropertyValue(property));
        }
        query.append(String.join(", ", formattedValues));

        query.append(")");

        ResultSet resultSet = execute(query.toString());
        if (!resultSet.isSucceeded()) {
            log.error("Failed to insert node: {} query:{} message:{}", id, query, resultSet.getErrorMessage());
            return;
        }
        log.info("Inserted node: {} fullName:{}", id, node.getFullName());
    }

    /**
     * 插入边
     *
     * @param edge 边
     */
    public void insertEdge(Edge edge) {
        ensureEdgeSchema(edge);
        String edgeType = edge.getTypeName();
        String srcId = edge.getSrcId();
        String dstId = edge.getDstId();

        StringBuilder query = new StringBuilder();
        query.append("INSERT EDGE ").append(edgeType).append(" (");
        Map<String, Object> resolvedProperties = edge.resolvedProperties();
        List<String> propertyNames = new ArrayList<>(resolvedProperties.keySet());
        List<String> propertyValues = propertyNames.stream()
                .map(name -> formatPropertyValue(resolvedProperties.get(name)))
                .toList();

        if (!propertyNames.isEmpty()) {
            query.append(String.join(", ", propertyNames));
            query.append(") VALUES \"").append(srcId).append("\" -> \"").append(dstId).append("\":(");
            query.append(String.join(", ", propertyValues));
            query.append(")");
        } else {
            query.append(") VALUES \"").append(srcId).append("\" -> \"").append(dstId).append("\":(");
            query.append(")");
        }

        ResultSet resultSet = execute(query.toString());
        if (!resultSet.isSucceeded()) {
            log.error("Failed to insert edge: {} -> {} query:{} message:{}", srcId, dstId, query, resultSet.getErrorMessage());
            return;
        }
        log.info("Inserted edge: {} -> {}", srcId, dstId);
    }

    /**
     * 批量插入边
     *
     * @param edges 边列表
     */
    public void batchInsertEdges(List<Edge> edges) {
        if (edges == null || edges.isEmpty()) {
            return;
        }

        // 按边类型分组
        Map<EdgeType, List<Edge>> edgesByType = edges.stream()
                .filter(Objects::nonNull)
                .filter(edge -> edge.getType() instanceof EdgeType)
                .collect(Collectors.groupingBy(edge -> (EdgeType) edge.getType()));

        List<Edge> dynamicEdges = edges.stream()
                .filter(Objects::nonNull)
                .filter(edge -> !(edge.getType() instanceof EdgeType))
                .toList();

        // 对每个边类型组批量插入
        for (Map.Entry<EdgeType, List<Edge>> entry : edgesByType.entrySet()) {
            EdgeType edgeType = entry.getKey();
            List<Edge> typeEdges = entry.getValue();

            if (typeEdges.isEmpty()) {
                continue;
            }
            String edgeTypeName = edgeType.getValue();
            ensureEdgeSchema(edgeTypeName, edgeType.getCategory(), typeEdges);

            // 确定属性名
            Set<String> allPropertyNames = new LinkedHashSet<>();
            for (Edge edge : typeEdges) {
                allPropertyNames.addAll(edge.resolvedProperties().keySet());
            }

            List<String> propertyNames = new ArrayList<>(allPropertyNames);

            StringBuilder query = new StringBuilder();
            query.append("INSERT EDGE ").append(edgeTypeName).append(" (");

            if (!propertyNames.isEmpty()) {
                query.append(String.join(", ", propertyNames));
            }

            query.append(") VALUES ");

            List<String> valuesList = new ArrayList<>();

            for (Edge edge : typeEdges) {
                StringBuilder values = new StringBuilder();
                values.append("\"").append(edge.getSrcId()).append("\" -> \"").append(edge.getDstId()).append("\":");

                if (!propertyNames.isEmpty()) {
                    List<String> edgePropertyValues = getPropertyValues(edge, propertyNames);
                    values.append("(").append(String.join(", ", edgePropertyValues)).append(")");
                } else {
                    values.append("()");
                }

                valuesList.add(values.toString());
            }

            query.append(String.join(", ", valuesList));

            ResultSet resultSet = execute(query.toString());
            if (!resultSet.isSucceeded()) {
                log.error("Failed to batch insert edges for type {}: {}", edgeTypeName, resultSet.getErrorMessage());
            } else {
                log.info("Batch inserted {} edges with type {}", typeEdges.size(), edgeTypeName);
            }

        }

        for (Edge edge : dynamicEdges) {
            insertEdge(edge);
        }
    }

    @NotNull
    private static List<String> getPropertyValues(Edge edge, List<String> propertyNames) {
        List<String> edgePropertyValues = new ArrayList<>();
        Map<String, Object> resolvedProperties = edge.resolvedProperties();

        for (String propertyName : propertyNames) {
            Object value = resolvedProperties.get(propertyName);
            edgePropertyValues.add(value == null ? "NULL" : formatStaticPropertyValue(value));
        }
        return edgePropertyValues;
    }

    /**
     * 批量插入节点
     *
     * @param nodes 节点列表
     */
    public void batchInsertNodes(List<Node> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        // 按标签分组节点
        Map<String, List<Node>> nodesByTag = nodes.stream()
                .collect(Collectors.groupingBy(Node::getTag));

        // 对每个标签组批量插入
        for (Map.Entry<String, List<Node>> entry : nodesByTag.entrySet()) {
            String tag = entry.getKey();
            List<Node> tagNodes = entry.getValue();

            // 确保所有节点具有相同的属性名
            if (!tagNodes.isEmpty()) {
                Node firstNode = tagNodes.getFirst();
                String[] propertyNames = firstNode.getPropertyNames();

                StringBuilder query = new StringBuilder();
                query.append("INSERT VERTEX ").append(tag).append(" (");
                query.append(String.join(", ", propertyNames));
                query.append(") VALUES ");

                List<String> valuesList = new ArrayList<>();

                for (Node node : tagNodes) {
                    StringBuilder values = new StringBuilder();
                    values.append("\"").append(node.getId()).append("\":");

                    Object[] properties = node.getProperties();
                    List<String> formattedValues = new ArrayList<>();
                    for (Object property : properties) {
                        formattedValues.add(formatPropertyValue(property));
                    }

                    values.append("(").append(String.join(", ", formattedValues)).append(")");
                    valuesList.add(values.toString());
                }

                query.append(String.join(", ", valuesList));

                ResultSet resultSet = execute(query.toString());
                if (!resultSet.isSucceeded()) {
                    log.error("Failed to batch insert nodes for tag {}: {}", tag, resultSet.getErrorMessage());
                } else {
                    log.info("Batch inserted {} nodes with tag {}", tagNodes.size(), tag);
                }
            }
        }
    }

    /**
     * 格式化属性值
     *
     * @param property 属性值
     * @return 格式化后的属性值字符串
     */
    private String formatPropertyValue(Object property) {
        if (property == null) {
            return "NULL";
        } else if (property instanceof String s) {
            return "\"" + s.replace("\"", "\\\"") + "\"";
        } else if (property instanceof Boolean) {
            return property.toString().toLowerCase();
        } else if (property instanceof LocalDateTime localDateTime) {
            return String.valueOf(localDateTime.toEpochSecond(ZoneOffset.ofHours(8)));
        } else {
            return property.toString();
        }
    }

    private static String formatStaticPropertyValue(Object property) {
        if (property == null) {
            return "NULL";
        } else if (property instanceof String s) {
            return "\"" + s.replace("\"", "\\\"") + "\"";
        } else if (property instanceof Boolean) {
            return property.toString().toLowerCase();
        } else {
            return property.toString();
        }
    }

    private void ensureEdgeSchema(Edge edge) {
        if (schemaManager == null || edge == null || edge.getTypeName() == null) {
            return;
        }
        schemaManager.ensureEdgeSchema(edge.getTypeName(), edge.getCategory(), edge.resolvedProperties());
    }

    private void ensureEdgeSchema(String edgeType, EdgeCategory category, List<Edge> edges) {
        if (schemaManager == null) {
            return;
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        for (Edge edge : edges) {
            edge.resolvedProperties().forEach((key, value) -> {
                if (value != null && !properties.containsKey(key)) {
                    properties.put(key, value);
                }
            });
        }
        schemaManager.ensureEdgeSchema(edgeType, category, properties);
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
