package com.yaoguangyun.proto;

import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;

/**
 * 卡片协议缓冲消息类
 * 基于bbc.me应用的CardProtobufMessage实现
 * 对应原始类: bbc.me.CardProtobufMessage
 * 
 * 消息结构:
 * - basic: 基本设备信息
 * - op: 操作类型
 * - data: 数据映射
 * 
 * 使用Gson注解支持JSON序列化和反序列化
 */
public class CardProtobufMessage {
    
    // 字段号常量
    public static final int BASIC_FIELD_NUMBER = 1;
    public static final int OP_FIELD_NUMBER = 2;
    public static final int DATA_FIELD_NUMBER = 3;
    
    // 默认操作类型
    private static final CardOperationType DEFAULT_OP = CardOperationType.Verify;
    
    // 消息字段，使用SerializedName注解支持JSON字段映射
    @SerializedName("basic")
    private BasicDeviceInfo basic;          // 基本设备信息
    
    @SerializedName("op")
    private CardOperationType op;           // 操作类型
    
    @SerializedName("data")
    private Map<String, String> data;       // 数据映射
    
    /**
     * 默认构造函数
     */
    public CardProtobufMessage() {
        this.op = DEFAULT_OP;
        this.data = new HashMap<>();
    }
    
    /**
     * 构造函数
     * @param basic 基本设备信息
     * @param op 操作类型
     * @param data 数据映射
     */
    public CardProtobufMessage(BasicDeviceInfo basic, CardOperationType op, Map<String, String> data) {
        this.basic = basic;
        this.op = op != null ? op : DEFAULT_OP;
        this.data = data != null ? new HashMap<>(data) : new HashMap<>();
    }
    
    /**
     * 获取基本设备信息
     * @return BasicDeviceInfo对象
     */
    public BasicDeviceInfo getBasic() {
        return basic;
    }
    
    /**
     * 设置基本设备信息
     * @param basic 基本设备信息
     */
    public void setBasic(BasicDeviceInfo basic) {
        this.basic = basic;
    }
    
    /**
     * 检查是否有基本设备信息
     * @return 是否有值
     */
    public boolean hasBasic() {
        return basic != null;
    }
    
    /**
     * 获取操作类型
     * @return CardOperationType枚举
     */
    public CardOperationType getOp() {
        return op;
    }
    
    /**
     * 设置操作类型
     * @param op 操作类型
     */
    public void setOp(CardOperationType op) {
        this.op = op != null ? op : DEFAULT_OP;
    }
    
    /**
     * 获取操作类型的数值
     * @return 操作类型数值
     */
    public int getOpValue() {
        return op.getNumber();
    }
    
    /**
     * 设置操作类型的数值
     * @param value 操作类型数值
     */
    public void setOpValue(int value) {
        this.op = CardOperationType.valueOf(value);
    }
    
    /**
     * 获取数据映射
     * @return 数据映射
     */
    public Map<String, String> getData() {
        return data;
    }
    
    /**
     * 设置数据映射
     * @param data 数据映射
     */
    public void setData(Map<String, String> data) {
        this.data = data != null ? new HashMap<>(data) : new HashMap<>();
    }
    
    /**
     * 获取数据数量
     * @return 数据数量
     */
    public int getDataCount() {
        return data.size();
    }
    
    /**
     * 获取数据映射的副本
     * @return 数据映射副本
     */
    public Map<String, String> getDataMap() {
        return new HashMap<>(data);
    }
    
    /**
     * 检查是否包含指定键的数据
     * @param key 键
     * @return 是否包含
     */
    public boolean containsData(String key) {
        return data.containsKey(key);
    }
    
    /**
     * 获取指定键的数据，如果不存在则返回默认值
     * @param key 键
     * @param defaultValue 默认值
     * @return 数据值
     */
    public String getDataOrDefault(String key, String defaultValue) {
        return data.getOrDefault(key, defaultValue);
    }
    
    /**
     * 获取指定键的数据，如果不存在则抛出异常
     * @param key 键
     * @return 数据值
     * @throws IllegalArgumentException 如果键不存在
     */
    public String getDataOrThrow(String key) {
        String value = data.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Key not found: " + key);
        }
        return value;
    }
    
    /**
     * 添加数据
     * @param key 键
     * @param value 值
     */
    public void putData(String key, String value) {
        data.put(key, value);
    }
    
    /**
     * 移除数据
     * @param key 键
     */
    public void removeData(String key) {
        data.remove(key);
    }
    
    /**
     * 清空数据
     */
    public void clearData() {
        data.clear();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        CardProtobufMessage that = (CardProtobufMessage) obj;
        
        if (basic != null ? !basic.equals(that.basic) : that.basic != null) {
            return false;
        }
        if (op != that.op) {
            return false;
        }
        return data.equals(that.data);
    }
    
    @Override
    public int hashCode() {
        int result = basic != null ? basic.hashCode() : 0;
        result = 31 * result + op.hashCode();
        result = 31 * result + data.hashCode();
        return result;
    }
    
    @Override
    public String toString() {
        return "CardProtobufMessage{" +
                "basic=" + basic +
                ", op=" + op +
                ", data=" + data +
                '}';
    }
}