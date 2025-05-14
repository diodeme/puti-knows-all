package com.puti.code.documentation.repository.graph.support;

import com.vesoft.nebula.client.graph.data.Node;
import com.vesoft.nebula.client.graph.data.Relationship;
import com.vesoft.nebula.client.graph.data.ValueWrapper;
import com.puti.code.base.model.MethodInfo;
import com.puti.code.base.model.SubgraphData;
import com.puti.code.base.util.ContentCompressor;
import com.puti.code.repository.graph.query.GraphQueryEdge;
import com.puti.code.repository.graph.query.GraphQueryNode;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@UtilityClass
public class GraphSupport {

    /**
     * * 将Vertex解析为MethodInfo
     */
    public MethodInfo parseVertexToMethodInfo(Node node) {
        try {
            String methodId = node.getId().asString();
            var properties = node.properties("function");

            String fullName = getStringProperty(properties, "full_name");
            String methodName = getStringProperty(properties, "name");

            String classFullName = null;
            if (StringUtils.isNotBlank(fullName)) {
                String[] vars = fullName.split("#");
                classFullName = vars[0];
            }
            return MethodInfo.builder()
                    .methodId(methodId)
                    .methodName(methodName)
                    .fullName(fullName)
                    .signature(fullName) // 使用fullName作为签名
                    .className(extractClassName(classFullName))
                    .packageName(extractPackageName(classFullName))
                    .visibility(getStringProperty(properties, "visibility"))
                    .isStatic(getBooleanProperty(properties, "is_static"))
                    .isAbstract(getBooleanProperty(properties, "is_abstract"))
                    .isFinal(getBooleanProperty(properties, "is_final"))
                    .isEntryPoint(getBooleanProperty(properties, "is_entry_point"))
                    .returnType(getStringProperty(properties, "return_type"))
                    .parameterTypes(getStringProperty(properties, "parameter_types"))
                    .exceptionTypes(getStringProperty(properties, "exception_types"))
                    .sourceFile(getStringProperty(properties, "source_file"))
                    .startLine(getIntegerProperty(properties, "line_start"))
                    .endLine(getIntegerProperty(properties, "line_end"))
                    .complexityScore(getIntegerProperty(properties, "complexity"))
                    .description(getStringProperty(properties, "description"))
                    .content(ContentCompressor.decompress(getStringProperty(properties, "content")))
                    .build();

        } catch (Exception e) {
            log.warn("解析Vertex为MethodInfo时发生错误", e);
            return null;
        }
    }

    public MethodInfo parseGraphQueryNodeToMethodInfo(GraphQueryNode node) {
        if (node == null) {
            return null;
        }
        return parsePropertiesToMethodInfo(node.getId(), node.getProperties());
    }

    /**
     * 将Vertex解析为动态Map，保留所有属性
     * 解决了硬编码字段会丢失新增字段的问题
     * 使用LinkedHashMap保持属性顺序，支持Schema扩展
     *
     * @param node 节点
     * @return 包含所有属性的Map
     */
    public static Map<String, Object> parseVertexToDynamicMap(Node node) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (node == null) {
            return result;
        }

        // 添加节点ID
        String nodeId;
        try {
            nodeId = node.getId().asString();
        } catch (UnsupportedEncodingException e) {
            log.warn("获取节点ID编码失败", e);
            nodeId = node.getId().toString();
        }
        result.put("id", nodeId);

        // 获取所有tag名称
        List<String> tagNames = node.tagNames();
        result.put("tags", new ArrayList<>(tagNames));

        // 遍历所有tag的所有属性，动态解析
        for (String tagName : tagNames) {
            Map<String, ValueWrapper> properties;
            try {
                properties = node.properties(tagName);
            } catch (UnsupportedEncodingException e) {
                log.warn("获取节点属性编码失败", e);
                properties = Map.of();
            } catch (Exception e) {
                log.warn("获取节点属性失败，tag: {}", tagName, e);
                properties = Map.of();
            }

            Map<String, Object> parsedProps = new LinkedHashMap<>();
            for (Map.Entry<String, ValueWrapper> entry : properties.entrySet()) {
                String key = entry.getKey();
                ValueWrapper wrapper = entry.getValue();
                parsedProps.put(key, GraphEntityFormatter.parseValueWrapper(wrapper));
            }
            result.put("properties_" + tagName, parsedProps);
        }

        return result;
    }

    private String extractClassName(String classFullName) {
        if (classFullName == null || classFullName.isEmpty()) {
            return null;
        }
        int lastDotIndex = classFullName.lastIndexOf('.');
        return classFullName.substring(lastDotIndex + 1);
    }

    private String extractPackageName(String classFullName) {
        if (classFullName == null || classFullName.isEmpty()) {
            return null;
        }
        int lastDotIndex = classFullName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return classFullName.substring(0, lastDotIndex);
        } else {
            return classFullName;
        }
    }

    private String getStringProperty(Map<String, ValueWrapper> properties, String key) throws UnsupportedEncodingException {
        ValueWrapper wrapper = properties.get(key);
        return (wrapper != null && wrapper.isString()) ? wrapper.asString() : null;
    }

    private Boolean getBooleanProperty(Map<String, ValueWrapper> properties, String key) {
        ValueWrapper wrapper = properties.get(key);
        return (wrapper != null && wrapper.isBoolean()) ? wrapper.asBoolean() : null;
    }

    private Integer getIntegerProperty(Map<String, ValueWrapper> properties, String key) {
        ValueWrapper wrapper = properties.get(key);
        return (wrapper != null && wrapper.isLong()) ? (int) wrapper.asLong() : null;
    }

    public String getEdgeStringProperty(Relationship edge, String key) {
        try {
            var properties = edge.properties();
            ValueWrapper wrapper = properties.get(key);
            return (wrapper != null && wrapper.isString()) ? wrapper.asString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public Integer getEdgeIntProperty(Relationship edge, String key) {
        try {
            var properties = edge.properties();
            ValueWrapper wrapper = properties.get(key);
            return (wrapper != null && wrapper.isLong()) ? (int) wrapper.asLong() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public SubgraphData.CallRelation parseGraphQueryEdgeToCallRelation(GraphQueryEdge edge) {
        if (edge == null) {
            return null;
        }
        Map<String, Object> properties = edge.getProperties() != null ? edge.getProperties() : Map.of();
        return SubgraphData.CallRelation.builder()
                .sourceMethodId(edge.getSource())
                .targetMethodId(edge.getTarget())
                .relationType(edge.getType())
                .lineNumber(parseInteger(properties.get("line_number")))
                .dependencyType(parseString(properties.get("dependency_type")))
                .properties(new LinkedHashMap<>(properties))
                .build();
    }

    /**
     * 将 Edge 解析为动态 Map，保留所有属性
     * 使用 LinkedHashMap 保持属性顺序，支持 Schema 扩展
     *
     * @param edge 边（Relationship）
     * @return 包含所有属性的 Map
     */
    public static Map<String, Object> parseEdgeToDynamicMap(Relationship edge) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (edge == null) {
            return result;
        }

        try {
            result.put("source", GraphEntityFormatter.parseValueWrapper(edge.srcId()));
            result.put("target", GraphEntityFormatter.parseValueWrapper(edge.dstId()));
            result.put("type", edge.edgeName());

            Map<String, ValueWrapper> properties = edge.properties();
            Map<String, Object> parsedProps = new LinkedHashMap<>();

            for (Map.Entry<String, ValueWrapper> entry : properties.entrySet()) {
                String key = entry.getKey();
                ValueWrapper wrapper = entry.getValue();
                parsedProps.put(key, GraphEntityFormatter.parseValueWrapper(wrapper));
            }
            result.put("properties", parsedProps);
        } catch (Exception e) {
            log.warn("解析边属性失败", e);
        }

        return result;
    }

    private MethodInfo parsePropertiesToMethodInfo(String methodId, Map<String, Object> properties) {
        try {
            String fullName = parseString(properties.get("full_name"));
            String methodName = parseString(properties.get("name"));
            String classFullName = null;
            if (StringUtils.isNotBlank(fullName)) {
                String[] vars = fullName.split("#");
                classFullName = vars[0];
            }
            return MethodInfo.builder()
                    .methodId(methodId)
                    .methodName(methodName)
                    .fullName(fullName)
                    .signature(fullName)
                    .className(extractClassName(classFullName))
                    .packageName(extractPackageName(classFullName))
                    .visibility(parseString(properties.get("visibility")))
                    .isStatic(parseBoolean(properties.get("is_static")))
                    .isAbstract(parseBoolean(properties.get("is_abstract")))
                    .isFinal(parseBoolean(properties.get("is_final")))
                    .isEntryPoint(parseBoolean(properties.get("is_entry_point")))
                    .returnType(parseString(properties.get("return_type")))
                    .parameterTypes(parseString(properties.get("parameter_types")))
                    .exceptionTypes(parseString(properties.get("exception_types")))
                    .sourceFile(parseString(properties.get("source_file")))
                    .startLine(parseInteger(properties.get("line_start")))
                    .endLine(parseInteger(properties.get("line_end")))
                    .complexityScore(parseInteger(properties.get("complexity")))
                    .description(parseString(properties.get("description")))
                    .content(ContentCompressor.decompress(parseString(properties.get("content"))))
                    .build();
        } catch (Exception e) {
            log.warn("解析 GraphQueryNode 为 MethodInfo 时发生错误", e);
            return null;
        }
    }

    private String parseString(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private Boolean parseBoolean(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof Boolean booleanValue ? booleanValue : Boolean.parseBoolean(String.valueOf(value));
    }

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer integerValue) {
            return integerValue;
        }
        if (value instanceof Long longValue) {
            return longValue.intValue();
        }
        if (value instanceof Double doubleValue) {
            return doubleValue.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
