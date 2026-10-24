package com.yaoguangyun.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.yaoguangyun.device.DeviceInfo;
import com.yaoguangyun.proto.BasicDeviceInfo;
import com.yaoguangyun.proto.CardOperationType;
import com.yaoguangyun.proto.CardProtobufMessage;
import com.yaoguangyun.util.GsonUtils;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * JSON数据处理器
 * 
 * 用于处理网络请求中的JSON数据
 * 提供JSON序列化、反序列化和数据转换功能
 */
public class JsonDataProcessor {
    
    /** 默认Gson实例 */
    private static final Gson GSON = new Gson();
    
    /** 格式化Gson实例 */
    private static final Gson GSON_PRETTY = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    
    /**
     * 构建验证请求JSON
     * 
     * @param deviceInfo 设备信息
     * @param operationType 操作类型
     * @param data 附加数据
     * @return JSON字符串
     */
    public static String buildVerifyRequest(DeviceInfo deviceInfo, CardOperationType operationType, Map<String, String> data) {
        // 创建基本设备信息
        BasicDeviceInfo basicInfo = buildBasicDeviceInfo(deviceInfo);

        // 创建卡片消息
        CardProtobufMessage cardMessage = new CardProtobufMessage();
        cardMessage.setBasic(basicInfo);
        cardMessage.setOp(operationType);

        // 添加数据（null 值跳过：Map<String,String> 的值被 Gson 写成 JSON null 时，
        // 部分服务端反序列化器会把它当作协议错误；与 addDeviceInfoToCard 的过滤策略保持一致）
        if (data != null) {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                if (entry.getValue() != null) {
                    cardMessage.putData(entry.getKey(), entry.getValue());
                }
            }
        }

        // 添加设备信息
        addDeviceInfoToCard(cardMessage, deviceInfo);

        // 序列化为JSON
        return GSON.toJson(cardMessage);
    }

    /**
     * 构建心跳请求JSON
     *
     * <p>注意：{@link CardOperationType} 没有独立的「心跳」取值，心跳与验证共用
     * {@link CardOperationType#Verify}，两者的区别仅在于心跳不携带业务数据。
     * 若服务端需要区分心跳，应先在协议枚举中补充对应取值。</p>
     *
     * @param deviceInfo 设备信息，可为 null
     * @return JSON字符串
     */
    public static String buildHeartbeatRequest(DeviceInfo deviceInfo) {
        // 创建基本设备信息
        BasicDeviceInfo basicInfo = buildBasicDeviceInfo(deviceInfo);

        // 创建卡片消息
        CardProtobufMessage cardMessage = new CardProtobufMessage();
        cardMessage.setBasic(basicInfo);
        cardMessage.setOp(CardOperationType.Verify);

        // 添加设备信息
        addDeviceInfoToCard(cardMessage, deviceInfo);

        // 序列化为JSON
        return GSON.toJson(cardMessage);
    }

    /**
     * 构建基本设备信息，对 null 设备信息保持宽容（与 addDeviceInfoToCard 的处理一致）
     *
     * @param deviceInfo 设备信息，可为 null
     * @return BasicDeviceInfo 实例（字段非 null）
     */
    private static BasicDeviceInfo buildBasicDeviceInfo(DeviceInfo deviceInfo) {
        BasicDeviceInfo basicInfo = new BasicDeviceInfo();
        basicInfo.setLocale("zh");
        basicInfo.setVersion(1);
        basicInfo.setTime(System.currentTimeMillis());

        if (deviceInfo != null) {
            basicInfo.setAndroidId(deviceInfo.getAndroidId());
            // null 品牌/型号不能直接拼接：会产生字面量 "null(null)"（构造器把 null 归一为 ""）。
            // 品牌与型号都缺失时 statusMachine 留空，避免发送无意义的占位值。
            String brand = deviceInfo.getBrand();
            String model = deviceInfo.getModel();
            if (brand != null || model != null) {
                basicInfo.setStatusMachine((brand != null ? brand : "") + "(" + (model != null ? model : "") + ")");
            }
        }
        return basicInfo;
    }
    
    /**
     * 解析服务器响应
     * 
     * @param jsonResponse JSON响应字符串
     * @return 响应数据映射
     */
    public static Map<String, Object> parseServerResponse(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.isEmpty()) {
            return null;
        }
        
        try {
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            return GSON.fromJson(jsonResponse, type);
        } catch (Exception e) {
            // 保留异常对象，避免只留下 message 而丢失栈与根因
            System.err.println("解析服务器响应失败: " + e);
            return null;
        }
    }
    
    /**
     * 解析设备信息JSON
     * 
     * @param json 设备信息JSON字符串
     * @return DeviceInfo对象
     */
    public static DeviceInfo parseDeviceInfo(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        try {
            return GSON.fromJson(json, DeviceInfo.class);
        } catch (Exception e) {
            System.err.println("解析设备信息失败: " + e);
            return null;
        }
    }
    
    /**
     * 序列化设备信息
     * 
     * @param deviceInfo 设备信息
     * @return JSON字符串
     */
    public static String serializeDeviceInfo(DeviceInfo deviceInfo) {
        if (deviceInfo == null) {
            return "{}";
        }
        
        return GSON.toJson(deviceInfo);
    }
    
    /**
     * 序列化为格式化JSON
     * 
     * @param obj 对象
     * @return 格式化JSON字符串
     */
    public static String toPrettyJson(Object obj) {
        if (obj == null) {
            return "{}";
        }
        
        return GSON_PRETTY.toJson(obj);
    }
    
    /**
     * 构建错误响应JSON
     * 
     * @param errorCode 错误代码
     * @param errorMessage 错误消息
     * @return JSON字符串
     */
    public static String buildErrorResponse(int errorCode, String errorMessage) {
        JsonObject error = new JsonObject();
        error.addProperty("error_code", errorCode);
        error.addProperty("error_message", errorMessage);
        error.addProperty("timestamp", System.currentTimeMillis());
        
        return GSON.toJson(error);
    }
    
    /**
     * 构建成功响应JSON
     * 
     * @param data 响应数据
     * @return JSON字符串
     */
    public static String buildSuccessResponse(Object data) {
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("timestamp", System.currentTimeMillis());
        
        if (data != null) {
            response.add("data", GSON.toJsonTree(data));
        }
        
        return GSON.toJson(response);
    }
    
    /**
     * 向卡片消息添加设备信息
     * 
     * @param cardMessage 卡片消息
     * @param deviceInfo 设备信息
     */
    private static void addDeviceInfoToCard(CardProtobufMessage cardMessage, DeviceInfo deviceInfo) {
        if (deviceInfo == null) {
            return;
        }

        // 添加基本设备信息（null 值统一跳过，保持与 latitude/longitude 一致的过滤策略）
        putIfNotNull(cardMessage, "brand", deviceInfo.getBrand());
        putIfNotNull(cardMessage, "model", deviceInfo.getModel());
        putIfNotNull(cardMessage, "device", deviceInfo.getDevice());
        putIfNotNull(cardMessage, "manufacturer", deviceInfo.getManufacturer());

        // 添加系统信息
        putIfNotNull(cardMessage, "release", deviceInfo.getRelease());
        putIfNotNull(cardMessage, "sdk_int", deviceInfo.getSdkInt());
        putIfNotNull(cardMessage, "build_id", deviceInfo.getBuildID());

        // 添加屏幕信息
        putIfNotNull(cardMessage, "width_pixels", deviceInfo.getWidthPixels());
        putIfNotNull(cardMessage, "height_pixels", deviceInfo.getHeightPixels());
        putIfNotNull(cardMessage, "density", deviceInfo.getDensity());

        // 添加网络信息
        putIfNotNull(cardMessage, "mac_address", deviceInfo.getMacAddress());
        putIfNotNull(cardMessage, "ip_address", deviceInfo.getIpAddress());
        putIfNotNull(cardMessage, "network_type", deviceInfo.getNetworkType());

        // 添加位置信息
        putIfNotNull(cardMessage, "latitude", deviceInfo.getLatitude());
        putIfNotNull(cardMessage, "longitude", deviceInfo.getLongitude());
    }

    /**
     * 仅当值非 null 时写入数据映射。
     *
     * <p>写入 null 会让 Gson 在 JSON 中产出 {@code "key":null}，部分服务端反序列化器
     * 对此按协议错误处理；因此 null 字段直接省略，与缺失字段在语义上无法区分开。</p>
     */
    private static void putIfNotNull(CardProtobufMessage cardMessage, String key, String value) {
        if (value != null) {
            cardMessage.putData(key, value);
        }
    }
    
    /**
     * 验证JSON格式
     * 
     * @param json JSON字符串
     * @return 是否有效
     */
    public static boolean isValidJson(String json) {
        return GsonUtils.isValidJson(json);
    }
    
    /**
     * 合并JSON对象
     * 
     * @param json1 第一个JSON字符串
     * @param json2 第二个JSON字符串
     * @return 合并后的JSON字符串
     */
    public static String mergeJson(String json1, String json2) {
        try {
            JsonObject obj1 = GsonUtils.toJsonObject(json1);
            JsonObject obj2 = GsonUtils.toJsonObject(json2);
            
            if (obj1 == null) {
                return json2;
            }
            if (obj2 == null) {
                return json1;
            }
            
            // 合并对象
            for (String key : obj2.keySet()) {
                obj1.add(key, obj2.get(key));
            }
            
            return GSON.toJson(obj1);
        } catch (Exception e) {
            System.err.println("合并JSON失败: " + e);
            return json1 != null ? json1 : json2;
        }
    }
    
    /**
     * 提取JSON字段值
     *
     * @param json JSON字符串
     * @param fieldName 字段名
     * @return 字段值（字符串形式的原始值）；键不存在、值为 null 或值为对象/数组时返回 null
     */
    public static String extractField(String json, String fieldName) {
        JsonObject jsonObject = GsonUtils.toJsonObject(json);
        if (jsonObject == null || !jsonObject.has(fieldName)) {
            return null;
        }
        JsonElement element = jsonObject.get(fieldName);
        if (element.isJsonNull() || !element.isJsonPrimitive()) {
            // 对象/数组没有字符串表示，不是异常而是「不可提取」
            return null;
        }
        return element.getAsString();
    }
    
    /**
     * 转换为Map
     * 
     * @param obj 对象
     * @return Map实例
     */
    public static Map<String, Object> toMap(Object obj) {
        return GsonUtils.toMap(obj);
    }
    
    /**
     * 从Map创建对象
     * 
     * @param map Map实例
     * @param clazz 目标类
     * @param <T> 目标类型
     * @return 对象实例
     */
    public static <T> T fromMap(Map<String, Object> map, Class<T> clazz) {
        return GsonUtils.fromMap(map, clazz);
    }
}