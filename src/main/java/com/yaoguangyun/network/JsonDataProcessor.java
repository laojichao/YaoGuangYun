package com.yaoguangyun.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.yaoguangyun.device.DeviceInfo;
import com.yaoguangyun.proto.BasicDeviceInfo;
import com.yaoguangyun.proto.CardOperationType;
import com.yaoguangyun.proto.CardProtobufMessage;
import com.yaoguangyun.util.GsonUtils;

import java.lang.reflect.Type;
import java.util.HashMap;
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
        BasicDeviceInfo basicInfo = new BasicDeviceInfo();
        basicInfo.setLocale("zh");
        basicInfo.setAndroidId(deviceInfo.getAndroidId());
        basicInfo.setVersion(1);
        basicInfo.setStatusMachine(deviceInfo.getBrand() + "(" + deviceInfo.getModel() + ")");
        basicInfo.setTime(System.currentTimeMillis());
        
        // 创建卡片消息
        CardProtobufMessage cardMessage = new CardProtobufMessage();
        cardMessage.setBasic(basicInfo);
        cardMessage.setOp(operationType);
        
        // 添加数据
        if (data != null) {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                cardMessage.putData(entry.getKey(), entry.getValue());
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
     * @param deviceInfo 设备信息
     * @return JSON字符串
     */
    public static String buildHeartbeatRequest(DeviceInfo deviceInfo) {
        // 创建基本设备信息
        BasicDeviceInfo basicInfo = new BasicDeviceInfo();
        basicInfo.setLocale("zh");
        basicInfo.setAndroidId(deviceInfo.getAndroidId());
        basicInfo.setVersion(1);
        basicInfo.setStatusMachine(deviceInfo.getBrand() + "(" + deviceInfo.getModel() + ")");
        basicInfo.setTime(System.currentTimeMillis());
        
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
            System.err.println("解析服务器响应失败: " + e.getMessage());
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
            System.err.println("解析设备信息失败: " + e.getMessage());
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
        
        // 添加基本设备信息
        cardMessage.putData("brand", deviceInfo.getBrand());
        cardMessage.putData("model", deviceInfo.getModel());
        cardMessage.putData("device", deviceInfo.getDevice());
        cardMessage.putData("manufacturer", deviceInfo.getManufacturer());
        
        // 添加系统信息
        cardMessage.putData("release", deviceInfo.getRelease());
        cardMessage.putData("sdk_int", deviceInfo.getSdkInt());
        cardMessage.putData("build_id", deviceInfo.getBuildID());
        
        // 添加屏幕信息
        cardMessage.putData("width_pixels", deviceInfo.getWidthPixels());
        cardMessage.putData("height_pixels", deviceInfo.getHeightPixels());
        cardMessage.putData("density", deviceInfo.getDensity());
        
        // 添加网络信息
        cardMessage.putData("mac_address", deviceInfo.getMacAddress());
        cardMessage.putData("ip_address", deviceInfo.getIpAddress());
        cardMessage.putData("network_type", deviceInfo.getNetworkType());
        
        // 添加位置信息
        if (deviceInfo.getLatitude() != null) {
            cardMessage.putData("latitude", deviceInfo.getLatitude());
        }
        if (deviceInfo.getLongitude() != null) {
            cardMessage.putData("longitude", deviceInfo.getLongitude());
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
            System.err.println("合并JSON失败: " + e.getMessage());
            return json1;
        }
    }
    
    /**
     * 提取JSON字段值
     * 
     * @param json JSON字符串
     * @param fieldName 字段名
     * @return 字段值
     */
    public static String extractField(String json, String fieldName) {
        try {
            JsonObject jsonObject = GsonUtils.toJsonObject(json);
            if (jsonObject != null && jsonObject.has(fieldName)) {
                return jsonObject.get(fieldName).getAsString();
            }
        } catch (Exception e) {
            System.err.println("提取字段失败: " + e.getMessage());
        }
        return null;
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