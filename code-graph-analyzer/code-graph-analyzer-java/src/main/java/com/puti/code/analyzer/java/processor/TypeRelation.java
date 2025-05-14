package com.puti.code.analyzer.java.processor;

import lombok.Builder;
import lombok.Value;

/**
 * 类型关系事实。
 */
@Value
@Builder
public class TypeRelation {
    String sourceType;
    String targetType;
    String relationKind;
}
