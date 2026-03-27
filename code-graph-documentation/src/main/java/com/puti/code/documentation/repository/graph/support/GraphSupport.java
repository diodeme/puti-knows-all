package com.puti.code.documentation.repository.graph.support;

import com.puti.code.base.model.MethodInfo;
import com.puti.code.base.model.SubgraphData;
import com.puti.code.base.util.ContentCompressor;
import com.puti.code.repository.graph.query.GraphQueryEdge;
import com.puti.code.repository.graph.query.GraphQueryNode;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@UtilityClass
public class GraphSupport {

    public MethodInfo parseGraphQueryNodeToMethodInfo(GraphQueryNode node) {
        if (node == null) {
            return null;
        }
        return parsePropertiesToMethodInfo(node.getId(), node.getProperties());
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
        }
        return classFullName;
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
