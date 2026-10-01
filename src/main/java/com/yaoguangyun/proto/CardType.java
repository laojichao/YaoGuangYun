package com.yaoguangyun.proto;

import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * 卡片类型枚举
 * 基于bbc.me应用的CardType实现
 * 对应原始类: bbc.me.CardType
 *
 * <p>JSON 表示：写出时使用枚举名；读取时同时接受枚举名和数值，
 * 遇到无法识别的取值会明确抛错，而不是静默返回 null。</p>
 */
@JsonAdapter(CardType.Adapter.class)
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

    /**
     * Gson 类型适配器：写出枚举名，读取时兼容枚举名与数值。
     */
    public static class Adapter extends TypeAdapter<CardType> {

        @Override
        public void write(JsonWriter out, CardType value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.name());
            }
        }

        @Override
        public CardType read(JsonReader in) throws IOException {
            JsonToken token = in.peek();
            if (token == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            if (token == JsonToken.NUMBER) {
                int number;
                try {
                    // 非整数（如 1.5）或超出 int 范围会抛 NumberFormatException。
                    // 它不是 JsonParseException，会绕过 GsonUtils 的 catch 直接逃到调用方，
                    // 因此这里统一转成 JsonSyntaxException。
                    number = in.nextInt();
                } catch (NumberFormatException | IllegalStateException e) {
                    throw new JsonSyntaxException("非法的 CardType 数值: " + e.getMessage(), e);
                }
                CardType result = forNumber(number);
                if (result == null) {
                    throw new JsonSyntaxException("未知的 CardType 数值: " + number);
                }
                return result;
            }
            String name = in.nextString();
            try {
                return CardType.valueOf(name);
            } catch (IllegalArgumentException e) {
                throw new JsonSyntaxException("未知的 CardType: " + name);
            }
        }
    }
}
