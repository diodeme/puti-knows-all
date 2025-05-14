package com.puti.code.base.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EdgeDefinitionResolverTest {

    private final EdgeSchemaRegistry edgeSchemaRegistry = EdgeSchemaRegistry.getInstance();
    private final EdgeDefinitionResolver resolver = new EdgeDefinitionResolver(edgeSchemaRegistry);

    @Test
    void shouldResolveBuiltinEdgeType() {
        EdgeDefinition edgeDefinition = resolver.resolve("calls", EdgeCategory.DATA_FLOW);

        assertEquals("calls", edgeDefinition.getValue());
        assertEquals(EdgeCategory.SEMANTIC, edgeDefinition.getCategory());
        assertInstanceOf(EdgeType.class, edgeDefinition);
    }

    @Test
    void shouldResolveRegisteredDynamicSchema() {
        edgeSchemaRegistry.register(EdgeSchema.builder()
                .value("resolver_registered_edge")
                .category(EdgeCategory.FRAMEWORK)
                .displayName("Resolver Registered Edge")
                .build());

        EdgeDefinition edgeDefinition = resolver.resolve("resolver_registered_edge", EdgeCategory.DATA_FLOW);

        assertEquals("resolver_registered_edge", edgeDefinition.getValue());
        assertEquals(EdgeCategory.FRAMEWORK, edgeDefinition.getCategory());
        assertInstanceOf(RegisteredEdgeType.class, edgeDefinition);
    }

    @Test
    void shouldAutoRegisterUnknownEdgeWithFallbackCategory() {
        EdgeDefinition edgeDefinition = resolver.resolve("resolver_auto_registered_edge", EdgeCategory.DATA_FLOW);

        assertEquals("resolver_auto_registered_edge", edgeDefinition.getValue());
        assertEquals(EdgeCategory.DATA_FLOW, edgeDefinition.getCategory());
        assertEquals(EdgeCategory.DATA_FLOW,
                edgeSchemaRegistry.find("resolver_auto_registered_edge").orElseThrow().getCategory());
        assertInstanceOf(RegisteredEdgeType.class, edgeDefinition);
    }

    @Test
    void shouldRejectBlankEdgeType() {
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("  ", EdgeCategory.FRAMEWORK));
    }
}
