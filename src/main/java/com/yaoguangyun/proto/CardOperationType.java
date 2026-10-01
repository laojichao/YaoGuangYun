package com.yaoguangyun.proto;

import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * 卡片操作类型枚举
 * 基于bbc.me应用的CardOperationType实现
 * 对应原始类: bbc.me.CardOperationType
 *
 * <p>JSON 表示：写出时使用枚举名（与既有线上格式一致）；读取时同时接受
 * 枚举名和数值（protobuf 风格），遇到无法识别的取值会明确抛错，
 * 而不是静默返回 null 导致后续 NPE。</p>
 */
@JsonAdapter(CardOperationType.Adapter.class)
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

    /**
     * Gson 类型适配器：写出枚举名，读取时兼容枚举名与数值。
     */
    public static class Adapter extends TypeAdapter<CardOperationType> {

        @Override
        public void write(JsonWriter out, CardOperationType value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.name());
            }
        }

        @Override
        public CardOperationType read(JsonReader in) throws IOException {
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
                    throw new JsonSyntaxException("非法的 CardOperationType 数值: " + e.getMessage(), e);
                }
                CardOperationType result = forNumber(number);
                if (result == null) {
                    throw new JsonSyntaxException("未知的 CardOperationType 数值: " + number);
                }
                return result;
            }
            String name = in.nextString();
            try {
                return CardOperationType.valueOf(name);
            } catch (IllegalArgumentException e) {
                throw new JsonSyntaxException("未知的 CardOperationType: " + name);
            }
        }
    }
}
