package com.puti.code.app.query;

import com.vesoft.nebula.client.graph.data.ResultSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Nebula查询结果包装类
 * 保留所有原始信息，不丢失任何数据
 */
public class NebulaQueryResult {

    private final boolean succeeded;
    private final String errorMessage;
    private final List<String> columnNames;
    private final List<QueryRow> rows;
    private final int rowCount;
    private final String rawResult;

    /**
     * 私有构造函数
     *
     * @param succeeded     查询是否成功
     * @param errorMessage  错误信息
     * @param columnNames   列名列表
     * @param rows          结果行列表
     * @param rawResult     原始结果字符串
     */
    private NebulaQueryResult(boolean succeeded, String errorMessage,
                              List<String> columnNames, List<QueryRow> rows,
                              String rawResult) {
        this.succeeded = succeeded;
        this.errorMessage = errorMessage;
        this.columnNames = columnNames != null ? new ArrayList<>(columnNames) : Collections.emptyList();
        this.rows = rows != null ? new ArrayList<>(rows) : Collections.emptyList();
        this.rowCount = this.rows.size();
        this.rawResult = rawResult != null ? rawResult : "";
    }

    /**
     * 从ResultSet创建NebulaQueryResult
     * 注意：由于不同版本的Nebula Client API存在差异，
     * 当前实现仅提取基本可用的信息
     *
     * @param resultSet ResultSet对象
     * @return NebulaQueryResult实例
     */
    public static NebulaQueryResult fromResultSet(ResultSet resultSet) {
        if (resultSet == null) {
            return new NebulaQueryResult(false, "ResultSet is null",
                    Collections.emptyList(), Collections.emptyList(), "");
        }

        boolean succeeded = resultSet.isSucceeded();
        String errorMessage = succeeded ? null : resultSet.getErrorMessage();
        List<String> columnNames = resultSet.getColumnNames();

        // 构建原始结果字符串
        StringBuilder rawResultBuilder = new StringBuilder();
        rawResultBuilder.append("Succeeded: ").append(succeeded).append("\n");
        if (!succeeded) {
            rawResultBuilder.append("Error: ").append(errorMessage).append("\n");
        }
        rawResultBuilder.append("Columns: ").append(columnNames).append("\n");

        // 由于不同版本API差异，尝试获取行数据
        List<QueryRow> rows = new ArrayList<>();

        // 尝试使用不同的方法获取行数据
        try {
            // 尝试使用 getRows 方法（某些版本存在）
            java.lang.reflect.Method getRowsMethod = resultSet.getClass().getMethod("getRows");
            Object rowsObj = getRowsMethod.invoke(resultSet);
            if (rowsObj instanceof Iterable) {
                for (Object row : (Iterable<?>) rowsObj) {
                    QueryRow queryRow = convertRowToQueryRow(row, columnNames);
                    if (queryRow != null) {
                        rows.add(queryRow);
                        rawResultBuilder.append("Row: ").append(java.util.Arrays.toString(queryRow.getRawValues())).append("\n");
                    }
                }
            }
        } catch (NoSuchMethodException e) {
            // getRows 方法不存在，尝试其他方式
            rawResultBuilder.append("Note: Row data access method not available in this API version\n");
        } catch (Exception e) {
            rawResultBuilder.append("Note: Failed to access row data: ").append(e.getMessage()).append("\n");
        }

        return new NebulaQueryResult(
                succeeded,
                errorMessage,
                columnNames,
                rows,
                rawResultBuilder.toString()
        );
    }

    /**
     * 将Row对象转换为QueryRow
     *
     * @param row         Row对象
     * @param columnNames 列名列表
     * @return QueryRow对象
     */
    private static QueryRow convertRowToQueryRow(Object row, List<String> columnNames) {
        try {
            // 尝试获取行的值
            java.lang.reflect.Method getValuesMethod = row.getClass().getMethod("getValues");
            Object valuesObj = getValuesMethod.invoke(row);

            List<ValueWrapper> wrappedValues = new ArrayList<>();
            List<String> rawValuesList = new ArrayList<>();

            if (valuesObj instanceof Iterable) {
                for (Object value : (Iterable<?>) valuesObj) {
                    // 使用类型感知的格式化逻辑，将Nebula的ValueWrapper转换为可读字符串
                    String stringValue = valueToString(value);
                    rawValuesList.add(stringValue);

                    // 创建本地ValueWrapper对象
                    wrappedValues.add(new ValueWrapper(value, stringValue));
                }
            }

            String[] rawValues = rawValuesList.toArray(new String[0]);
            return new QueryRow(wrappedValues, rawValues, columnNames);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将Value对象转换为字符串
     * 解决16进制格式问题，使用 NebulaValueFormatter 进行类型感知的格式化
     *
     * @param value Value对象（可能是Nebula的ValueWrapper）
     * @return 字符串表示
     */
    private static String valueToString(Object value) {
        if (value == null) {
            return "NULL";
        }
        try {
            // 判断是否为Nebula的ValueWrapper类型，使用类型感知的格式化逻辑
            if (value.getClass().getSimpleName().equals("ValueWrapper")) {
                // 尝试调用Nebula的ValueWrapper方法
                java.lang.reflect.Method isStringMethod = value.getClass().getMethod("isString");
                java.lang.reflect.Method asStringMethod = value.getClass().getMethod("asString");
                java.lang.reflect.Method isLongMethod = value.getClass().getMethod("isLong");
                java.lang.reflect.Method asLongMethod = value.getClass().getMethod("asLong");

                boolean isString = (Boolean) isStringMethod.invoke(value);
                if (isString) {
                    return (String) asStringMethod.invoke(value);
                }

                boolean isLong = (Boolean) isLongMethod.invoke(value);
                if (isLong) {
                    return String.valueOf(asLongMethod.invoke(value));
                }

                // 对于其他类型，使用 Nebula 值格式化器处理
                Object parsed = NebulaValueFormatter.parseValueWrapper(value);
                if (parsed != null) {
                    return parsed.toString();
                }
            }
            // 其他类型使用toString
            return value.toString();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    /**
     * 获取查询是否成功
     *
     * @return 查询是否成功
     */
    public boolean isSucceeded() {
        return succeeded;
    }

    /**
     * 获取错误信息
     *
     * @return 错误信息，如果成功则为null
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 获取列名列表
     *
     * @return 列名列表
     */
    public List<String> getColumnNames() {
        return new ArrayList<>(columnNames);
    }

    /**
     * 获取结果行列表
     *
     * @return 结果行列表
     */
    public List<QueryRow> getRows() {
        return new ArrayList<>(rows);
    }

    /**
     * 获取行数
     *
     * @return 行数
     */
    public int getRowCount() {
        return rowCount;
    }

    /**
     * 获取原始结果字符串
     *
     * @return 原始结果字符串
     */
    public String getRawResult() {
        return rawResult;
    }

    /**
     * 获取第一行
     *
     * @return 第一行，如果不存在则返回null
     */
    public QueryRow getFirstRow() {
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * 获取指定索引的行
     *
     * @param index 行索引
     * @return 指定索引的行
     */
    public QueryRow getRow(int index) {
        if (index < 0 || index >= rows.size()) {
            return null;
        }
        return rows.get(index);
    }

    /**
     * 判断是否有结果
     *
     * @return 是否有结果
     */
    public boolean hasResults() {
        return !rows.isEmpty();
    }

    @Override
    public String toString() {
        return "NebulaQueryResult{" +
                "succeeded=" + succeeded +
                ", errorMessage='" + errorMessage + '\'' +
                ", columnNames=" + columnNames +
                ", rowCount=" + rowCount +
                '}';
    }
}
