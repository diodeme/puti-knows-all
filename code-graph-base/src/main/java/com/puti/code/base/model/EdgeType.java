package com.puti.code.base.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 边类型枚举
 */
@Getter
public enum EdgeType implements EdgeDefinition {
    CALLS("calls", EdgeCategory.SEMANTIC, "调用关系",
            List.of(property("line_number", EdgePropertyType.INT64, "代码行号"))),                    // 普通方法调用
    OUT_CALLS("out_calls", EdgeCategory.SEMANTIC, "外部调用",
            List.of(property("line_number", EdgePropertyType.INT64, "代码行号"))),           // 外部调用
    CONTAINS("contains", EdgeCategory.STRUCTURAL, "包含关系", List.of()),             // 包含关系
    DEPENDS_ON("depends_on", EdgeCategory.SEMANTIC, "依赖关系",
            List.of(
                    property("dependency_type", EdgePropertyType.STRING, "依赖类型"),
                    property("line_number", EdgePropertyType.INT64, "代码行号")
            )),         // 依赖关系
    INSTANCE_OF("instance_of", EdgeCategory.FRAMEWORK, "实例关系",
            List.of(
                    property("line_number", EdgePropertyType.INT64, "代码行号"),
                    property("dependency_type", EdgePropertyType.STRING, "依赖类型"),
                    property("injection_mode", EdgePropertyType.STRING, "注入方式"),
                    property("resolution_kind", EdgePropertyType.STRING, "解析类型"),
                    property("resolved_type", EdgePropertyType.STRING, "解析类型名"),
                    property("requested_name", EdgePropertyType.STRING, "请求名称")
            )),       // 实例化关系
    DOCUMENTED_BY("documented_by", EdgeCategory.STRUCTURAL, "文档关系", List.of()),   // 文档关系
    IMPLEMENTED_BY("implemented_by", EdgeCategory.SEMANTIC, "实现关系", List.of()), //接口实现
    OVERRIDE("overridden_by", EdgeCategory.SEMANTIC, "重写关系", List.of()),        //方法重写
    SUPER_CALLS("super_calls", EdgeCategory.SEMANTIC, "父类调用",
            List.of(property("line_number", EdgePropertyType.INT64, "代码行号"))),       // 父类方法调用
    INTERFACE_CALLS("interface_calls", EdgeCategory.SEMANTIC, "接口调用",
            List.of(property("line_number", EdgePropertyType.INT64, "代码行号"))), // 接口方法调用
    SUBTYPE_CALLS("subtype_calls", EdgeCategory.SEMANTIC, "子类调用",
            List.of(property("line_number", EdgePropertyType.INT64, "代码行号"))),   // 子类方法调用
    INJECTION_CALLS("injection_calls", EdgeCategory.FRAMEWORK, "注入调用",
            List.of(
                    property("line_number", EdgePropertyType.INT64, "代码行号"),
                    property("injection_mode", EdgePropertyType.STRING, "注入方式"),
                    property("resolution_kind", EdgePropertyType.STRING, "解析类型"),
                    property("resolved_type", EdgePropertyType.STRING, "解析类型名"),
                    property("declared_target_type", EdgePropertyType.STRING, "声明目标类型")
            )), // 依赖注入调用

    // Data Lineage Edges
    READS_FIELD("reads_field", EdgeCategory.DATA_FLOW, "读取字段",
            List.of(property("line_number", EdgePropertyType.INT64, "代码行号"))),       // 读属性
    WRITES_FIELD("writes_field", EdgeCategory.DATA_FLOW, "写入字段",
            List.of(property("line_number", EdgePropertyType.INT64, "代码行号"))),     // 写属性
    MAPS_TO("maps_to", EdgeCategory.DATA_FLOW, "映射关系",
            List.of(property("line_number", EdgePropertyType.INT64, "代码行号"))),               // 对象映射拷贝
    PASSES_TO("passes_to", EdgeCategory.DATA_FLOW, "参数透传",
            List.of(property("line_number", EdgePropertyType.INT64, "代码行号"))),           // 参数透传

    // Dependency Graph Edges
    USES_DEPENDENCY("uses_dependency", EdgeCategory.DEPENDENCY, "项目依赖",
            List.of(
                    property("project_id", EdgePropertyType.STRING, "项目ID"),
                    property("scope", EdgePropertyType.STRING, "依赖范围"),
                    property("version", EdgePropertyType.STRING, "依赖版本")
            ));                  // 项目引用依赖库

    private final String value;
    private final EdgeCategory category;
    private final String displayName;
    private final List<EdgePropertySchema> propertySchemas;

    EdgeType(String value, EdgeCategory category, String displayName, List<EdgePropertySchema> propertySchemas) {
        this.value = value;
        this.category = category;
        this.displayName = displayName;
        this.propertySchemas = propertySchemas;
    }

    public static Optional<EdgeType> fromValue(String value) {
        return Arrays.stream(values())
                .filter(edgeType -> edgeType.value.equals(value))
                .findFirst();
    }

    private static EdgePropertySchema property(String name, EdgePropertyType type, String comment) {
        return EdgePropertySchema.builder()
                .name(name)
                .type(type)
                .comment(comment)
                .build();
    }
}
