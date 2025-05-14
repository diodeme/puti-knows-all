package com.puti.code.repository.nebula;

import com.puti.code.base.model.EdgeCategory;
import com.puti.code.base.model.EdgePropertySchema;
import com.puti.code.base.model.EdgePropertyType;
import com.puti.code.base.model.EdgeSchema;
import com.puti.code.base.model.EdgeSchemaRegistry;
import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.data.ValueWrapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NebulaSchemaManagerTest {

    private final NebulaSchemaManager schemaManager = new NebulaSchemaManager(query -> null, EdgeSchemaRegistry.getInstance());

    @Test
    void shouldBuildCreateStatementForMissingEdge() {
        EdgeSchema schema = EdgeSchema.builder()
                .value("custom_edge")
                .category(EdgeCategory.FRAMEWORK)
                .comment("custom edge")
                .propertySchema(EdgePropertySchema.builder().name("scope").type(EdgePropertyType.STRING).comment("scope").build())
                .build();

        List<String> statements = schemaManager.buildEnsureStatements(schema, Set.of(), Set.of());

        assertEquals(1, statements.size());
        assertTrue(statements.get(0).startsWith("CREATE EDGE IF NOT EXISTS `custom_edge`"));
        assertTrue(statements.get(0).contains("`scope` string NULL"));
    }

    @Test
    void shouldBuildAlterStatementForMissingColumns() {
        EdgeSchema schema = EdgeSchema.builder()
                .value("custom_edge")
                .category(EdgeCategory.DATA_FLOW)
                .propertySchema(EdgePropertySchema.builder().name("line_number").type(EdgePropertyType.INT64).comment("line").build())
                .propertySchema(EdgePropertySchema.builder().name("flow_kind").type(EdgePropertyType.STRING).comment("kind").build())
                .build();

        List<String> statements = schemaManager.buildEnsureStatements(schema, Set.of("custom_edge"), Set.of("line_number"));

        assertEquals(1, statements.size());
        assertEquals("ALTER EDGE `custom_edge` ADD (`flow_kind` string NULL COMMENT \"kind\")", statements.get(0));
    }

    @Test
    void shouldInferNebulaPropertyTypes() {
        assertEquals(EdgePropertyType.STRING, schemaManager.inferType("demo"));
        assertEquals(EdgePropertyType.INT64, schemaManager.inferType(1));
        assertEquals(EdgePropertyType.INT64, schemaManager.inferType(1L));
        assertEquals(EdgePropertyType.BOOL, schemaManager.inferType(true));
    }

    @Test
    void shouldWaitUntilSchemaBecomesVisibleAfterCreate() {
        AtomicInteger showEdgesCalls = new AtomicInteger();
        List<String> executedQueries = new ArrayList<>();
        String createStatement = "CREATE EDGE `delayed_edge` (`line_number` int64 NULL)";
        NebulaSchemaManager delayedSchemaManager = new NebulaSchemaManager(query -> {
            executedQueries.add(query);
            if ("SHOW EDGES".equals(query)) {
                return stringRowsResult(showEdgesCalls.getAndIncrement() == 0 ? List.of() : List.of("delayed_edge"));
            }
            if ("DESCRIBE EDGE `delayed_edge`".equals(query)) {
                return stringRowsResult(List.of("line_number"));
            }
            if ("SHOW CREATE EDGE `delayed_edge`".equals(query)) {
                return multiColumnStringRowsResult(List.of(List.of("delayed_edge", createStatement)));
            }
            return successResult();
        }, EdgeSchemaRegistry.getInstance(), 3, 0L);

        delayedSchemaManager.ensureEdgeSchema("delayed_edge", EdgeCategory.FRAMEWORK, Map.of("line_number", 12));

        assertTrue(executedQueries.stream().anyMatch(query -> query.startsWith("CREATE EDGE IF NOT EXISTS `delayed_edge`")));
        assertTrue(executedQueries.contains("SHOW CREATE EDGE `delayed_edge`"));
        assertEquals(createStatement, delayedSchemaManager.getCachedCreateStatement("delayed_edge"));
        assertTrue(showEdgesCalls.get() >= 2);
    }

    @Test
    void shouldFailWhenSchemaNeverBecomesVisible() {
        NebulaSchemaManager delayedSchemaManager = new NebulaSchemaManager(query -> {
            if ("SHOW EDGES".equals(query)) {
                return stringRowsResult(List.of());
            }
            return successResult();
        }, EdgeSchemaRegistry.getInstance(), 2, 0L);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                delayedSchemaManager.ensureEdgeSchema("missing_edge", EdgeCategory.SEMANTIC, Map.of("line_number", 1)));

        assertTrue(exception.getMessage().contains("missing_edge"));
    }

    private ResultSet successResult() {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.isSucceeded()).thenReturn(true);
        when(resultSet.getRows()).thenReturn(List.of());
        return resultSet;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ResultSet stringRowsResult(List<String> rowValues) {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.isSucceeded()).thenReturn(true);
        List<ResultSet.Record> records = new ArrayList<>();
        try {
            for (String rowValue : rowValues) {
                ResultSet.Record record = mock(ResultSet.Record.class);
                ValueWrapper valueWrapper = mock(ValueWrapper.class);
                when(valueWrapper.asString()).thenReturn(rowValue);
                when(record.values()).thenReturn(List.of(valueWrapper));
                records.add(record);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        List rows = records;
        when(resultSet.getRows()).thenReturn(rows);
        for (int i = 0; i < records.size(); i++) {
            when(resultSet.rowValues(i)).thenReturn(records.get(i));
        }
        return resultSet;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ResultSet multiColumnStringRowsResult(List<List<String>> rowValues) {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.isSucceeded()).thenReturn(true);
        List<ResultSet.Record> records = new ArrayList<>();
        try {
            for (List<String> rowValue : rowValues) {
                ResultSet.Record record = mock(ResultSet.Record.class);
                List<ValueWrapper> wrappedValues = new ArrayList<>();
                for (String columnValue : rowValue) {
                    ValueWrapper valueWrapper = mock(ValueWrapper.class);
                    when(valueWrapper.asString()).thenReturn(columnValue);
                    wrappedValues.add(valueWrapper);
                }
                when(record.values()).thenReturn(wrappedValues);
                records.add(record);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        List rows = records;
        when(resultSet.getRows()).thenReturn(rows);
        for (int i = 0; i < records.size(); i++) {
            when(resultSet.rowValues(i)).thenReturn(records.get(i));
        }
        return resultSet;
    }
}
