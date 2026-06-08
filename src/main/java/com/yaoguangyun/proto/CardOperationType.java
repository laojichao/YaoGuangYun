package com.yaoguangyun.proto;

/**
 * 卡片操作类型枚举
 * 基于bbc.me应用的CardOperationType实现
 * 对应原始类: bbc.me.CardOperationType
 */
public enum CardOperationType {
    
    /**
     * 验证操作 - 用于设备验证
     */
    Verify(0),
    
    /**
     * 查询操作 - 用于查询信息
     */
    Query(1),
    
    /**
     * 有效操作 - 用于验证有效性
     */
    Valid(2),
    
    /**
     * 解绑操作 - 用于解绑设备
     */
    Unbind(3),
    
    /**
     * 通过操作 - 用于认证通过
     */
    Pass(4),
    
    /**
     * 未识别操作 - 用于处理未知操作
     */
    UNRECOGNIZED(-1);
    
    // 常量定义
    public static final int Verify_VALUE = 0;
    public static final int Query_VALUE = 1;
    public static final int Valid_VALUE = 2;
    public static final int Unbind_VALUE = 3;
    public static final int Pass_VALUE = 4;
    
    // 枚举值
    private final int value;
    
    /**
     * 构造函数
     * @param value 枚举对应的数值
     */
    CardOperationType(int value) {
        this.value = value;
    }
    
    /**
     * 根据数值获取对应的枚举类型
     * @param value 数值
     * @return 对应的CardOperationType枚举，如果不存在则返回null
     */
    public static CardOperationType forNumber(int value) {
        if (value == 0) {
            return Verify;
        }
        if (value == 1) {
            return Query;
        }
        if (value == 2) {
            return Valid;
        }
        if (value == 3) {
            return Unbind;
        }
        if (value == 4) {
            return Pass;
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
     * 根据数值获取CardOperationType，如果不存在则返回UNRECOGNIZED
     * @param value 数值
     * @return CardOperationType枚举
     */
    public static CardOperationType valueOf(int value) {
        CardOperationType result = forNumber(value);
        return result != null ? result : UNRECOGNIZED;
    }
}