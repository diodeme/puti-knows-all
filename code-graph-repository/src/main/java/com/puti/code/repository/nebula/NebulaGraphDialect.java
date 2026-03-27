package com.puti.code.repository.nebula;

import com.puti.code.base.model.Edge;
import com.puti.code.base.model.EdgePropertySchema;
import com.puti.code.base.model.EdgeSchema;
import com.puti.code.base.model.Node;
import com.puti.code.repository.graph.dialect.GraphDialect;
import com.puti.code.repository.graph.query.GraphDirection;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Nebula nGQL 方言实现。
 */
public class NebulaGraphDialect implements GraphDialect {

    @Override
    public String buildUseSpaceStatement(String spaceName) {
        return "USE " + spaceName;
    }

    @Override
    public String buildShowEdgesQuery() {
        return "SHOW EDGES";
    }

    @Override
    public String buildDescribeEdgeQuery(String edgeName) {
        return "DESCRIBE EDGE `" + edgeName + "`";
    }

    @Override
    public String buildShowCreateEdgeQuery(String edgeName) {
        return "SHOW CREATE EDGE `" + edgeName + "`";
    }

    @Override
    public String buildCreateEdgeStatement(EdgeSchema schema) {
        String columns = schema.getPropertySchemas().stream()
                .map(this::buildPropertyDefinition)
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        return "CREATE EDGE IF NOT EXISTS `" + schema.getValue() + "` (" + columns + ")"
                + " ttl_duration = 0, ttl_col = \"\", comment = \"" + escapeComment(schema.getComment()) + "\"";
    }

    @Override
    public String buildAlterEdgeAddPropertiesStatement(String edgeName, List<EdgePropertySchema> missingProperties) {
        if (missingProperties == null || missingProperties.isEmpty()) {
            return "";
        }
        String columns = missingProperties.stream()
                .map(this::buildPropertyDefinition)
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        return "ALTER EDGE `" + edgeName + "` ADD (" + columns + ")";
    }

    @Override
    public String buildInsertNodeStatement(Node node) {
        if (node == null) {
            return "";
        }
        String[] propertyNames = node.getPropertyNames();
        Object[] properties = node.getProperties();
        List<String> formattedValues = new ArrayList<>();
        if (properties != null) {
            for (Object property : properties) {
                formattedValues.add(formatPropertyValue(property));
            }
        }
        return "INSERT VERTEX " + node.getTag()
                + " (" + String.join(", ", propertyNames) + ") VALUES \""
                + escapeString(node.getId()) + "\":("
                + String.join(", ", formattedValues) + ")";
    }

    @Override
    public String buildInsertEdgeStatement(Edge edge) {
        if (edge == null || edge.getTypeName() == null || edge.getTypeName().isBlank()) {
            return "";
        }
        Map<String, Object> resolvedProperties = edge.resolvedProperties();
        List<String> propertyNames = new ArrayList<>(resolvedProperties.keySet());
        List<String> propertyValues = propertyNames.stream()
                .map(name -> formatPropertyValue(resolvedProperties.get(name)))
                .toList();

        StringBuilder query = new StringBuilder();
        query.append("INSERT EDGE ").append(edge.getTypeName()).append(" (");
        if (!propertyNames.isEmpty()) {
            query.append(String.join(", ", propertyNames));
        }
        query.append(") VALUES \"")
                .append(escapeString(edge.getSrcId()))
                .append("\" -> \"")
                .append(escapeString(edge.getDstId()))
                .append("\":(");
        if (!propertyValues.isEmpty()) {
            query.append(String.join(", ", propertyValues));
        }
        query.append(")");
        return query.toString();
    }

    @Override
    public String buildBatchInsertNodesStatement(String tag, List<Node> nodes) {
        if (tag == null || tag.isBlank() || nodes == null || nodes.isEmpty()) {
            return "";
        }
        Node firstNode = nodes.getFirst();
        String[] propertyNames = firstNode.getPropertyNames();
        StringBuilder query = new StringBuilder();
        query.append("INSERT VERTEX ").append(tag).append(" (")
                .append(String.join(", ", propertyNames))
                .append(") VALUES ");

        List<String> valuesList = new ArrayList<>();
        for (Node node : nodes) {
            List<String> formattedValues = new ArrayList<>();
            Object[] properties = node.getProperties();
            if (properties != null) {
                for (Object property : properties) {
                    formattedValues.add(formatPropertyValue(property));
                }
            }
            valuesList.add("\"" + escapeString(node.getId()) + "\":(" + String.join(", ", formattedValues) + ")");
        }
        query.append(String.join(", ", valuesList));
        return query.toString();
    }

    @Override
    public String buildBatchInsertEdgesStatement(String edgeType, List<String> propertyNames, List<Edge> edges) {
        if (edgeType == null || edgeType.isBlank() || edges == null || edges.isEmpty()) {
            return "";
        }
        List<String> safePropertyNames = propertyNames != null ? propertyNames : List.of();
        StringBuilder query = new StringBuilder();
        query.append("INSERT EDGE ").append(edgeType).append(" (");
        if (!safePropertyNames.isEmpty()) {
            query.append(String.join(", ", safePropertyNames));
        }
        query.append(") VALUES ");

        List<String> valuesList = new ArrayList<>();
        for (Edge edge : edges) {
            List<String> edgePropertyValues = safePropertyNames.stream()
                    .map(propertyName -> formatPropertyValue(edge.resolvedProperties().get(propertyName)))
                    .toList();
            valuesList.add("\"" + escapeString(edge.getSrcId()) + "\" -> \""
                    + escapeString(edge.getDstId()) + "\":("
                    + String.join(", ", edgePropertyValues) + ")");
        }
        query.append(String.join(", ", valuesList));
        return query.toString();
    }

    @Override
    public String buildSearchMethodByNameQuery(String methodName) {
        String escapedMethodName = escapeString(methodName);
        return String.format(
                "MATCH (v:function) WHERE v.function.full_name STARTS WITH \"%s\" OR v.function.name == \"%s\" "
                        + "RETURN v LIMIT 100",
                escapedMethodName,
                escapedMethodName);
    }

    @Override
    public String buildFindFunctionByFullNameQuery(String methodFullName) {
        return String.format("MATCH (v:function) WHERE v.function.full_name == \"%s\" RETURN v LIMIT 1",
                escapeString(methodFullName));
    }

    @Override
    public String buildFindFunctionIdByFullNameQuery(String methodFullName) {
        return String.format("MATCH (v:function) WHERE v.function.full_name == \"%s\" RETURN id(v) as vid LIMIT 1",
                escapeString(methodFullName));
    }

    @Override
    public String buildGetSubgraphQuery(String startNodeId, int pathDepth, GraphDirection direction, List<String> edgeTypes) {
        return String.format(
                "GET SUBGRAPH WITH PROP %d STEPS FROM \"%s\"%s YIELD VERTICES AS nodes, EDGES AS relationships",
                pathDepth,
                escapeString(startNodeId),
                buildTraversalClause(direction, edgeTypes));
    }

    @Override
    public String buildFindNodeByIdQuery(String nodeId) {
        return String.format("MATCH (v) WHERE id(v) == \"%s\" RETURN v AS node LIMIT 1", escapeString(nodeId));
    }

    @Override
    public String buildGetEntryPointsQuery(String projectId, String branchName) {
        return String.format("""
                MATCH (v:function{is_entry_point:TRUE})
                WHERE v.function.repo_id == \"%s\" AND v.function.branch_name == \"%s\"
                RETURN v
                """, escapeString(projectId), escapeString(branchName));
    }

    @Override
    public String buildCountEntryPointsQuery() {
        return "MATCH (v:function{is_entry_point:TRUE}) RETURN count(v) as count";
    }

    private String buildTraversalClause(GraphDirection direction, List<String> edgeTypes) {
        if (edgeTypes == null || edgeTypes.isEmpty()) {
            return "";
        }
        List<String> deduplicatedEdgeTypes = new ArrayList<>(new LinkedHashSet<>(edgeTypes));
        return " " + direction.name() + " " + String.join(", ", deduplicatedEdgeTypes);
    }

    private String buildPropertyDefinition(EdgePropertySchema propertySchema) {
        String comment = propertySchema.getComment() != null
                ? " COMMENT \"" + escapeComment(propertySchema.getComment()) + "\""
                : "";
        return "`" + propertySchema.getName() + "` " + propertySchema.getType().getNebulaType() + " NULL" + comment;
    }

    private String escapeComment(String comment) {
        return comment == null ? "" : comment.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String escapeString(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String formatPropertyValue(Object property) {
        if (property == null) {
            return "NULL";
        }
        if (property instanceof String stringValue) {
            return "\"" + escapeString(stringValue) + "\"";
        }
        if (property instanceof Boolean) {
            return property.toString().toLowerCase();
        }
        if (property instanceof LocalDateTime localDateTime) {
            return String.valueOf(localDateTime.toEpochSecond(ZoneOffset.ofHours(8)));
        }
        return property.toString();
    }
}
