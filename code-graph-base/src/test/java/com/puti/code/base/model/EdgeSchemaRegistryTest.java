package com.puti.code.base.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EdgeSchemaRegistryTest {

    @Test
    void shouldContainBuiltinSchemaMetadata() {
        EdgeSchemaRegistry registry = EdgeSchemaRegistry.getInstance();

        EdgeSchema dependsOn = registry.find("depends_on").orElseThrow();

        assertEquals(EdgeCategory.SEMANTIC, dependsOn.getCategory());
        assertEquals("依赖关系", dependsOn.getDisplayName());
        assertTrue(dependsOn.getPropertySchemas().stream().anyMatch(schema -> "dependency_type".equals(schema.getName())));
        assertTrue(dependsOn.getPropertySchemas().stream().anyMatch(schema -> "line_number".equals(schema.getName())));
    }

    @Test
    void shouldMergeDynamicPropertyRegistrationWithoutDuplicates() {
        EdgeSchemaRegistry registry = EdgeSchemaRegistry.getInstance();
        registry.registerProperty("custom_edge", EdgeCategory.FRAMEWORK,
                EdgePropertySchema.builder().name("weight").type(EdgePropertyType.INT64).comment("weight").build());
        registry.registerProperty("custom_edge", EdgeCategory.FRAMEWORK,
                EdgePropertySchema.builder().name("weight").type(EdgePropertyType.INT64).comment("weight").build());
        registry.registerProperty("custom_edge", EdgeCategory.FRAMEWORK,
                EdgePropertySchema.builder().name("scope").type(EdgePropertyType.STRING).comment("scope").build());

        EdgeSchema schema = registry.find("custom_edge").orElseThrow();

        assertEquals(2, schema.getPropertySchemas().size());
        assertEquals(EdgeCategory.FRAMEWORK, schema.getCategory());
        assertEquals("custom_edge", schema.getDisplayName());
    }

    @Test
    void shouldFallbackDisplayNameToCommentForDynamicSchema() {
        EdgeSchemaRegistry registry = EdgeSchemaRegistry.getInstance();
        registry.register(EdgeSchema.builder()
                .value("custom_flow")
                .category(EdgeCategory.DATA_FLOW)
                .comment("自定义流转")
                .build());

        EdgeSchema schema = registry.find("custom_flow").orElseThrow();

        assertEquals("自定义流转", schema.getDisplayName());
    }
}
