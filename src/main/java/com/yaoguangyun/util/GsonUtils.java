package com.yaoguangyun.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Gson工具类
 * 基于bbc.me应用的JSON处理实现
 * 
 * 功能特点：
 * 1. JSON序列化和反序列化
 * 2. 支持自定义格式化输出
 * 3. 支持复杂类型转换
 * 4. 提供便捷的JSON解析方法
 * 5. 支持JSON对象操作
 * 
 * 使用场景：
 * - 网络请求数据序列化
 * - 配置文件解析
 * - 数据存储和读取
 * - API数据转换
 */
public class GsonUtils {
    
    /** 默认Gson实例（紧凑格式） */
    private static final Gson GSON = new Gson();
    
    /** 格式化Gson实例（美化输出） */
    private static final Gson GSON_PRETTY = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    
    /**
     * 获取默认Gson实例
     * @return Gson实例
     */
    public static Gson getGson() {
        return GSON;
    }
    
    /**
     * 获取格式化Gson实例
     * @return 格式化Gson实例
     */
    public static Gson getPrettyGson() {
        return GSON_PRETTY;
    }
    
    /**
     * 对象转JSON字符串
     * @param obj 对象
     * @return JSON字符串
     */
    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }
    
    /**
     * 对象转格式化JSON字符串
     * @param obj 对象
     * @return 格式化JSON字符串
     */
    public static String toPrettyJson(Object obj) {
        return GSON_PRETTY.toJson(obj);
    }
    
    /**
     * JSON字符串转对象
     * @param json JSON字符串
     * @param clazz 目标类
     * @param <T> 目标类型
     * @return 对象实例
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return GSON.fromJson(json, clazz);
        } catch (JsonSyntaxException e) {
            System.err.println("JSON解析失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * JSON字符串转对象（支持泛型）
     * @param json JSON字符串
     * @param type 类型信息
     * @param <T> 目标类型
     * @return 对象实例
     */
    public static <T> T fromJson(String json, Type type) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return GSON.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            System.err.println("JSON解析失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * JSON字符串转List
     * @param json JSON字符串
     * @param clazz 元素类
     * @param <T> 元素类型
     * @return List实例
     */
    public static <T> List<T> fromJsonList(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            Type type = TypeToken.getParameterized(List.class, clazz).getType();
            return GSON.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            System.err.println("JSON解析失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * JSON字符串转Map
     * @param json JSON字符串
     * @return Map实例
     */
    public static Map<String, Object> fromJsonMap(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            return GSON.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            System.err.println("JSON解析失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * JSON字符串转JsonObject
     * @param json JSON字符串
     * @return JsonObject实例
     */
    public static JsonObject toJsonObject(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            JsonElement element = JsonParser.parseString(json);
            if (element.isJsonObject()) {
                return element.getAsJsonObject();
            }
            return null;
        } catch (Exception e) {
            System.err.println("JSON解析失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * JSON字符串转JsonElement
     * @param json JSON字符串
     * @return JsonElement实例
     */
    public static JsonElement toJsonElement(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return JsonParser.parseString(json);
        } catch (Exception e) {
            System.err.println("JSON解析失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 从JsonObject获取字符串值
     * @param jsonObject JsonObject
     * @param key 键
     * @return 字符串值，如果不存在返回null
     */
    public static String getString(JsonObject jsonObject, String key) {
        if (jsonObject == null || !jsonObject.has(key)) {
            return null;
        }
        JsonElement element = jsonObject.get(key);
        if (element.isJsonNull()) {
            return null;
        }
        return element.getAsString();
    }
    
    /**
     * 从JsonObject获取整数值
     * @param jsonObject JsonObject
     * @param key 键
     * @return 整数值，如果不存在返回0
     */
    public static int getInt(JsonObject jsonObject, String key) {
        if (jsonObject == null || !jsonObject.has(key)) {
            return 0;
        }
        JsonElement element = jsonObject.get(key);
        if (element.isJsonNull()) {
            return 0;
        }
        try {
            return element.getAsInt();
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * 从JsonObject获取长整数值
     * @param jsonObject JsonObject
     * @param key 键
     * @return 长整数值，如果不存在返回0
     */
    public static long getLong(JsonObject jsonObject, String key) {
        if (jsonObject == null || !jsonObject.has(key)) {
            return 0L;
        }
        JsonElement element = jsonObject.get(key);
        if (element.isJsonNull()) {
            return 0L;
        }
        try {
            return element.getAsLong();
        } catch (Exception e) {
            return 0L;
        }
    }
    
    /**
     * 从JsonObject获取布尔值
     * @param jsonObject JsonObject
     * @param key 键
     * @return 布尔值，如果不存在返回false
     */
    public static boolean getBoolean(JsonObject jsonObject, String key) {
        if (jsonObject == null || !jsonObject.has(key)) {
            return false;
        }
        JsonElement element = jsonObject.get(key);
        if (element.isJsonNull()) {
            return false;
        }
        return element.getAsBoolean();
    }
    
    /**
     * 从JsonObject获取双精度浮点值
     * @param jsonObject JsonObject
     * @param key 键
     * @return 双精度浮点值，如果不存在返回0
     */
    public static double getDouble(JsonObject jsonObject, String key) {
        if (jsonObject == null || !jsonObject.has(key)) {
            return 0.0;
        }
        JsonElement element = jsonObject.get(key);
        if (element.isJsonNull()) {
            return 0.0;
        }
        try {
            return element.getAsDouble();
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    /**
     * 检查JsonObject是否包含指定键
     * @param jsonObject JsonObject
     * @param key 键
     * @return 是否包含
     */
    public static boolean has(JsonObject jsonObject, String key) {
        return jsonObject != null && jsonObject.has(key) && !jsonObject.get(key).isJsonNull();
    }
    
    /**
     * 创建空的JsonObject
     * @return JsonObject实例
     */
    public static JsonObject createJsonObject() {
        return new JsonObject();
    }
    
    /**
     * 向JsonObject添加字符串值
     * @param jsonObject JsonObject
     * @param key 键
     * @param value 值
     */
    public static void putString(JsonObject jsonObject, String key, String value) {
        if (jsonObject != null) {
            jsonObject.addProperty(key, value);
        }
    }
    
    /**
     * 向JsonObject添加整数值
     * @param jsonObject JsonObject
     * @param key 键
     * @param value 值
     */
    public static void putInt(JsonObject jsonObject, String key, int value) {
        if (jsonObject != null) {
            jsonObject.addProperty(key, value);
        }
    }
    
    /**
     * 向JsonObject添加长整数值
     * @param jsonObject JsonObject
     * @param key 键
     * @param value 值
     */
    public static void putLong(JsonObject jsonObject, String key, long value) {
        if (jsonObject != null) {
            jsonObject.addProperty(key, value);
        }
    }
    
    /**
     * 向JsonObject添加布尔值
     * @param jsonObject JsonObject
     * @param key 键
     * @param value 值
     */
    public static void putBoolean(JsonObject jsonObject, String key, boolean value) {
        if (jsonObject != null) {
            jsonObject.addProperty(key, value);
        }
    }
    
    /**
     * 向JsonObject添加双精度浮点值
     * @param jsonObject JsonObject
     * @param key 键
     * @param value 值
     */
    public static void putDouble(JsonObject jsonObject, String key, double value) {
        if (jsonObject != null) {
            jsonObject.addProperty(key, value);
        }
    }
    
    /**
     * 向JsonObject添加JsonElement
     * @param jsonObject JsonObject
     * @param key 键
     * @param value 值
     */
    public static void put(JsonObject jsonObject, String key, JsonElement value) {
        if (jsonObject != null) {
            jsonObject.add(key, value);
        }
    }
    
    /**
     * 对象转Map
     * @param obj 对象
     * @return Map实例
     */
    public static Map<String, Object> toMap(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            String json = GSON.toJson(obj);
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            return GSON.fromJson(json, type);
        } catch (Exception e) {
            System.err.println("对象转Map失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Map转对象
     * @param map Map
     * @param clazz 目标类
     * @param <T> 目标类型
     * @return 对象实例
     */
    public static <T> T fromMap(Map<String, Object> map, Class<T> clazz) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        try {
            String json = GSON.toJson(map);
            return GSON.fromJson(json, clazz);
        } catch (Exception e) {
            System.err.println("Map转对象失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 验证JSON字符串是否有效
     * @param json JSON字符串
     * @return 是否有效
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.isEmpty()) {
            return false;
        }
        try {
            JsonParser.parseString(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 格式化JSON字符串
     * @param json JSON字符串
     * @return 格式化后的JSON字符串
     */
    public static String formatJson(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        try {
            JsonElement element = JsonParser.parseString(json);
            return GSON_PRETTY.toJson(element);
        } catch (Exception e) {
            return json;
        }
    }
}