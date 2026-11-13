package com.yaoguangyun.proto;

import java.util.HashMap;
import java.util.Map;

/**
 * DNS消息类
 * 基于bbc.me应用的DnsMessage实现
 * 对应原始类: bbc.me.DnsMessage
 * 
 * 用于软件类型的消息传输
 */
public class DnsMessage {
    
    // 消息字段
    private Map<String, String> properties;  // 属性映射
    
    /**
     * 默认构造函数
     */
    public DnsMessage() {
        this.properties = new HashMap<>();
    }
    
    /**
     * 构造函数
     * @param properties 属性映射
     */
    public DnsMessage(Map<String, String> properties) {
        this.properties = copyNonNullEntries(properties);
    }

    private static Map<String, String> copyNonNullEntries(Map<String, String> source) {
        Map<String, String> copy = new HashMap<>();
        if (source != null) {
            for (Map.Entry<String, String> entry : source.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    copy.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return copy;
    }
    
    /**
     * 获取属性映射（惰性初始化，保证非 null）
     *
     * <p>Gson 反序列化会绕过构造函数与 setter 直接写字段，JSON 中的
     * {@code "properties": null} 会让字段变成 null；这里统一补齐。</p>
     *
     * @return 属性映射
     */
    public Map<String, String> getProperties() {
        if (properties == null) {
            synchronized (this) {
                if (properties == null) {
                    properties = new HashMap<>();
                }
            }
        }
        return properties;
    }
    
    /**
     * 设置属性映射
     * @param properties 属性映射
     */
    public void setProperties(Map<String, String> properties) {
        this.properties = copyNonNullEntries(properties);
    }
    
    /**
     * 获取属性数量
     * @return 属性数量
     */
    public int getPropertiesCount() {
        int count = 0;
        for (String value : getProperties().values()) {
            if (value != null) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 检查是否包含指定键的属性
     * @param key 键
     * @return 是否包含
     */
    public boolean containsProperty(String key) {
        return getProperties().get(key) != null;
    }
    
    /**
     * 获取指定键的属性值
     * @param key 键
     * @return 属性值，如果不存在则返回null
     */
    public String getProperty(String key) {
        return getProperties().get(key);
    }
    
    /**
     * 获取指定键的属性值，如果不存在则返回默认值
     * @param key 键
     * @param defaultValue 默认值
     * @return 属性值
     */
    public String getPropertyOrDefault(String key, String defaultValue) {
        String value = getProperties().get(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 设置属性
     * @param key 键
     * @param value 值
     */
    public void setProperty(String key, String value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("key/value 不能为 null；删除请使用 removeProperty");
        }
        getProperties().put(key, value);
    }
    
    /**
     * 移除属性
     * @param key 键
     */
    public void removeProperty(String key) {
        getProperties().remove(key);
    }
    
    /**
     * 清空属性
     */
    public void clearProperties() {
        getProperties().clear();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        DnsMessage that = (DnsMessage) obj;
        return getProperties().equals(that.getProperties());
    }
    
    @Override
    public int hashCode() {
        return getProperties().hashCode();
    }
    
    @Override
    public String toString() {
        return "DnsMessage{properties=" + properties + '}';
    }
}