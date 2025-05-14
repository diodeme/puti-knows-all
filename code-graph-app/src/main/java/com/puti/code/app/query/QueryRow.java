package com.puti.code.app.query;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询结果行
 * 包含值列表和原始字符串数组
 */
public class QueryRow {

    private final List<ValueWrapper> values;
    private final String[] rawValues;
    private final List<String> columnNames;

    /**
     * 构造函数
     *
     * @param values    包装后的值列表
     * @param rawValues 原始字符串值数组
     */
    public QueryRow(List<ValueWrapper> values, String[] rawValues) {
        this.values = new ArrayList<>(values);
        this.rawValues = rawValues != null ? rawValues.clone() : new String[0];
        this.columnNames = null;
    }

    /**
     * 构造函数
     *
     * @param values      包装后的值列表
     * @param rawValues   原始字符串值数组
     * @param columnNames 列名列表
     */
    public QueryRow(List<ValueWrapper> values, String[] rawValues, List<String> columnNames) {
        this.values = new ArrayList<>(values);
        this.rawValues = rawValues != null ? rawValues.clone() : new String[0];
        this.columnNames = columnNames != null ? new ArrayList<>(columnNames) : null;
    }

    /**
     * 获取值列表
     *
     * @return 值列表
     */
    public List<ValueWrapper> getValues() {
        return new ArrayList<>(values);
    }

    /**
     * 获取原始字符串值数组
     *
     * @return 原始字符串值数组
     */
    public String[] getRawValues() {
        return rawValues.clone();
    }

    /**
     * 获取列数
     *
     * @return 列数
     */
    public int getColumnCount() {
        return values.size();
    }

    /**
     * 按索引获取值
     *
     * @param index 列索引
     * @return 值包装对象
     * @throws IndexOutOfBoundsException 如果索引越界
     */
    public ValueWrapper getValue(int index) {
        if (index < 0 || index >= values.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + values.size());
        }
        return values.get(index);
    }

    /**
     * 按索引获取原始字符串值
     *
     * @param index 列索引
     * @return 原始字符串值
     * @throws IndexOutOfBoundsException 如果索引越界
     */
    public String getRawValue(int index) {
        if (index < 0 || index >= rawValues.length) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + rawValues.length);
        }
        return rawValues[index];
    }

    /**
     * 按列名获取值
     *
     * @param columnName 列名
     * @return 值包装对象，如果列名不存在则返回null
     */
    public ValueWrapper getValue(String columnName) {
        if (columnNames == null) {
            throw new IllegalStateException("Column names are not available");
        }
        int index = columnNames.indexOf(columnName);
        if (index >= 0 && index < values.size()) {
            return values.get(index);
        }
        return null;
    }

    /**
     * 按列名获取原始字符串值
     *
     * @param columnName 列名
     * @return 原始字符串值，如果列名不存在则返回null
     */
    public String getRawValue(String columnName) {
        if (columnNames == null) {
            throw new IllegalStateException("Column names are not available");
        }
        int index = columnNames.indexOf(columnName);
        if (index >= 0 && index < rawValues.length) {
            return rawValues[index];
        }
        return null;
    }

    /**
     * 判断是否包含指定列名
     *
     * @param columnName 列名
     * @return 是否包含
     */
    public boolean hasColumn(String columnName) {
        return columnNames != null && columnNames.contains(columnName);
    }

    /**
     * 获取列名列表
     *
     * @return 列名列表
     */
    public List<String> getColumnNames() {
        return columnNames != null ? new ArrayList<>(columnNames) : null;
    }

    @Override
    public String toString() {
        return "QueryRow{" +
                "values=" + values +
                ", rawValues=" + java.util.Arrays.toString(rawValues) +
                '}';
    }
}
