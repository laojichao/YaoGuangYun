package com.yaoguangyun.proto;

/**
 * TCP消息类型枚举
 * 基于bbc.me应用的TcpMessageType实现
 * 对应原始类: bbc.me.TcpMessageType
 * 
 * 用于标识TCP通信中的消息类型
 */
public enum TcpMessageType {
    
    /**
     * 验证消息 - 用于设备验证
     */
    TCP_MESSAGE_TYPE_VERIFY(0),
    
    /**
     * 心跳消息 - 用于保持连接
     */
    TCP_MESSAGE_TYPE_HEARTBEAT(1),
    
    /**
     * 数据消息 - 用于数据传输
     */
    TCP_MESSAGE_TYPE_DATA(2),
    
    /**
     * 未识别消息 - 用于处理未知类型
     */
    UNRECOGNIZED(-1);
    
    // 常量定义
    public static final int TCP_MESSAGE_TYPE_VERIFY_VALUE = 0;
    public static final int TCP_MESSAGE_TYPE_HEARTBEAT_VALUE = 1;
    public static final int TCP_MESSAGE_TYPE_DATA_VALUE = 2;
    
    // 枚举值
    private final int value;
    
    /**
     * 构造函数
     * @param value 枚举对应的数值
     */
    TcpMessageType(int value) {
        this.value = value;
    }
    
    /**
     * 根据数值获取对应的枚举类型
     * @param value 数值
     * @return 对应的TcpMessageType枚举，如果不存在则返回null
     */
    public static TcpMessageType forNumber(int value) {
        if (value == 0) {
            return TCP_MESSAGE_TYPE_VERIFY;
        }
        if (value == 1) {
            return TCP_MESSAGE_TYPE_HEARTBEAT;
        }
        if (value == 2) {
            return TCP_MESSAGE_TYPE_DATA;
        }
        return null;
    }
    
    /**
     * 获取枚举对应的数值
     * @return 数值
     */
    public final int getNumber() {
        if (this != UNRECOGNIZED) {
            return value;
        }
        throw new IllegalArgumentException("Can't get the number of an unknown enum value.");
    }
    
    /**
     * 根据数值获取TcpMessageType，如果不存在则返回UNRECOGNIZED
     * @param value 数值
     * @return TcpMessageType枚举
     */
    public static TcpMessageType valueOf(int value) {
        TcpMessageType result = forNumber(value);
        return result != null ? result : UNRECOGNIZED;
    }
}