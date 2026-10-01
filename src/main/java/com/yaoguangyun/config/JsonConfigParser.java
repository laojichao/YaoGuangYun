package com.yaoguangyun.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import com.yaoguangyun.util.GsonUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * JSON配置文件解析器
 * 
 * 用于读取和解析JSON格式的配置文件
 * 支持嵌套配置、默认值、配置更新等功能
 */
public class JsonConfigParser {
    
    /** 配置文件路径 */
    private final String configFilePath;
    
    /** 配置数据 */
    private JsonObject configData;
    
    /** Gson实例（紧凑输出） */
    private final Gson gson;
    
    /** Gson实例（美化输出） */
    private final Gson prettyGson;
    
    /**
     * 构造函数
     * 
     * @param configFilePath 配置文件路径
     */
    public JsonConfigParser(String configFilePath) {
        this.configFilePath = configFilePath;
        this.gson = new GsonBuilder()
                .disableHtmlEscaping()
                .create();
        this.prettyGson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
        this.configData = new JsonObject();
    }
    
    /**
     * 加载配置文件
     *
     * <p>先按文件路径查找；找不到时回退到 classpath（打包进 jar 的 config.json），
     * 因此从任意工作目录运行都能读到随包发布的配置。</p>
     *
     * <p>文件不存在、读取失败或 JSON 非法时都返回 false，并保留原有配置数据；
     * 不会把 JsonSyntaxException / IllegalStateException 抛给调用方。</p>
     *
     * @return 是否加载成功
     */
    public boolean load() {
        File file = new File(configFilePath);
        if (file.exists()) {
            try (InputStream inputStream = new FileInputStream(file)) {
                return loadFrom(inputStream);
            } catch (IOException e) {
                System.err.println("读取配置文件失败: " + e);
                return false;
            }
        }

        // 回退：classpath 资源
        InputStream resource = JsonConfigParser.class.getClassLoader().getResourceAsStream(configFilePath);
        if (resource == null) {
            System.err.println("配置文件不存在: " + configFilePath);
            return false;
        }
        try (InputStream inputStream = resource) {
            return loadFrom(inputStream);
        } catch (IOException e) {
            System.err.println("读取配置文件失败: " + e);
            return false;
        }
    }

    /**
     * 从输入流解析配置
     *
     * @param inputStream 配置输入流
     * @return 是否解析成功
     */
    private boolean loadFrom(InputStream inputStream) {
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(reader)) {

            StringBuilder content = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                content.append(line).append("\n");
            }

            String jsonContent = content.toString().trim();
            if (jsonContent.isEmpty()) {
                System.err.println("配置文件为空: " + configFilePath);
                return false;
            }

            // 解析失败会抛未受检异常（JsonSyntaxException / IllegalStateException），
            // 必须在这里一并捕获，否则一个字符的笔误会让调用方直接崩溃
            configData = JsonParser.parseString(jsonContent).getAsJsonObject();
            return true;

        } catch (IOException | RuntimeException e) {
            System.err.println("解析配置文件失败: " + e);
            return false;
        }
    }
    
    /**
     * 保存配置到文件
     * 
     * @return 是否保存成功
     */
    public boolean save() {
        File file = new File(configFilePath);
        
        // 确保目录存在
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        try (OutputStream outputStream = new FileOutputStream(file);
             OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
             BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            
            String jsonContent = prettyGson.toJson(configData);
            bufferedWriter.write(jsonContent);
            bufferedWriter.flush();
            
            return true;
            
        } catch (IOException e) {
            System.err.println("保存配置文件失败: " + e);
            return false;
        }
    }
    
    /**
     * 获取字符串配置值
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值；键不存在或不是基本类型时返回 defaultValue
     */
    public String getString(String key, String defaultValue) {
        JsonElement element = elementFor(key);
        if (element == null || !element.isJsonPrimitive()) {
            return defaultValue;
        }
        return element.getAsString();
    }
    
    /**
     * 获取整数配置值
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值；键不存在或无法解析为整数时返回 defaultValue（而不是静默返回 0）
     */
    public int getInt(String key, int defaultValue) {
        JsonElement element = elementFor(key);
        if (element == null || !element.isJsonPrimitive()) {
            return defaultValue;
        }
        try {
            return element.getAsInt();
        } catch (NumberFormatException | UnsupportedOperationException e) {
            System.err.println("[JsonConfigParser] 配置项 " + key + " 不是合法整数，使用默认值 " + defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * 获取长整数配置值
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值；键不存在或无法解析为长整数时返回 defaultValue
     */
    public long getLong(String key, long defaultValue) {
        JsonElement element = elementFor(key);
        if (element == null || !element.isJsonPrimitive()) {
            return defaultValue;
        }
        try {
            return element.getAsLong();
        } catch (NumberFormatException | UnsupportedOperationException e) {
            System.err.println("[JsonConfigParser] 配置项 " + key + " 不是合法长整数，使用默认值 " + defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * 获取双精度浮点配置值
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值；键不存在或无法解析为浮点数时返回 defaultValue
     */
    public double getDouble(String key, double defaultValue) {
        JsonElement element = elementFor(key);
        if (element == null || !element.isJsonPrimitive()) {
            return defaultValue;
        }
        try {
            return element.getAsDouble();
        } catch (NumberFormatException | UnsupportedOperationException e) {
            System.err.println("[JsonConfigParser] 配置项 " + key + " 不是合法浮点数，使用默认值 " + defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * 获取布尔配置值
     *
     * <p>只接受真正的布尔值，或 "true"/"false" 字符串；
     * 其他取值（例如 "yes"）会返回 defaultValue，而不是被 Boolean.parseBoolean 静默判为 false。</p>
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        JsonElement element = elementFor(key);
        if (element == null || !element.isJsonPrimitive()) {
            return defaultValue;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        if (primitive.isString()) {
            String text = primitive.getAsString().trim();
            if ("true".equalsIgnoreCase(text)) {
                return true;
            }
            if ("false".equalsIgnoreCase(text)) {
                return false;
            }
        }
        System.err.println("[JsonConfigParser] 配置项 " + key + " 不是合法布尔值，使用默认值 " + defaultValue);
        return defaultValue;
    }

    /**
     * 取出键对应的 JsonElement（不存在时返回 null）
     */
    private JsonElement elementFor(String key) {
        JsonObject parent = resolveParent(key);
        return parent == null ? null : parent.get(leafKey(key));
    }
    
    /**
     * 获取嵌套配置对象
     * 
     * @param key 配置键
     * @return 配置对象
     */
    public JsonObject getJsonObject(String key) {
        JsonElement element = resolvePath(key);
        return (element != null && element.isJsonObject()) ? element.getAsJsonObject() : null;
    }
    
    /**
     * 设置字符串配置值
     * 
     * @param key 配置键
     * @param value 配置值
     */
    public void setString(String key, String value) {
        GsonUtils.putString(ensureParent(key), leafKey(key), value);
    }
    
    /**
     * 设置整数配置值
     * 
     * @param key 配置键
     * @param value 配置值
     */
    public void setInt(String key, int value) {
        GsonUtils.putInt(ensureParent(key), leafKey(key), value);
    }
    
    /**
     * 设置长整数配置值
     * 
     * @param key 配置键
     * @param value 配置值
     */
    public void setLong(String key, long value) {
        GsonUtils.putLong(ensureParent(key), leafKey(key), value);
    }
    
    /**
     * 设置双精度浮点配置值
     * 
     * @param key 配置键
     * @param value 配置值
     */
    public void setDouble(String key, double value) {
        GsonUtils.putDouble(ensureParent(key), leafKey(key), value);
    }
    
    /**
     * 设置布尔配置值
     * 
     * @param key 配置键
     * @param value 配置值
     */
    public void setBoolean(String key, boolean value) {
        GsonUtils.putBoolean(ensureParent(key), leafKey(key), value);
    }
    
    /**
     * 检查配置键是否存在
     * 
     * @param key 配置键
     * @return 是否存在
     */
    public boolean hasKey(String key) {
        JsonElement element = resolvePath(key);
        return element != null && !element.isJsonNull();
    }
    
    /**
     * 删除配置键
     * 
     * @param key 配置键
     */
    public void removeKey(String key) {
        JsonObject parent = resolveParent(key);
        if (parent != null) {
            parent.remove(leafKey(key));
        }
    }

    // ==================== 路径解析辅助方法 ====================

    /**
     * 解析点分路径（如 "server.ip"），返回键所在的对象节点
     *
     * @param key 配置键，可用"."分隔嵌套层级
     * @return 键所在的对象节点，路径不存在时返回null
     */
    private JsonObject resolveParent(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        String[] parts = key.split("\\.");
        JsonObject current = configData;
        for (int i = 0; i < parts.length - 1; i++) {
            if (!current.has(parts[i]) || !current.get(parts[i]).isJsonObject()) {
                return null;
            }
            current = current.getAsJsonObject(parts[i]);
        }
        return current;
    }

    /**
     * 解析点分路径，返回对应的JsonElement
     *
     * @param key 配置键，可用"."分隔嵌套层级
     * @return 对应的JsonElement，不存在时返回null
     */
    private JsonElement resolvePath(String key) {
        JsonObject parent = resolveParent(key);
        return parent == null ? null : parent.get(leafKey(key));
    }

    /**
     * 获取（或创建）键所在的对象节点，支持点分路径
     *
     * @param key 配置键，可用"."分隔嵌套层级
     * @return 键所在的对象节点
     */
    private JsonObject ensureParent(String key) {
        String[] parts = key.split("\\.");
        JsonObject current = configData;
        for (int i = 0; i < parts.length - 1; i++) {
            JsonElement next = current.get(parts[i]);
            if (next == null || !next.isJsonObject()) {
                JsonObject child = new JsonObject();
                current.add(parts[i], child);
                current = child;
            } else {
                current = next.getAsJsonObject();
            }
        }
        return current;
    }

    /**
     * 获取键的叶子名称（点分路径的最后一段）
     *
     * @param key 配置键
     * @return 最后一段键名
     */
    private static String leafKey(String key) {
        if (key == null || key.isEmpty()) {
            return key;
        }
        int index = key.lastIndexOf('.');
        return index >= 0 ? key.substring(index + 1) : key;
    }
    
    /**
     * 获取所有配置键
     * 
     * @return 配置键集合
     */
    public java.util.Set<String> getKeys() {
        return configData.keySet();
    }
    
    /**
     * 获取配置数据映射
     * 
     * @return 配置数据映射
     */
    public Map<String, Object> toMap() {
        Type type = new TypeToken<Map<String, Object>>(){}.getType();
        return gson.fromJson(configData, type);
    }
    
    /**
     * 从Map加载配置
     * 
     * @param map 配置数据映射
     */
    public void fromMap(Map<String, Object> map) {
        if (map == null) {
            return;
        }
        
        configData = gson.toJsonTree(map).getAsJsonObject();
    }
    
    /**
     * 合并配置
     * 
     * @param otherConfig 其他配置
     */
    public void merge(JsonConfigParser otherConfig) {
        if (otherConfig == null) {
            return;
        }
        
        JsonObject otherData = otherConfig.configData;
        for (String key : otherData.keySet()) {
            configData.add(key, otherData.get(key));
        }
    }
    
    /**
     * 获取配置文件路径
     * 
     * @return 配置文件路径
     */
    public String getConfigFilePath() {
        return configFilePath;
    }
    
    /**
     * 获取配置数据JSON字符串（紧凑格式）
     *
     * @return JSON字符串
     */
    public String toJson() {
        return gson.toJson(configData);
    }
    
    /**
     * 获取格式化的配置数据JSON字符串（美化输出）
     *
     * @return 格式化JSON字符串
     */
    public String toPrettyJson() {
        return prettyGson.toJson(configData);
    }
    
    /**
     * 从JSON字符串加载配置
     * 
     * @param json JSON字符串
     * @return 是否加载成功
     */
    public boolean fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return false;
        }
        
        try {
            configData = JsonParser.parseString(json).getAsJsonObject();
            return true;
        } catch (Exception e) {
            System.err.println("解析JSON失败: " + e);
            return false;
        }
    }
    
    /**
     * 创建默认配置文件
     * 
     * @param defaultConfig 默认配置
     * @return 是否创建成功
     */
    public static boolean createDefaultConfig(String configFilePath, JsonObject defaultConfig) {
        JsonConfigParser parser = new JsonConfigParser(configFilePath);
        parser.configData = defaultConfig;
        return parser.save();
    }
}