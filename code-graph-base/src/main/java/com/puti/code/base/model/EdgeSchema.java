package com.puti.code.base.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * Edge 的元数据定义，作为运行时 schema 注册真相。
 */
@Value
@Builder(toBuilder = true)
public class EdgeSchema implements EdgeDefinition {
    String value;
    EdgeCategory category;
    String displayName;
    String comment;
    @Singular
    List<EdgePropertySchema> propertySchemas;
}
