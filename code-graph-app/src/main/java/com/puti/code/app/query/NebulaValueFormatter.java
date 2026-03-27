package com.puti.code.app.query;

import com.vesoft.nebula.client.graph.data.Node;
import com.vesoft.nebula.client.graph.data.Relationship;
import com.vesoft.nebula.client.graph.data.ValueWrapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Nebula 值格式化工具，供调试查询工具将 ValueWrapper 转为可读对象。
 */
public final class NebulaValueFormatter {

    private NebulaValueFormatter() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static Object parseValueWrapper(Object value) {
        if (value instanceof ValueWrapper valueWrapper) {
            return parseValueWrapper(valueWrapper);
        }
        return value;
    }

    public static Object parseValueWrapper(ValueWrapper wrapper) {
        if (wrapper == null) {
            return null;
        }

        try {
            if (wrapper.isString()) {
                return wrapper.asString();
            }
            if (wrapper.isLong()) {
                return wrapper.asLong();
            }
            if (wrapper.isBoolean()) {
                return wrapper.asBoolean();
            }
            if (wrapper.isDouble()) {
                return wrapper.asDouble();
            }
            if (wrapper.isList()) {
                return wrapper.asList().stream()
                        .map(NebulaValueFormatter::parseValueWrapper)
                        .toList();
            }
            if (wrapper.isSet()) {
                return wrapper.asSet().stream()
                        .map(NebulaValueFormatter::parseValueWrapper)
                        .toList();
            }
            if (wrapper.isMap()) {
                Map<String, Object> result = new LinkedHashMap<>();
                wrapper.asMap().forEach((key, value) -> result.put(key, parseValueWrapper(value)));
                return result;
            }
            if (wrapper.isDate()) {
                return wrapper.asDate();
            }
            if (wrapper.isDateTime()) {
                return wrapper.asDateTime();
            }
            if (wrapper.isTime()) {
                return wrapper.asTime();
            }
            if (wrapper.isVertex()) {
                return formatNode(wrapper.asNode());
            }
            if (wrapper.isEdge()) {
                return formatEdge(wrapper.asRelationship());
            }
            if (wrapper.isPath()) {
                return wrapper.toString();
            }
            if (wrapper.isNull()) {
                return null;
            }
            return wrapper.toString();
        } catch (Exception e) {
            return wrapper.toString();
        }
    }

    private static Map<String, Object> formatNode(Node node) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (node == null) {
            return result;
        }
        result.put("id", parseValueWrapper(node.getId()));
        List<String> tagNames = node.tagNames();
        result.put("tags", tagNames);

        Map<String, Object> properties = new LinkedHashMap<>();
        for (String tagName : tagNames) {
            try {
                node.properties(tagName).forEach((key, value) -> properties.put(key, parseValueWrapper(value)));
            } catch (Exception ignored) {
            }
        }
        result.put("properties", properties);
        return result;
    }

    private static Map<String, Object> formatEdge(Relationship edge) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (edge == null) {
            return result;
        }
        result.put("source", parseValueWrapper(edge.srcId()));
        result.put("target", parseValueWrapper(edge.dstId()));
        result.put("type", edge.edgeName());

        Map<String, Object> properties = new LinkedHashMap<>();
        try {
            edge.properties().forEach((key, value) -> properties.put(key, parseValueWrapper(value)));
        } catch (Exception ignored) {
        }
        result.put("properties", properties);
        return result;
    }
}
