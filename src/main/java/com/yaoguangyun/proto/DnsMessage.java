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
        this.properties = properties != null ? new HashMap<>(properties) : new HashMap<>();
    }
    
    /**
     * 获取属性映射
     * @return 属性映射
     */
    public Map<String, String> getProperties() {
        return properties;
    }
    
    /**
     * 设置属性映射
     * @param properties 属性映射
     */
    public void setProperties(Map<String, String> properties) {
        this.properties = properties != null ? new HashMap<>(properties) : new HashMap<>();
    }
    
    /**
     * 获取属性数量
     * @return 属性数量
     */
    public int getPropertiesCount() {
        return properties.size();
    }
    
    /**
     * 检查是否包含指定键的属性
     * @param key 键
     * @return 是否包含
     */
    public boolean containsProperty(String key) {
        return properties.containsKey(key);
    }
    
    /**
     * 获取指定键的属性值
     * @param key 键
     * @return 属性值，如果不存在则返回null
     */
    public String getProperty(String key) {
        return properties.get(key);
    }
    
    /**
     * 获取指定键的属性值，如果不存在则返回默认值
     * @param key 键
     * @param defaultValue 默认值
     * @return 属性值
     */
    public String getPropertyOrDefault(String key, String defaultValue) {
        return properties.getOrDefault(key, defaultValue);
    }
    
    /**
     * 设置属性
     * @param key 键
     * @param value 值
     */
    public void setProperty(String key, String value) {
        properties.put(key, value);
    }
    
    /**
     * 移除属性
     * @param key 键
     */
    public void removeProperty(String key) {
        properties.remove(key);
    }
    
    /**
     * 清空属性
     */
    public void clearProperties() {
        properties.clear();
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
        return properties.equals(that.properties);
    }
    
    @Override
    public int hashCode() {
        return properties.hashCode();
    }
    
    @Override
    public String toString() {
        return "DnsMessage{properties=" + properties + '}';
    }
}