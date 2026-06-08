package com.yaoguangyun.proto;

/**
 * 卡片类型枚举
 * 基于bbc.me应用的CardType实现
 * 对应原始类: bbc.me.CardType
 */
public enum CardType {
    
    /**
     * 软件类型 - 用于软件相关操作
     */
    Soft(0),
    
    /**
     * 卡片类型 - 用于卡片相关操作
     */
    Card(1),
    
    /**
     * 未识别类型 - 用于处理未知类型
     */
    UNRECOGNIZED(-1);
    
    // 常量定义
    public static final int Soft_VALUE = 0;
    public static final int Card_VALUE = 1;
    
    // 枚举值
    private final int value;
    
    /**
     * 构造函数
     * @param value 枚举对应的数值
     */
    CardType(int value) {
        this.value = value;
    }
    
    /**
     * 根据数值获取对应的枚举类型
     * @param value 数值
     * @return 对应的CardType枚举，如果不存在则返回null
     */
    public static CardType forNumber(int value) {
        if (value == 0) {
            return Soft;
        }
        if (value == 1) {
            return Card;
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
     * 根据数值获取CardType，如果不存在则返回UNRECOGNIZED
     * @param value 数值
     * @return CardType枚举
     */
    public static CardType valueOf(int value) {
        CardType result = forNumber(value);
        return result != null ? result : UNRECOGNIZED;
    }
}