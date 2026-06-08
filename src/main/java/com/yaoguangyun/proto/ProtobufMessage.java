package com.yaoguangyun.proto;

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
    private CardType dataType;                    // 数据类型
    private DnsMessage soft;                      // 软件消息
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
     * @return CardType枚举
     */
    public CardType getDataType() {
        return dataType;
    }
    
    /**
     * 设置数据类型
     * @param dataType 数据类型
     */
    public void setDataType(CardType dataType) {
        this.dataType = dataType != null ? dataType : CardType.Soft;
    }
    
    /**
     * 获取数据类型的数值
     * @return 数据类型数值
     */
    public int getDataTypeValue() {
        return dataType.getNumber();
    }
    
    /**
     * 设置数据类型的数值
     * @param value 数据类型数值
     */
    public void setDataTypeValue(int value) {
        this.dataType = CardType.valueOf(value);
    }
    
    /**
     * 获取软件消息
     * @return DnsMessage对象，如果不是软件类型则返回null
     */
    public DnsMessage getSoft() {
        return dataType == CardType.Soft ? soft : null;
    }
    
    /**
     * 设置软件消息
     * @param soft 软件消息
     */
    public void setSoft(DnsMessage soft) {
        this.soft = soft;
        if (soft != null) {
            this.dataType = CardType.Soft;
        }
    }
    
    /**
     * 检查是否有软件消息
     * @return 是否有软件消息
     */
    public boolean hasSoft() {
        return dataType == CardType.Soft && soft != null;
    }
    
    /**
     * 获取卡片消息
     * @return CardProtobufMessage对象，如果不是卡片类型则返回null
     */
    public CardProtobufMessage getCard() {
        return dataType == CardType.Card ? card : null;
    }
    
    /**
     * 设置卡片消息
     * @param card 卡片消息
     */
    public void setCard(CardProtobufMessage card) {
        this.card = card;
        if (card != null) {
            this.dataType = CardType.Card;
        }
    }
    
    /**
     * 检查是否有卡片消息
     * @return 是否有卡片消息
     */
    public boolean hasCard() {
        return dataType == CardType.Card && card != null;
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
        
        if (dataType != that.dataType) {
            return false;
        }
        if (dataType == CardType.Soft) {
            return soft != null ? soft.equals(that.soft) : that.soft == null;
        } else if (dataType == CardType.Card) {
            return card != null ? card.equals(that.card) : that.card == null;
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        int result = dataType.hashCode();
        if (dataType == CardType.Soft) {
            result = 31 * result + (soft != null ? soft.hashCode() : 0);
        } else if (dataType == CardType.Card) {
            result = 31 * result + (card != null ? card.hashCode() : 0);
        }
        return result;
    }
    
    @Override
    public String toString() {
        if (dataType == CardType.Soft) {
            return "ProtobufMessage{dataType=Soft, soft=" + soft + '}';
        } else if (dataType == CardType.Card) {
            return "ProtobufMessage{dataType=Card, card=" + card + '}';
        } else {
            return "ProtobufMessage{dataType=" + dataType + '}';
        }
    }
}