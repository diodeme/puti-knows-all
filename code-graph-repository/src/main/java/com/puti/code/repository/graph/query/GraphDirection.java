package com.puti.code.repository.graph.query;

/**
 * 图查询方向。
 */
public enum GraphDirection {
    IN,
    OUT,
    BOTH;

    public static GraphDirection fromValue(String value) {
        if (value == null || value.isBlank()) {
            return BOTH;
        }
        return switch (value.trim().toUpperCase()) {
            case "IN" -> IN;
            case "OUT" -> OUT;
            default -> BOTH;
        };
    }
}
