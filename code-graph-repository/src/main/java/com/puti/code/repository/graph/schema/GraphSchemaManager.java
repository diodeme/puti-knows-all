package com.puti.code.repository.graph.schema;

import com.puti.code.base.model.EdgeCategory;

import java.util.Map;

/**
 * 图 schema 管理抽象。
 */
public interface GraphSchemaManager {

    void ensureRegisteredSchemas();

    void ensureRegisteredTags();

    void ensureEdgeSchema(String edgeType, EdgeCategory category, Map<String, Object> properties);
}
