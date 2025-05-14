package com.puti.code.base.model;

import lombok.Builder;
import lombok.Value;

/**
 * 单个 edge 属性的 schema 定义。
 */
@Value
@Builder
public class EdgePropertySchema {
    String name;
    EdgePropertyType type;
    String comment;
}
