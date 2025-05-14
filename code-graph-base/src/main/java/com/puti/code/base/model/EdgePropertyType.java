package com.puti.code.base.model;

/**
 * Edge 属性类型定义，用于 Nebula schema 注册。
 */
public enum EdgePropertyType {
    STRING("string"),
    INT64("int64"),
    BOOL("bool"),
    DOUBLE("double");

    private final String nebulaType;

    EdgePropertyType(String nebulaType) {
        this.nebulaType = nebulaType;
    }

    public String getNebulaType() {
        return nebulaType;
    }
}
