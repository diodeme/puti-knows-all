package com.puti.code.app.query;

import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.data.ValueWrapper;
import com.puti.code.documentation.repository.graph.support.GraphEntityFormatter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Nebula 查询结果格式化服务
 * 将 Nebula 原生格式转换为 AI 友好的 JSON 格式
 */
@Slf4j
public class NebulaResultFormatter {

    /**
     * 格式化整个查询结果为 JSON 兼容的 Map
     *
     * @param resultSet Nebula 查询结果集
     * @return 格式化后的结果 Map
     */
    public static Map<String, Object> formatResultSet(ResultSet resultSet) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("succeeded", resultSet.isSucceeded());
        result.put("errorMessage", resultSet.getErrorMessage());
        result.put("columns", resultSet.getColumnNames());

        if (resultSet.isSucceeded()) {
            List<List<Object>> rows = new ArrayList<>();
            for (int i = 0; i < resultSet.rowsSize(); i++) {
                List<Object> row = new ArrayList<>();
                // Nebula ResultSet.rowValues() returns Record which implements Iterable<ValueWrapper>
                Iterable<ValueWrapper> values = resultSet.rowValues(i);
                for (ValueWrapper vw : values) {
                    row.add(parseValueWrapper(vw));
                }
                rows.add(row);
            }
            result.put("rows", rows);
            result.put("rowCount", resultSet.rowsSize());
        }

        return result;
    }

    /**
     * 解析 ValueWrapper 为可读值
     * 复用 GraphEntityFormatter.parseValueWrapper 的逻辑
     *
     * @param wrapper ValueWrapper
     * @return 可读值
     */
    private static Object parseValueWrapper(ValueWrapper wrapper) {
        return GraphEntityFormatter.parseValueWrapper(wrapper);
    }

    /**
     * 将结果转换为 JSON 字符串
     *
     * @param resultMap 格式化后的结果 Map
     * @return JSON 字符串
     */
    public static String toJsonString(Map<String, Object> resultMap) {
        return toJson(resultMap);
    }

    /**
     * 简单的 JSON 序列化方法
     */
    private static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }

        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder();
            sb.append("{");

            int i = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append("\"").append(escapeJsonString(String.valueOf(entry.getKey()))).append("\": ");
                sb.append(toJson(entry.getValue()));
                i++;
            }

            sb.append("}");
            return sb.toString();
        }

        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            StringBuilder sb = new StringBuilder();
            sb.append("[");

            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(toJson(list.get(i)));
            }

            sb.append("]");
            return sb.toString();
        }

        if (obj instanceof String) {
            return "\"" + escapeJsonString((String) obj) + "\"";
        }

        if (obj instanceof Number || obj instanceof Boolean) {
            return String.valueOf(obj);
        }

        return "\"" + escapeJsonString(String.valueOf(obj)) + "\"";
    }

    /**
     * 转义 JSON 字符串中的特殊字符
     */
    private static String escapeJsonString(String str) {
        if (str == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
}
