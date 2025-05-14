package com.puti.code.base.model;

import lombok.Getter;

/**
 * 节点类型枚举
 */
@Getter
public enum NodeType {
    FILE("file"),
    CLASS("class"),
    FUNCTION("function"),
    COMMENT("comment"),
    ANNOTATION("annotations"),
    MARKER_ANNOTATION("marker_annotations"),
    FIELD("field"),
    ;

    private final String value;

    NodeType(String value) {
        this.value = value;
    }

}
