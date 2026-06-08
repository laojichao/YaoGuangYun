package com.yaoguangyun.network;

/**
 * TCP消息类型枚举
 * 基于bbc.me应用的TcpMessageType实现
 * 对应原始类: bbc.me.TcpMessageType
 * 
 * 用于标识TCP通信中的消息类型
 * 在TCP协议头部中使用，标识消息的用途
 */
public enum TcpMessageType {
    
    /** 未知消息类型 - 默认值 */
    TCP_MESSAGE_TYPE_UNKNOWN(0),
    
    /** 验证消息 - 用于设备验证和身份认证 */
    TCP_MESSAGE_TYPE_VERIFY(1),
    
    /** 心跳消息 - 用于保持连接活性 */
    TCP_MESSAGE_TYPE_HEARTBEAT(2),
    
    /** 数据消息 - 用于业务数据传输 */
    TCP_MESSAGE_TYPE_DATA(3);
    
    /** 消息类型数值 */
    private final int type;
    
    /**
     * 构造函数
     * @param type 消息类型数值
     */
    TcpMessageType(int type) {
        this.type = type;
    }
    
    /**
     * 获取消息类型数值
     * @return 消息类型数值
     */
    public int getType() {
        return type;
    }
    
    /**
     * 根据数值获取消息类型
     * 
     * @param type 消息类型数值
     * @return 对应的TcpMessageType枚举，如果未找到则返回TCP_MESSAGE_TYPE_UNKNOWN
     */
    public static TcpMessageType getType(int type) {
        for (TcpMessageType msgType : values()) {
            if (msgType.type == type) {
                return msgType;
            }
        }
        return TCP_MESSAGE_TYPE_UNKNOWN;
    }
}