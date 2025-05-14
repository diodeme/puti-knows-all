package com.puti.code.base.model;

import lombok.Getter;

/**
 * 依赖类型枚举
 */
@Getter
public enum DependencyType {
    IMPORT("IMPORT"),
    USAGE("USAGE"),
    CALL("CALL"),
    CREATION("CREATION"),
    INHERITANCE("INHERITANCE"),
    IMPLEMENTATION("IMPLEMENTATION"),
    ASSOCIATION("ASSOCIATION"),
    INJECTION("INJECTION"),
    BEAN_DEFINITION("BEAN_DEFINITION");

    private final String value;

    DependencyType(String value) {
        this.value = value;
    }

}
