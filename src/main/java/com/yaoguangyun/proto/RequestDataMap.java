package com.yaoguangyun.proto;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 请求数据映射类
 * 基于bbc.me应用的RequestDataMap实现
 * 对应原始类: bbc.me.RequestDataMap
 * 
 * 用于存储和处理请求数据，支持字节和字符串操作
 */
public class RequestDataMap {
    
    // 字节数据
    private byte[] data;
    
    /**
     * 默认构造函数
     */
    public RequestDataMap() {
        this.data = new byte[0];
    }
    
    /**
     * 从字节数组创建
     * @param data 字节数组
     */
    public RequestDataMap(byte[] data) {
        this.data = data != null ? data.clone() : new byte[0];
    }
    
    /**
     * 从字符串创建（UTF-8编码）
     * @param text 字符串
     */
    public RequestDataMap(String text) {
        this.data = text != null ? text.getBytes(StandardCharsets.UTF_8) : new byte[0];
    }
    
    /**
     * 创建指定大小的空数据
     * @param size 大小
     * @return RequestDataMap实例
     */
    public static RequestDataMap empty(int size) {
        return new RequestDataMap(new byte[size]);
    }
    
    /**
     * 从字符串复制创建（UTF-8编码）
     * @param text 字符串
     * @return RequestDataMap实例
     */
    public static RequestDataMap copyFromUtf8(String text) {
        return new RequestDataMap(text);
    }
    
    /**
     * 从字节数组复制创建
     * @param data 字节数组
     * @return RequestDataMap实例
     */
    public static RequestDataMap copyFrom(byte[] data) {
        return new RequestDataMap(data);
    }
    
    /**
     * 从字节数组的指定部分创建
     * @param data 字节数组
     * @param offset 偏移量
     * @param length 长度
     * @return RequestDataMap实例
     */
    public static RequestDataMap copyFrom(byte[] data, int offset, int length) {
        byte[] newData = new byte[length];
        System.arraycopy(data, offset, newData, 0, length);
        return new RequestDataMap(newData);
    }
    
    /**
     * 获取数据大小
     * @return 数据大小（字节）
     */
    public int size() {
        return data.length;
    }
    
    /**
     * 获取字节数组
     * @return 字节数组的副本
     */
    public byte[] toByteArray() {
        return data.clone();
    }
    
    /**
     * 转换为UTF-8字符串
     * @return 字符串表示
     */
    public String toStringUtf8() {
        return new String(data, StandardCharsets.UTF_8);
    }
    
    /**
     * 获取指定位置的字节
     * @param index 索引
     * @return 字节值
     */
    public byte byteAt(int index) {
        return data[index];
    }
    
    /**
     * 检查是否为空
     * @return 是否为空
     */
    public boolean isEmpty() {
        return data.length == 0;
    }
    
    /**
     * 检查是否包含指定字节序列
     * @param target 目标字节数组
     * @return 是否包含
     */
    public boolean contains(byte[] target) {
        if (target.length > data.length) {
            return false;
        }
        
        for (int i = 0; i <= data.length - target.length; i++) {
            boolean found = true;
            for (int j = 0; j < target.length; j++) {
                if (data[i + j] != target[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查是否包含指定字符串
     * @param target 目标字符串
     * @return 是否包含
     */
    public boolean contains(String target) {
        return contains(target.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * 查找字节序列的位置
     * @param target 目标字节数组
     * @param start 起始位置
     * @return 位置索引，未找到返回-1
     */
    public int indexOf(byte[] target, int start) {
        for (int i = start; i <= data.length - target.length; i++) {
            boolean found = true;
            for (int j = 0; j < target.length; j++) {
                if (data[i + j] != target[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * 查找字符串的位置
     * @param target 目标字符串
     * @return 位置索引，未找到返回-1
     */
    public int indexOf(String target) {
        return indexOf(target.getBytes(StandardCharsets.UTF_8), 0);
    }
    
    /**
     * 获取子数据
     * @param beginIndex 起始索引
     * @param endIndex 结束索引
     * @return 子数据
     */
    public RequestDataMap substring(int beginIndex, int endIndex) {
        int length = endIndex - beginIndex;
        return copyFrom(data, beginIndex, length);
    }
    
    /**
     * 连接数据
     * @param other 其他数据
     * @return 连接后的数据
     */
    public RequestDataMap concat(RequestDataMap other) {
        byte[] newData = new byte[data.length + other.data.length];
        System.arraycopy(data, 0, newData, 0, data.length);
        System.arraycopy(other.data, 0, newData, data.length, other.data.length);
        return new RequestDataMap(newData);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        RequestDataMap that = (RequestDataMap) obj;
        return Arrays.equals(data, that.data);
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }
    
    @Override
    public String toString() {
        if (data.length <= 100) {
            return "RequestDataMap{" + toStringUtf8() + '}';
        } else {
            return "RequestDataMap{size=" + data.length + ", preview=" + 
                   new String(data, 0, 100, StandardCharsets.UTF_8) + "...}";
        }
    }
}