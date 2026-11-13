package com.yaoguangyun.proto;

import com.google.gson.annotations.SerializedName;

/**
 * 协议缓冲消息类
 * 基于bbc.me应用的ProtobufMessage实现
 * 对应原始类: bbc.me.ProtobufMessage
 * 
 * 消息结构:
 * - dataType: 数据类型
 * - soft: 软件消息（当dataType为Soft时）
 * - card: 卡片消息（当dataType为Card时）
 */
public class ProtobufMessage {
    
    // 字段号常量
    public static final int DATA_TYPE_FIELD_NUMBER = 1;
    public static final int SOFT_FIELD_NUMBER = 2;
    public static final int CARD_FIELD_NUMBER = 3;
    
    // 消息字段
    @SerializedName("dataType")
    private CardType dataType;                    // 数据类型
    @SerializedName("soft")
    private DnsMessage soft;                      // 软件消息
    @SerializedName("card")
    private CardProtobufMessage card;             // 卡片消息
    
    /**
     * 默认构造函数
     */
    public ProtobufMessage() {
        this.dataType = CardType.Soft;
    }
    
    /**
     * 构造函数 - 软件消息
     * @param soft 软件消息
     */
    public ProtobufMessage(DnsMessage soft) {
        this.dataType = CardType.Soft;
        this.soft = soft;
    }
    
    /**
     * 构造函数 - 卡片消息
     * @param card 卡片消息
     */
    public ProtobufMessage(CardProtobufMessage card) {
        this.dataType = CardType.Card;
        this.card = card;
    }
    
    /**
     * 获取数据类型
     * @return CardType枚举（非 null）
     */
    public CardType getDataType() {
        // Gson 反序列化会调用无参构造器（dataType 默认 Soft），但不走 setter。
        // 因此 card 非空而 soft 为空时，必须按实际分支推断，否则 JSON 里的 card 不可达。
        if (dataType == CardType.Card) {
            return CardType.Card;
        }
        if (dataType == CardType.Soft) {
            return (soft == null && card != null) ? CardType.Card : CardType.Soft;
        }
        if (card != null) {
            return CardType.Card;
        }
        if (soft != null) {
            return CardType.Soft;
        }
        return CardType.Soft;
    }
    
    /**
     * 设置数据类型
     * @param dataType 数据类型
     */
    public void setDataType(CardType dataType) {
        if (dataType == CardType.UNRECOGNIZED) {
            throw new IllegalArgumentException("UNRECOGNIZED 不是合法的数据类型");
        }
        this.dataType = dataType != null ? dataType : CardType.Soft;
    }
    
    /**
     * 获取数据类型的数值
     * @return 数据类型数值
     */
    public int getDataTypeValue() {
        return getDataType().getNumber();
    }
    
    /**
     * 设置数据类型的数值
     *
     * @param value 数据类型数值
     * @throws IllegalArgumentException 数值不对应任何已知数据类型
     */
    public void setDataTypeValue(int value) {
        CardType parsed = CardType.forNumber(value);
        if (parsed == null) {
            throw new IllegalArgumentException("未知的 CardType 数值: " + value);
        }
        this.dataType = parsed;
    }
    
    /**
     * 获取软件消息
     * @return DnsMessage对象，如果不是软件类型则返回null
     */
    public DnsMessage getSoft() {
        return getDataType() == CardType.Soft ? soft : null;
    }
    
    /**
     * 设置软件消息
     * @param soft 软件消息
     */
    public void setSoft(DnsMessage soft) {
        this.soft = soft;
        if (soft != null) {
            // 保持 oneof：两个分支不能同时上线
            this.card = null;
            this.dataType = CardType.Soft;
        }
    }
    
    /**
     * 检查是否有软件消息
     * @return 是否有软件消息
     */
    public boolean hasSoft() {
        return getDataType() == CardType.Soft && soft != null;
    }
    
    /**
     * 获取卡片消息
     * @return CardProtobufMessage对象，如果不是卡片类型则返回null
     */
    public CardProtobufMessage getCard() {
        return getDataType() == CardType.Card ? card : null;
    }
    
    /**
     * 设置卡片消息
     * @param card 卡片消息
     */
    public void setCard(CardProtobufMessage card) {
        this.card = card;
        if (card != null) {
            // 保持 oneof：两个分支不能同时上线
            this.soft = null;
            this.dataType = CardType.Card;
        }
    }
    
    /**
     * 检查是否有卡片消息
     * @return 是否有卡片消息
     */
    public boolean hasCard() {
        return getDataType() == CardType.Card && card != null;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        ProtobufMessage that = (ProtobufMessage) obj;
        
        CardType thisType = getDataType();
        if (thisType != that.getDataType()) {
            return false;
        }
        if (thisType == CardType.Soft) {
            return soft != null ? soft.equals(that.soft) : that.soft == null;
        } else if (thisType == CardType.Card) {
            return card != null ? card.equals(that.card) : that.card == null;
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        CardType thisType = getDataType();
        int result = thisType.hashCode();
        if (thisType == CardType.Soft) {
            result = 31 * result + (soft != null ? soft.hashCode() : 0);
        } else if (thisType == CardType.Card) {
            result = 31 * result + (card != null ? card.hashCode() : 0);
        }
        return result;
    }
    
    @Override
    public String toString() {
        CardType thisType = getDataType();
        if (thisType == CardType.Soft) {
            return "ProtobufMessage{dataType=Soft, soft=" + soft + '}';
        } else if (thisType == CardType.Card) {
            return "ProtobufMessage{dataType=Card, card=" + card + '}';
        } else {
            return "ProtobufMessage{dataType=" + thisType + '}';
        }
    }
}