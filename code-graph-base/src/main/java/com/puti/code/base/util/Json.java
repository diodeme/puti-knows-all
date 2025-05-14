package com.puti.code.base.util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * JSON工具类
 * 提供统一的JSON序列化和反序列化功能
 */
@Slf4j
public final class Json {

    /**
     * 单例Gson实例，线程安全
     */
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting() // 格式化输出
            .disableHtmlEscaping() // 禁用HTML转义
            .setLenient()
            .create();

    /**
     * 私有构造函数，防止实例化
     */
    private Json() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 将对象转换为JSON字符串
     *
     * @param object 要转换的对象
     * @return JSON字符串，转换失败返回null
     */
    public static String toJson(Object object) {
        if (object == null) {
            return null;
        }

        try {
            return GSON.toJson(object);
        } catch (Exception e) {
            log.error("Failed to convert object to JSON: {}", object.getClass().getSimpleName(), e);
            return null;
        }
    }

    /**
     * 将对象转换为JsonElement
     *
     * @param object 要转换的对象
     * @return JsonElement，转换失败返回null
     */
    public static JsonElement toJsonTree(Object object) {
        if (object == null) {
            return null;
        }

        try {
            return GSON.toJsonTree(object);
        } catch (Exception e) {
            log.error("Failed to convert object to JsonElement: {}", object.getClass().getSimpleName(), e);
            return null;
        }
    }

    /**
     * 将JSON字符串转换为指定类型的对象
     *
     * @param json  JSON字符串
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 转换后的对象，转换失败返回null
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty() || clazz == null) {
            return null;
        }

        try {
            return GSON.fromJson(json, clazz);
        } catch (JsonSyntaxException e) {
            log.error("Failed to parse JSON to {}: {}", clazz.getSimpleName(), json, e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error when parsing JSON to {}: {}", clazz.getSimpleName(), json, e);
            return null;
        }
    }

    /**
     * 将JSON字符串转换为指定类型的对象（支持泛型）
     *
     * @param json JSON字符串
     * @param type 目标类型（支持泛型）
     * @param <T>  泛型类型
     * @return 转换后的对象，转换失败返回null
     */
    public static <T> T fromJson(String json, Type type) {
        if (json == null || json.trim().isEmpty() || type == null) {
            return null;
        }

        try {
            return GSON.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            log.error("Failed to parse JSON to {}: {}", type.getTypeName(), json, e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error when parsing JSON to {}: {}", type.getTypeName(), json, e);
            return null;
        }
    }

    /**
     * 将JSON字符串转换为List
     *
     * @param json         JSON字符串
     * @param elementClass List元素的类型
     * @param <T>          泛型类型
     * @return List对象，转换失败返回null
     */
    public static <T> List<T> fromJsonToList(String json, Class<T> elementClass) {
        if (json == null || json.trim().isEmpty() || elementClass == null) {
            return null;
        }

        try {
            Type listType = TypeToken.getParameterized(List.class, elementClass).getType();
            return GSON.fromJson(json, listType);
        } catch (JsonSyntaxException e) {
            log.error("Failed to parse JSON to List<{}>: {}", elementClass.getSimpleName(), json, e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error when parsing JSON to List<{}>: {}", elementClass.getSimpleName(), json, e);
            return null;
        }
    }

    /**
     * 将JSON字符串转换为Map
     *
     * @param json JSON字符串
     * @return Map对象，转换失败返回null
     */
    public static Map<String, Object> fromJsonToMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            Type mapType = new TypeToken<Map<String, Object>>() {
            }.getType();
            return GSON.fromJson(json, mapType);
        } catch (JsonSyntaxException e) {
            log.error("Failed to parse JSON to Map: {}", json, e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error when parsing JSON to Map: {}", json, e);
            return null;
        }
    }

    /**
     * 检查字符串是否为有效的JSON格式
     *
     * @param json 要检查的字符串
     * @return true表示有效的JSON，false表示无效
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }

        try {
            GSON.fromJson(json, Object.class);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        } catch (Exception e) {
            log.warn("Unexpected error when validating JSON: {}", json, e);
            return false;
        }
    }

    /**
     * 深度复制对象（通过JSON序列化/反序列化）
     *
     * @param object 要复制的对象
     * @param clazz  对象类型
     * @param <T>    泛型类型
     * @return 复制后的对象，复制失败返回null
     */
    public static <T> T deepCopy(T object, Class<T> clazz) {
        if (object == null || clazz == null) {
            return null;
        }

        try {
            String json = GSON.toJson(object);
            return GSON.fromJson(json, clazz);
        } catch (Exception e) {
            log.error("Failed to deep copy object of type {}", clazz.getSimpleName(), e);
            return null;
        }
    }

    /**
     * 获取Gson实例（如果需要自定义操作）
     *
     * @return Gson实例
     */
    public static Gson getGson() {
        return GSON;
    }
}
