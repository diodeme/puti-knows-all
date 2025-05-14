package com.puti.code.base.model;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 边类
 */
@Data
public class Edge {
    /**
     * 源节点ID
     */
    private String srcId;

    /**
     * 目标节点ID
     */
    private String dstId;

    /**
     * 边类型
     */
    private EdgeDefinition type = EdgeType.CALLS;

    /**
     * 动态扩展属性。
     */
    private Map<String, Object> properties = new LinkedHashMap<>();

    public static EdgeBuilder builder() {
        return new EdgeBuilder();
    }

    public Map<String, Object> resolvedProperties() {
        Map<String, Object> resolved = new LinkedHashMap<>();
        if (properties != null) {
            resolved.putAll(properties);
        }
        return resolved;
    }

    public String getTypeName() {
        return type != null ? type.getValue() : null;
    }

    public EdgeCategory getCategory() {
        return type != null ? type.getCategory() : EdgeCategory.SEMANTIC;
    }

    public static class EdgeBuilder {
        private final Edge edge = new Edge();

        public EdgeBuilder srcId(String srcId) {
            edge.setSrcId(srcId);
            return this;
        }

        public EdgeBuilder dstId(String dstId) {
            edge.setDstId(dstId);
            return this;
        }

        public EdgeBuilder type(EdgeDefinition type) {
            edge.setType(type);
            return this;
        }

        public EdgeBuilder properties(Map<String, Object> properties) {
            Map<String, Object> merged = new LinkedHashMap<>(edge.getProperties());
            if (properties != null) {
                merged.putAll(properties);
            }
            edge.setProperties(merged);
            return this;
        }

        public EdgeBuilder property(String name, Object value) {
            if (name == null || name.isBlank() || value == null) {
                return this;
            }
            edge.getProperties().put(name, value);
            return this;
        }

        public EdgeBuilder dependencyType(DependencyType dependencyType) {
            return property("dependency_type", dependencyType != null ? dependencyType.getValue() : null);
        }

        public EdgeBuilder lineNumber(Integer lineNumber) {
            return property("line_number", lineNumber);
        }

        public Edge build() {
            Edge built = new Edge();
            built.setSrcId(edge.getSrcId());
            built.setDstId(edge.getDstId());
            built.setType(edge.getType() != null ? edge.getType() : EdgeType.CALLS);
            built.setProperties(new LinkedHashMap<>(edge.getProperties()));
            return built;
        }
    }
}
