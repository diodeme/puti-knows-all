package com.puti.code.repository.nebula;

import com.puti.code.base.model.EdgeCategory;
import com.puti.code.base.model.EdgePropertySchema;
import com.puti.code.base.model.EdgePropertyType;
import com.puti.code.base.model.EdgeSchema;
import com.puti.code.base.model.EdgeSchemaRegistry;
import com.vesoft.nebula.client.graph.data.ResultSet;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nebula edge schema 管理器。
 */
@Slf4j
public class NebulaSchemaManager {

    private static final int DEFAULT_PROPAGATION_MAX_ATTEMPTS = 5;
    private static final long DEFAULT_PROPAGATION_WAIT_MILLIS = 200L;

    @FunctionalInterface
    public interface QueryExecutor {
        ResultSet execute(String query);
    }

    private final QueryExecutor queryExecutor;
    private final EdgeSchemaRegistry edgeSchemaRegistry;
    private final int propagationMaxAttempts;
    private final long propagationWaitMillis;
    private final Map<String, Set<String>> edgePropertyCache = new ConcurrentHashMap<>();
    private final Map<String, String> edgeCreateStatementCache = new ConcurrentHashMap<>();
    private final Set<String> existingEdgesCache = ConcurrentHashMap.newKeySet();

    public NebulaSchemaManager(QueryExecutor queryExecutor, EdgeSchemaRegistry edgeSchemaRegistry) {
        this(queryExecutor, edgeSchemaRegistry, DEFAULT_PROPAGATION_MAX_ATTEMPTS, DEFAULT_PROPAGATION_WAIT_MILLIS);
    }

    NebulaSchemaManager(QueryExecutor queryExecutor, EdgeSchemaRegistry edgeSchemaRegistry,
                        int propagationMaxAttempts, long propagationWaitMillis) {
        this.queryExecutor = queryExecutor;
        this.edgeSchemaRegistry = edgeSchemaRegistry;
        this.propagationMaxAttempts = Math.max(1, propagationMaxAttempts);
        this.propagationWaitMillis = Math.max(0L, propagationWaitMillis);
    }

    public synchronized void ensureRegisteredEdges() {
        refreshEdgeCache();
        for (EdgeSchema schema : edgeSchemaRegistry.getAll()) {
            ensureEdgeSchema(schema);
        }
    }

    public synchronized void ensureEdgeSchema(String edgeType, EdgeCategory category, Map<String, Object> properties) {
        if (properties != null) {
            properties.forEach((key, value) -> {
                if (value != null) {
                    edgeSchemaRegistry.registerProperty(edgeType, category, EdgePropertySchema.builder()
                            .name(key)
                            .type(inferType(value))
                            .comment(key)
                            .build());
                }
            });
        }
        ensureEdgeSchema(edgeSchemaRegistry.getOrCreate(edgeType, category));
    }

    public List<String> buildEnsureStatements(EdgeSchema schema, Set<String> existingEdges, Set<String> existingProps) {
        List<String> statements = new ArrayList<>();
        if (!existingEdges.contains(schema.getValue())) {
            statements.add(buildCreateEdgeStatement(schema));
            return statements;
        }

        List<String> missingColumns = schema.getPropertySchemas().stream()
                .filter(property -> !existingProps.contains(property.getName()))
                .map(this::buildPropertyDefinition)
                .toList();
        if (!missingColumns.isEmpty()) {
            statements.add("ALTER EDGE `" + schema.getValue() + "` ADD (" + String.join(", ", missingColumns) + ")");
        }
        return statements;
    }

    EdgePropertyType inferType(Object value) {
        if (value instanceof Boolean) {
            return EdgePropertyType.BOOL;
        }
        if (value instanceof Integer || value instanceof Long) {
            return EdgePropertyType.INT64;
        }
        if (value instanceof Float || value instanceof Double) {
            return EdgePropertyType.DOUBLE;
        }
        return EdgePropertyType.STRING;
    }

    private void ensureEdgeSchema(EdgeSchema schema) {
        Set<String> existingProps = edgePropertyCache.getOrDefault(schema.getValue(), Set.of());
        List<String> statements = buildEnsureStatements(schema, existingEdgesCache, existingProps);
        if (statements.isEmpty()) {
            return;
        }

        for (String statement : statements) {
            ResultSet resultSet = queryExecutor.execute(statement);
            if (resultSet == null || !resultSet.isSucceeded()) {
                String message = resultSet != null ? resultSet.getErrorMessage() : "null result";
                throw new IllegalStateException("Failed to ensure edge schema with query [" + statement + "]: " + message);
            }
            log.info("Ensured Nebula edge schema with query: {}", statement);
            awaitSchemaPropagation(schema, statement);
        }
    }

    private String buildCreateEdgeStatement(EdgeSchema schema) {
        String columns = schema.getPropertySchemas().stream()
                .map(this::buildPropertyDefinition)
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        return "CREATE EDGE IF NOT EXISTS `" + schema.getValue() + "` (" + columns + ")"
                + " ttl_duration = 0, ttl_col = \"\", comment = \"" + escapeComment(schema.getComment()) + "\"";
    }

    private String buildPropertyDefinition(EdgePropertySchema propertySchema) {
        String comment = propertySchema.getComment() != null ? " COMMENT \"" + escapeComment(propertySchema.getComment()) + "\"" : "";
        return "`" + propertySchema.getName() + "` " + propertySchema.getType().getNebulaType() + " NULL" + comment;
    }

    private String escapeComment(String comment) {
        return comment == null ? "" : comment.replace("\"", "\\\"");
    }

    private void awaitSchemaPropagation(EdgeSchema schema, String statement) {
        for (int attempt = 1; attempt <= propagationMaxAttempts; attempt++) {
            refreshEdgeCache();
            if (isSchemaReady(schema)) {
                refreshCreateStatementCache(schema.getValue());
                return;
            }
            log.debug("Nebula edge schema {} is not visible yet after [{}], attempt {}/{}",
                    schema.getValue(), statement, attempt, propagationMaxAttempts);
            if (attempt < propagationMaxAttempts) {
                sleepBeforeRetry(schema, statement);
            }
        }
        throw new IllegalStateException("Nebula edge schema [" + schema.getValue()
                + "] did not become visible after query [" + statement + "]");
    }

    private boolean isSchemaReady(EdgeSchema schema) {
        if (!existingEdgesCache.contains(schema.getValue())) {
            return false;
        }
        Set<String> existingProperties = edgePropertyCache.getOrDefault(schema.getValue(), Set.of());
        return schema.getPropertySchemas().stream()
                .map(EdgePropertySchema::getName)
                .allMatch(existingProperties::contains);
    }

    private void sleepBeforeRetry(EdgeSchema schema, String statement) {
        try {
            Thread.sleep(propagationWaitMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Nebula edge schema ["
                    + schema.getValue() + "] after query [" + statement + "]", e);
        }
    }

    private void refreshEdgeCache() {
        existingEdgesCache.clear();
        edgePropertyCache.clear();

        ResultSet edgeResult = queryExecutor.execute("SHOW EDGES");
        if (edgeResult == null || !edgeResult.isSucceeded() || edgeResult.getRows() == null) {
            return;
        }

        for (int i = 0; i < edgeResult.getRows().size(); i++) {
            try {
                String edgeName = edgeResult.rowValues(i).values().get(0).asString();
                existingEdgesCache.add(edgeName);
                edgePropertyCache.put(edgeName, describeEdge(edgeName));
            } catch (Exception e) {
                log.warn("Failed to parse edge info from SHOW EDGES", e);
            }
        }
        edgeCreateStatementCache.keySet().removeIf(edgeName -> !existingEdgesCache.contains(edgeName));
    }

    String getCachedCreateStatement(String edgeName) {
        return edgeCreateStatementCache.get(edgeName);
    }

    private Set<String> describeEdge(String edgeName) {
        ResultSet describeResult = queryExecutor.execute("DESCRIBE EDGE `" + edgeName + "`");
        Set<String> properties = new LinkedHashSet<>();
        if (describeResult == null || !describeResult.isSucceeded() || describeResult.getRows() == null) {
            return properties;
        }

        for (int i = 0; i < describeResult.getRows().size(); i++) {
            try {
                String propertyName = describeResult.rowValues(i).values().get(0).asString();
                if (Objects.nonNull(propertyName) && !propertyName.isBlank()) {
                    properties.add(propertyName);
                }
            } catch (Exception e) {
                log.warn("Failed to parse DESCRIBE EDGE result for {}", edgeName, e);
            }
        }
        return properties;
    }

    private void refreshCreateStatementCache(String edgeName) {
        String createStatement = showCreateEdge(edgeName);
        if (createStatement == null || createStatement.isBlank()) {
            edgeCreateStatementCache.remove(edgeName);
            return;
        }
        edgeCreateStatementCache.put(edgeName, createStatement);
    }

    private String showCreateEdge(String edgeName) {
        ResultSet createResult = queryExecutor.execute("SHOW CREATE EDGE `" + edgeName + "`");
        if (createResult == null || !createResult.isSucceeded() || createResult.getRows() == null
                || createResult.getRows().isEmpty()) {
            return null;
        }
        try {
            ResultSet.Record record = createResult.rowValues(0);
            for (int columnIndex = 0; columnIndex < record.values().size(); columnIndex++) {
                try {
                    String value = record.values().get(columnIndex).asString();
                    if (value != null && value.toUpperCase().contains("CREATE EDGE")) {
                        return value;
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse SHOW CREATE EDGE result for {}", edgeName, e);
        }
        return null;
    }
}
