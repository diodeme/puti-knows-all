package com.puti.code.base.enums;

import java.util.Arrays;

/**
 * 图存储后端类型。
 */
public enum GraphStorageType {
    NEBULA("nebula"),
    LOCAL_FILE("local-file"),
    NEO4J("neo4j");

    private final String value;

    GraphStorageType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static GraphStorageType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return NEBULA;
        }
        String normalizedValue = normalize(value);
        return Arrays.stream(values())
                .filter(type -> normalize(type.value).equals(normalizedValue))
                .findFirst()
                .orElse(NEBULA);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase().replace("-", "").replace("_", "");
    }
}
