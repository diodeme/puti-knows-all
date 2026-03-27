package com.puti.code.repository.graph.file;

import com.puti.code.base.model.EdgeCategory;
import com.puti.code.base.model.EdgePropertySchema;
import com.puti.code.base.model.EdgePropertyType;
import com.puti.code.base.model.EdgeSchemaRegistry;
import com.puti.code.repository.graph.schema.GraphSchemaManager;

import java.util.Map;

/**
 * 本地文件后端只维护逻辑 schema，不生成物理 DDL。
 */
public class LocalFileGraphSchemaManager implements GraphSchemaManager {

    private final EdgeSchemaRegistry edgeSchemaRegistry;

    public LocalFileGraphSchemaManager() {
        this(EdgeSchemaRegistry.getInstance());
    }

    public LocalFileGraphSchemaManager(EdgeSchemaRegistry edgeSchemaRegistry) {
        this.edgeSchemaRegistry = edgeSchemaRegistry;
    }

    @Override
    public void ensureRegisteredSchemas() {
        // local_file 只复用运行时 registry，无需额外物理 schema 操作。
    }

    @Override
    public void ensureEdgeSchema(String edgeType, EdgeCategory category, Map<String, Object> properties) {
        if (edgeType == null || edgeType.isBlank()) {
            return;
        }
        EdgeCategory resolvedCategory = category != null ? category : EdgeCategory.SEMANTIC;
        edgeSchemaRegistry.getOrCreate(edgeType, resolvedCategory);
        if (properties == null) {
            return;
        }
        properties.forEach((key, value) -> {
            if (key == null || key.isBlank() || value == null) {
                return;
            }
            edgeSchemaRegistry.registerProperty(edgeType, resolvedCategory, EdgePropertySchema.builder()
                    .name(key)
                    .type(inferType(value))
                    .comment(key)
                    .build());
        });
    }

    private EdgePropertyType inferType(Object value) {
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
}
