package com.puti.code.base.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class EdgeTest {

    @Test
    void shouldMapLegacyBuilderFieldsIntoDynamicProperties() {
        Edge edge = Edge.builder()
                .srcId("src")
                .dstId("dst")
                .type(EdgeType.DEPENDS_ON)
                .dependencyType(DependencyType.USAGE)
                .lineNumber(18)
                .property("custom_flag", true)
                .build();

        assertEquals("USAGE", edge.getProperties().get("dependency_type"));
        assertEquals(18, edge.getProperties().get("line_number"));
        assertEquals(Boolean.TRUE, edge.getProperties().get("custom_flag"));
        assertEquals(edge.getProperties(), edge.resolvedProperties());
        assertNotSame(edge.getProperties(), edge.resolvedProperties());
    }

    @Test
    void shouldDefensivelyCopyPropertiesWhenBuilding() {
        Map<String, Object> properties = Map.of("line_number", 7);

        Edge edge = Edge.builder()
                .srcId("src")
                .dstId("dst")
                .properties(properties)
                .build();

        assertEquals(7, edge.getProperties().get("line_number"));
        assertNotSame(properties, edge.getProperties());
    }

    @Test
    void shouldMergeExplicitPropertiesWithLegacyBuilderFields() {
        Edge edge = Edge.builder()
                .srcId("src")
                .dstId("dst")
                .lineNumber(19)
                .properties(Map.of("resolution_kind", "requestedName"))
                .build();

        assertEquals(19, edge.getProperties().get("line_number"));
        assertEquals("requestedName", edge.getProperties().get("resolution_kind"));
    }
}
