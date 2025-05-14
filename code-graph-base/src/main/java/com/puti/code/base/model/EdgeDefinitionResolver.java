package com.puti.code.base.model;

/**
 * 统一的逻辑 edge 定义解析器。
 */
public class EdgeDefinitionResolver {

    private final EdgeSchemaRegistry edgeSchemaRegistry;

    public EdgeDefinitionResolver() {
        this(EdgeSchemaRegistry.getInstance());
    }

    public EdgeDefinitionResolver(EdgeSchemaRegistry edgeSchemaRegistry) {
        this.edgeSchemaRegistry = edgeSchemaRegistry;
    }

    public EdgeDefinition resolve(String edgeType) {
        return resolve(edgeType, EdgeCategory.SEMANTIC);
    }

    public EdgeDefinition resolve(String edgeType, EdgeCategory fallbackCategory) {
        if (edgeType == null || edgeType.isBlank()) {
            throw new IllegalArgumentException("edgeType must not be blank");
        }
        return EdgeType.fromValue(edgeType)
                .<EdgeDefinition>map(value -> value)
                .orElseGet(() -> edgeSchemaRegistry.find(edgeType)
                        .<EdgeDefinition>map(RegisteredEdgeType::new)
                        .orElseGet(() -> new RegisteredEdgeType(edgeSchemaRegistry.getOrCreate(
                                edgeType,
                                fallbackCategory != null ? fallbackCategory : EdgeCategory.SEMANTIC))));
    }
}
