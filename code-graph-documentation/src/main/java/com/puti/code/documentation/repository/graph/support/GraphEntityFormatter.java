package com.puti.code.documentation.repository.graph.support;

import com.vesoft.nebula.client.graph.data.Node;
import com.vesoft.nebula.client.graph.data.Relationship;
import com.vesoft.nebula.client.graph.data.ValueWrapper;
import com.puti.code.base.model.SubgraphData;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 图谱实体格式化工具类
 * 将Nebula Graph的节点和边转换为标准JSON格式
 *
 * 核心功能：
 * 1. 自动遍历所有属性，不硬编码字段名
 * 2. 使用ValueWrapper.isString() + asString()正确解析字符串
 * 3. 支持所有Nebula类型：string, long, boolean, double, int, list等
 * 4. 使用LinkedHashMap保持属性顺序
 * 5. 输出标准JSON字符串，便于AI解析
 *
 * @author diodehe
 */
@Slf4j
@UtilityClass
public class GraphEntityFormatter {

    /**
     * 将节点转换为JSON格式的Map
     *
     * @param node 节点
     * @return JSON兼容的Map
     */
    public static Map<String, Object> formatNodeToMap(Node node) {
        if (node == null) {
            return new LinkedHashMap<>();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", parseValueWrapper(node.getId()));

        // 获取所有tag名称并处理每个tag的属性
        List<String> tagNames = node.tagNames();
        if (!tagNames.isEmpty()) {
            // 只处理第一个tag（通常是function）
            String tagName = tagNames.get(0);
            result.put("tag", tagName);

            Map<String, ValueWrapper> properties;
            try {
                properties = node.properties(tagName);
            } catch (Exception e) {
                log.warn("获取节点属性失败", e);
                properties = Map.of();
            }
            Map<String, Object> parsedProperties = new LinkedHashMap<>();

            for (Map.Entry<String, ValueWrapper> entry : properties.entrySet()) {
                String key = entry.getKey();
                ValueWrapper wrapper = entry.getValue();
                parsedProperties.put(key, parseValueWrapper(wrapper));
            }

            result.put("properties", parsedProperties);
        } else {
            result.put("tag", null);
            result.put("properties", new LinkedHashMap<>());
        }

        return result;
    }

    /**
     * 将边转换为JSON格式的Map
     *
     * @param edge 边（Relationship）
     * @return JSON兼容的Map
     */
    public static Map<String, Object> formatEdgeToMap(Relationship edge) {
        if (edge == null) {
            return new LinkedHashMap<>();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source", parseValueWrapper(edge.srcId()));
        result.put("target", parseValueWrapper(edge.dstId()));
        result.put("type", edge.edgeName());

        // 解析边的属性
        Map<String, ValueWrapper> properties;
        try {
            properties = edge.properties();
        } catch (Exception e) {
            log.warn("获取边属性失败", e);
            properties = Map.of();
        }
        Map<String, Object> parsedProperties = new LinkedHashMap<>();

        for (Map.Entry<String, ValueWrapper> entry : properties.entrySet()) {
            String key = entry.getKey();
            ValueWrapper wrapper = entry.getValue();
            parsedProperties.put(key, parseValueWrapper(wrapper));
        }

        result.put("properties", parsedProperties);

        return result;
    }

    /**
     * 将SubgraphData转换为JSON格式的Map
     *
     * @param data 子图数据
     * @return JSON兼容的Map
     */
    public static Map<String, Object> formatSubgraphToMap(SubgraphData data) {
        if (data == null) {
            return new LinkedHashMap<>();
        }

        Map<String, Object> result = new LinkedHashMap<>();

        // 转换节点列表
        List<Map<String, Object>> nodesList = new ArrayList<>();
        if (data.getMethods() != null) {
            for (Object method : data.getMethods()) {
                if (method instanceof Node) {
                    nodesList.add(formatNodeToMap((Node) method));
                } else if (method instanceof Map) {
                    nodesList.add((Map<String, Object>) method);
                }
            }
        }
        result.put("nodes", nodesList);

        // 转换边列表
        List<Map<String, Object>> edgesList = new ArrayList<>();
        if (data.getRelations() != null) {
            for (Object relation : data.getRelations()) {
                if (relation instanceof Relationship) {
                    edgesList.add(formatEdgeToMap((Relationship) relation));
                } else if (relation instanceof SubgraphData.CallRelation) {
                    SubgraphData.CallRelation callRel = (SubgraphData.CallRelation) relation;
                    Map<String, Object> edgeMap = new LinkedHashMap<>();
                    // callRel.getSourceMethodId() 返回 String 类型，直接使用
                    edgeMap.put("source", callRel.getSourceMethodId());
                    edgeMap.put("target", callRel.getTargetMethodId());
                    edgeMap.put("type", callRel.getRelationType());

                    Map<String, Object> props = new LinkedHashMap<>();
                    if (callRel.getLineNumber() != null) {
                        props.put("line_number", callRel.getLineNumber());
                    }
                    if (callRel.getDependencyType() != null) {
                        props.put("dependency_type", callRel.getDependencyType());
                    }
                    edgeMap.put("properties", props);

                    edgesList.add(edgeMap);
                } else if (relation instanceof Map) {
                    edgesList.add((Map<String, Object>) relation);
                }
            }
        }
        result.put("edges", edgesList);

        // 添加元数据
        result.put("totalNodes", nodesList.size());
        result.put("totalEdges", edgesList.size());
        result.put("maxLevel", data.getMaxLevel());
        result.put("timestamp", data.getTimestamp());

        return result;
    }

    /**
     * 将SubgraphData转换为动态格式的Map
     * 与formatSubgraphToMap的区别：
     * 1. 节点会处理所有tag的所有属性，而非只处理第一个tag
     * 2. 边的属性解析使用GraphEntityFormatter.parseValueWrapper进行类型感知转换
     * 3. 更适合Schema扩展场景，确保不丢失任何属性信息
     *
     * @param data 子图数据
     * @return JSON兼容的Map，包含所有属性的动态结构
     */
    public static Map<String, Object> formatSubgraphToDynamicMap(SubgraphData data) {
        if (data == null) {
            return new LinkedHashMap<>();
        }

        Map<String, Object> result = new LinkedHashMap<>();

        // 转换节点列表 - 处理所有tag的所有属性
        List<Map<String, Object>> nodesList = new ArrayList<>();
        if (data.getMethods() != null) {
            for (Object method : data.getMethods()) {
                if (method instanceof Node) {
                    nodesList.add(formatNodeToDynamicMap((Node) method));
                } else if (method instanceof Map) {
                    nodesList.add((Map<String, Object>) method);
                }
            }
        }
        result.put("nodes", nodesList);

        // 转换边列表 - 使用类型感知的属性解析
        List<Map<String, Object>> edgesList = new ArrayList<>();
        if (data.getRelations() != null) {
            for (Object relation : data.getRelations()) {
                if (relation instanceof Relationship) {
                    edgesList.add(formatEdgeToDynamicMap((Relationship) relation));
                } else if (relation instanceof SubgraphData.CallRelation) {
                    SubgraphData.CallRelation callRel = (SubgraphData.CallRelation) relation;
                    Map<String, Object> edgeMap = new LinkedHashMap<>();
                    // callRel.getSourceMethodId() 返回 String 类型，直接使用
                    edgeMap.put("source", callRel.getSourceMethodId());
                    edgeMap.put("target", callRel.getTargetMethodId());
                    edgeMap.put("type", callRel.getRelationType());

                    Map<String, Object> props = new LinkedHashMap<>();
                    if (callRel.getLineNumber() != null) {
                        props.put("line_number", callRel.getLineNumber());
                    }
                    if (callRel.getDependencyType() != null) {
                        props.put("dependency_type", callRel.getDependencyType());
                    }
                    edgeMap.put("properties", props);

                    edgesList.add(edgeMap);
                } else if (relation instanceof Map) {
                    edgesList.add((Map<String, Object>) relation);
                }
            }
        }
        result.put("edges", edgesList);

        // 添加元数据
        result.put("totalNodes", nodesList.size());
        result.put("totalEdges", edgesList.size());
        result.put("maxLevel", data.getMaxLevel());
        result.put("timestamp", data.getTimestamp());

        return result;
    }

    /**
     * 将节点转换为动态格式的Map
     * 处理所有tag的所有属性，使用LinkedHashMap保持顺序
     *
     * @param node 节点
     * @return JSON兼容的Map，包含所有tag的属性
     */
    public static Map<String, Object> formatNodeToDynamicMap(Node node) {
        if (node == null) {
            return new LinkedHashMap<>();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", parseValueWrapper(node.getId()));

        // 获取所有tag名称并处理每个tag的所有属性
        List<String> tagNames = node.tagNames();
        result.put("tags", tagNames);

        // 处理每个tag的属性
        Map<String, Object> allProperties = new LinkedHashMap<>();
        for (String tagName : tagNames) {
            Map<String, ValueWrapper> properties;
            try {
                properties = node.properties(tagName);
            } catch (Exception e) {
                log.warn("获取节点属性失败", e);
                properties = Map.of();
            }

            for (Map.Entry<String, ValueWrapper> entry : properties.entrySet()) {
                String key = entry.getKey();
                ValueWrapper wrapper = entry.getValue();
                allProperties.put(key, parseValueWrapper(wrapper));
            }
        }
        result.put("properties", allProperties);

        return result;
    }

    /**
     * 将边转换为动态格式的Map
     * 边的属性使用GraphEntityFormatter.parseValueWrapper进行类型感知转换
     *
     * @param edge 边（Relationship）
     * @return JSON兼容的Map
     */
    public static Map<String, Object> formatEdgeToDynamicMap(Relationship edge) {
        if (edge == null) {
            return new LinkedHashMap<>();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source", parseValueWrapper(edge.srcId()));
        result.put("target", parseValueWrapper(edge.dstId()));
        result.put("type", edge.edgeName());

        // 使用类型感知的属性解析
        Map<String, ValueWrapper> properties;
        try {
            properties = edge.properties();
        } catch (Exception e) {
            log.warn("获取边属性失败", e);
            properties = Map.of();
        }
        Map<String, Object> parsedProperties = new LinkedHashMap<>();

        for (Map.Entry<String, ValueWrapper> entry : properties.entrySet()) {
            String key = entry.getKey();
            ValueWrapper wrapper = entry.getValue();
            parsedProperties.put(key, parseValueWrapper(wrapper));
        }

        result.put("properties", parsedProperties);

        return result;
    }

    /**
     * 将SubgraphData转换为动态格式的JSON字符串
     *
     * @param data 子图数据
     * @return JSON字符串
     */
    public static String formatSubgraphToDynamicJson(SubgraphData data) {
        return toJson(formatSubgraphToDynamicMap(data));
    }

    /**
     * 将节点转换为JSON字符串
     *
     * @param node 节点
     * @return JSON字符串
     */
    public static String formatNodeToJson(Node node) {
        return toJson(formatNodeToMap(node));
    }

    /**
     * 将边转换为JSON字符串
     *
     * @param edge 边（Relationship）
     * @return JSON字符串
     */
    public static String formatEdgeToJson(Relationship edge) {
        return toJson(formatEdgeToMap(edge));
    }

    /**
     * 将SubgraphData转换为JSON字符串
     *
     * @param data 子图数据
     * @return JSON字符串
     */
    public static String formatSubgraphToJson(SubgraphData data) {
        return toJson(formatSubgraphToMap(data));
    }

    /**
     * 将节点列表转换为JSON数组
     *
     * @param nodes 节点列表
     * @return JSON字符串
     */
    public static String formatNodesToJson(List<Node> nodes) {
        List<Map<String, Object>> nodesList = nodes.stream()
                .map(GraphEntityFormatter::formatNodeToMap)
                .collect(Collectors.toList());
        return toJson(nodesList);
    }

    /**
     * 将边列表转换为JSON数组
     *
     * @param edges 边列表
     * @return JSON字符串
     */
    public static String formatEdgesToJson(List<Relationship> edges) {
        List<Map<String, Object>> edgesList = edges.stream()
                .map(GraphEntityFormatter::formatEdgeToMap)
                .collect(Collectors.toList());
        return toJson(edgesList);
    }

    /**
     * 解析Object为可读值（重载方法，支持Object类型）
     *
     * @param obj 任意对象，如果是ValueWrapper则解析，否则直接返回
     * @return 可读值
     */
    public static Object parseValueWrapper(Object obj) {
        if (obj instanceof ValueWrapper) {
            return parseValueWrapper((ValueWrapper) obj);
        }
        return obj;
    }

    /**
     * 解析ValueWrapper为可读值
     * 支持所有Nebula类型：string, long, boolean, double, int, list, map, set, date, datetime等
     *
     * @param wrapper ValueWrapper
     * @return 可读值（String, Long, Boolean, Double, List, Map等）
     */
    public static Object parseValueWrapper(ValueWrapper wrapper) {
        if (wrapper == null) {
            return null;
        }

        try {
            // String类型
            if (wrapper.isString()) {
                return wrapper.asString();
            }

            // Long类型
            if (wrapper.isLong()) {
                return wrapper.asLong();
            }

            // Boolean类型
            if (wrapper.isBoolean()) {
                return wrapper.asBoolean();
            }

            // Double类型
            if (wrapper.isDouble()) {
                return wrapper.asDouble();
            }

            // Long类型（所有整数值都用Long处理）
            if (wrapper.isLong()) {
                return wrapper.asLong();
            }

            // List类型
            if (wrapper.isList()) {
                return wrapper.asList().stream()
                        .map(GraphEntityFormatter::parseValueWrapper)
                        .collect(Collectors.toList());
            }

            // Set类型
            if (wrapper.isSet()) {
                return wrapper.asSet().stream()
                        .map(GraphEntityFormatter::parseValueWrapper)
                        .collect(Collectors.toList());
            }

            // Map类型
            if (wrapper.isMap()) {
                Map<String, ValueWrapper> map = wrapper.asMap();
                Map<String, Object> result = new LinkedHashMap<>();
                for (Map.Entry<String, ValueWrapper> entry : map.entrySet()) {
                    result.put(entry.getKey(), parseValueWrapper(entry.getValue()));
                }
                return result;
            }

            // Date类型
            if (wrapper.isDate()) {
                return wrapper.asDate();
            }

            // DateTime类型
            if (wrapper.isDateTime()) {
                return wrapper.asDateTime();
            }

            // Time类型
            if (wrapper.isTime()) {
                return wrapper.asTime();
            }

            // Vertex类型
            if (wrapper.isVertex()) {
                return formatNodeToMap(wrapper.asNode());
            }

            // Edge类型
            if (wrapper.isEdge()) {
                return formatEdgeToMap(wrapper.asRelationship());
            }

            // Path类型
            if (wrapper.isPath()) {
                // 返回路径的字符串表示
                return wrapper.toString();
            }

            // Null类型
            if (wrapper.isNull()) {
                return null;
            }

            // Unknown类型，使用toString兜底
            log.warn("Unknown ValueWrapper type, falling back to toString: {}", wrapper.getClass().getSimpleName());
            return wrapper.toString();

        } catch (Exception e) {
            log.warn("Failed to parse ValueWrapper", e);
            return wrapper.toString();
        }
    }

    /**
     * 将Map转换为JSON字符串
     * 简单的JSON序列化，不依赖外部JSON库
     *
     * @param map Map对象
     * @return JSON字符串
     */
    private static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }

        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder();
            sb.append("{");

            int i = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append("\"").append(escapeJsonString(String.valueOf(entry.getKey()))).append("\": ");
                sb.append(toJson(entry.getValue()));
                i++;
            }

            sb.append("}");
            return sb.toString();
        }

        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            StringBuilder sb = new StringBuilder();
            sb.append("[");

            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(toJson(list.get(i)));
            }

            sb.append("]");
            return sb.toString();
        }

        if (obj instanceof String) {
            return "\"" + escapeJsonString((String) obj) + "\"";
        }

        if (obj instanceof Number || obj instanceof Boolean) {
            return String.valueOf(obj);
        }

        return "\"" + escapeJsonString(String.valueOf(obj)) + "\"";
    }

    /**
     * 转义JSON字符串中的特殊字符
     *
     * @param str 原始字符串
     * @return 转义后的字符串
     */
    private static String escapeJsonString(String str) {
        if (str == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
}
