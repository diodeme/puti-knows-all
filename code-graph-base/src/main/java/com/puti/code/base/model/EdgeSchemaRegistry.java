package com.puti.code.base.model;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Edge schema 注册中心。
 */
@Slf4j
public class EdgeSchemaRegistry {

    private static final EdgeSchemaRegistry INSTANCE = new EdgeSchemaRegistry();

    private final Map<String, EdgeSchema> schemas = new ConcurrentHashMap<>();

    private EdgeSchemaRegistry() {
        registerBuiltinSchemas();
    }

    public static EdgeSchemaRegistry getInstance() {
        return INSTANCE;
    }

    public void register(EdgeSchema schema) {
        schemas.put(schema.getValue(), normalize(schema));
        log.debug("Registered edge schema: {}", schema.getValue());
    }

    public void register(EdgeDefinition definition) {
        register(EdgeSchema.builder()
                .value(definition.getValue())
                .category(definition.getCategory())
                .displayName(definition.getDisplayName())
                .propertySchemas(definition.getPropertySchemas())
                .build());
    }

    public Optional<EdgeSchema> find(String edgeType) {
        return Optional.ofNullable(schemas.get(edgeType));
    }

    public EdgeSchema getOrCreate(String edgeType, EdgeCategory category) {
        return schemas.computeIfAbsent(edgeType, name -> EdgeSchema.builder()
                .value(name)
                .category(category)
                .displayName(name)
                .comment(name)
                .build());
    }

    public void registerProperty(String edgeType, EdgeCategory category, EdgePropertySchema propertySchema) {
        EdgeSchema existing = getOrCreate(edgeType, category);
        Map<String, EdgePropertySchema> propertyMap = new LinkedHashMap<>();
        for (EdgePropertySchema schema : existing.getPropertySchemas()) {
            propertyMap.put(schema.getName(), schema);
        }
        propertyMap.put(propertySchema.getName(), propertySchema);
        register(EdgeSchema.builder()
                .value(existing.getValue())
                .category(existing.getCategory())
                .displayName(existing.getDisplayName())
                .comment(existing.getComment())
                .propertySchemas(new ArrayList<>(propertyMap.values()))
                .build());
    }

    public List<EdgeSchema> getAll() {
        return schemas.values().stream()
                .sorted(Comparator.comparing(EdgeSchema::getValue))
                .toList();
    }

    private EdgeSchema normalize(EdgeSchema schema) {
        Map<String, EdgePropertySchema> normalized = new LinkedHashMap<>();
        for (EdgePropertySchema propertySchema : schema.getPropertySchemas()) {
            normalized.put(propertySchema.getName(), propertySchema);
        }
        String displayName = schema.getDisplayName();
        if (displayName == null || displayName.isBlank()) {
            displayName = schema.getComment();
        }
        if (displayName == null || displayName.isBlank()) {
            displayName = schema.getValue();
        }
        return EdgeSchema.builder()
                .value(schema.getValue())
                .category(schema.getCategory())
                .displayName(displayName)
                .comment(schema.getComment())
                .propertySchemas(new ArrayList<>(normalized.values()))
                .build();
    }

    private void registerBuiltinSchemas() {
        for (EdgeType edgeType : EdgeType.values()) {
            register(edgeType);
        }
    }
}
