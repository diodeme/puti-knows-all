package com.puti.code.repository.graph.nebula;

import com.puti.code.base.model.DependencyType;
import com.puti.code.base.model.Edge;
import com.puti.code.base.model.EdgeCategory;
import com.puti.code.base.model.EdgeType;
import com.puti.code.repository.graph.schema.GraphSchemaManager;
import com.puti.code.repository.nebula.NebulaGraphClient;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NebulaGraphStorageRepositoryTest {

    @Test
    void shouldEnsureRegisteredSchemasOnCreation() {
        NebulaGraphClient nebulaGraphClient = mock(NebulaGraphClient.class);
        GraphSchemaManager graphSchemaManager = mock(GraphSchemaManager.class);

        new NebulaGraphStorageRepository(nebulaGraphClient, graphSchemaManager);

        verify(graphSchemaManager).ensureRegisteredSchemas();
    }

    @Test
    void shouldEnsureSchemaBeforeSingleEdgeInsert() {
        NebulaGraphClient nebulaGraphClient = mock(NebulaGraphClient.class);
        GraphSchemaManager graphSchemaManager = mock(GraphSchemaManager.class);
        NebulaGraphStorageRepository repository = new NebulaGraphStorageRepository(nebulaGraphClient, graphSchemaManager);
        clearInvocations(nebulaGraphClient, graphSchemaManager);

        Edge edge = Edge.builder()
                .srcId("source")
                .dstId("target")
                .type(EdgeType.CALLS)
                .lineNumber(12)
                .build();

        repository.insertEdge(edge);

        InOrder inOrder = inOrder(graphSchemaManager, nebulaGraphClient);
        inOrder.verify(graphSchemaManager).ensureEdgeSchema("calls", EdgeCategory.SEMANTIC, Map.of("line_number", 12));
        inOrder.verify(nebulaGraphClient).insertEdge(edge);
    }

    @Test
    void shouldMergeSchemaPropertiesByEdgeTypeBeforeBatchInsert() {
        NebulaGraphClient nebulaGraphClient = mock(NebulaGraphClient.class);
        GraphSchemaManager graphSchemaManager = mock(GraphSchemaManager.class);
        NebulaGraphStorageRepository repository = new NebulaGraphStorageRepository(nebulaGraphClient, graphSchemaManager);
        clearInvocations(nebulaGraphClient, graphSchemaManager);

        Edge firstEdge = Edge.builder()
                .srcId("source-1")
                .dstId("target-1")
                .type(EdgeType.CALLS)
                .lineNumber(10)
                .build();
        Edge secondEdge = Edge.builder()
                .srcId("source-2")
                .dstId("target-2")
                .type(EdgeType.CALLS)
                .dependencyType(DependencyType.CALL)
                .build();

        repository.batchInsertEdges(List.of(firstEdge, secondEdge));

        verify(graphSchemaManager).ensureEdgeSchema(
                eq("calls"),
                eq(EdgeCategory.SEMANTIC),
                argThat(properties -> properties.size() == 2
                        && properties.get("line_number").equals(10)
                        && properties.get("dependency_type").equals("CALL")));
        verify(nebulaGraphClient).batchInsertEdges(List.of(firstEdge, secondEdge));
    }
}
