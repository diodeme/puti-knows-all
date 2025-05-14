package com.puti.code.app.query;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 包装原始Value对象，提供类型安全的获取方法
 */
public class ValueWrapper {

    private final Object rawValue;
    private final String stringValue;
    private final ValueType valueType;

    /**
     * 值类型枚举
     */
    public enum ValueType {
        STRING,
        LONG,
        DOUBLE,
        BOOLEAN,
        DATE,
        DATETIME,
        NULL,
        VERTEX,
        EDGE,
        PATH,
        LIST,
        MAP,
        UNKNOWN
    }

    /**
     * 完整构造函数
     *
     * @param rawValue    原始Value对象
     * @param stringValue 字符串表示
     * @param valueType   值类型枚举
     */
    public ValueWrapper(Object rawValue, String stringValue, ValueType valueType) {
        this.rawValue = rawValue;
        this.stringValue = stringValue;
        this.valueType = valueType;
    }

    /**
     * 便捷构造函数，自动推断类型
     * 根据字符串值推断ValueType
     *
     * @param rawValue    原始Value对象
     * @param stringValue 字符串表示
     */
    public ValueWrapper(Object rawValue, String stringValue) {
        this.rawValue = rawValue;
        this.stringValue = stringValue;
        this.valueType = inferValueType(stringValue);
    }

    /**
     * 根据字符串值推断ValueType
     *
     * @param value 字符串值
     * @return ValueType枚举
     */
    private static ValueType inferValueType(String value) {
        if (value == null) {
            return ValueType.NULL;
        }

        String trimmed = value.trim();

        // 检查是否为NULL
        if ("NULL".equalsIgnoreCase(trimmed)) {
            return ValueType.NULL;
        }

        // 尝试解析为Long
        try {
            Long.parseLong(trimmed);
            return ValueType.LONG;
        } catch (NumberFormatException e) {
            // 不是Long，继续检查
        }

        // 尝试解析为Double
        try {
            Double.parseDouble(trimmed);
            return ValueType.DOUBLE;
        } catch (NumberFormatException e) {
            // 不是Double，继续检查
        }

        // 检查是否为Boolean
        if ("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed)) {
            return ValueType.BOOLEAN;
        }

        // 检查是否为日期格式 (YYYY-MM-DD)
        if (trimmed.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return ValueType.DATE;
        }

        // 检查是否为日期时间格式
        if (trimmed.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")) {
            return ValueType.DATETIME;
        }

        // 默认返回字符串类型
        return ValueType.STRING;
    }

    /**
     * 获取原始Value对象
     *
     * @return 原始Value对象
     */
    public Object getRawValue() {
        return rawValue;
    }

    /**
     * 获取字符串表示
     *
     * @return 字符串表示
     */
    public String getStringValue() {
        return stringValue;
    }

    /**
     * 获取值类型
     *
     * @return 值类型枚举
     */
    public ValueType getValueType() {
        return valueType;
    }

    /**
     * 获取字符串值
     *
     * @return 字符串值
     */
    public String asString() {
        return stringValue;
    }

    /**
     * 获取Long值
     *
     * @return Long值，如果无法转换则返回null
     */
    public Long asLong() {
        if (valueType == ValueType.NULL || stringValue == null) {
            return null;
        }
        try {
            return Long.parseLong(stringValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 获取Integer值
     *
     * @return Integer值，如果无法转换则返回null
     */
    public Integer asInt() {
        Long longValue = asLong();
        return longValue != null ? longValue.intValue() : null;
    }

    /**
     * 获取Double值
     *
     * @return Double值，如果无法转换则返回null
     */
    public Double asDouble() {
        if (valueType == ValueType.NULL || stringValue == null) {
            return null;
        }
        try {
            return Double.parseDouble(stringValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 获取Float值
     *
     * @return Float值，如果无法转换则返回null
     */
    public Float asFloat() {
        Double doubleValue = asDouble();
        return doubleValue != null ? doubleValue.floatValue() : null;
    }

    /**
     * 获取Boolean值
     *
     * @return Boolean值，如果无法转换则返回null
     */
    public Boolean asBoolean() {
        if (valueType == ValueType.NULL || stringValue == null) {
            return null;
        }
        return "true".equalsIgnoreCase(stringValue) || "1".equals(stringValue);
    }

    /**
     * 获取LocalDate值
     *
     * @return LocalDate值，如果无法转换则返回null
     */
    public LocalDate asDate() {
        if (valueType == ValueType.NULL || stringValue == null) {
            return null;
        }
        try {
            return LocalDate.parse(stringValue);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取LocalDateTime值
     *
     * @return LocalDateTime值，如果无法转换则返回null
     */
    public LocalDateTime asDateTime() {
        if (valueType == ValueType.NULL || stringValue == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(stringValue);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 判断是否为NULL
     *
     * @return 是否为NULL
     */
    public boolean isNull() {
        return valueType == ValueType.NULL;
    }

    /**
     * 判断是否为数字类型
     *
     * @return 是否为数字类型
     */
    public boolean isNumber() {
        return valueType == ValueType.LONG || valueType == ValueType.DOUBLE;
    }

    @Override
    public String toString() {
        return "ValueWrapper{" +
                "rawValue=" + rawValue +
                ", stringValue='" + stringValue + '\'' +
                ", valueType=" + valueType +
                '}';
    }
}
