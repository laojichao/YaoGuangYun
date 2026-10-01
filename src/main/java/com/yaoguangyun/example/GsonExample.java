package com.yaoguangyun.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.yaoguangyun.device.DeviceInfo;
import com.yaoguangyun.proto.BasicDeviceInfo;
import com.yaoguangyun.proto.CardOperationType;
import com.yaoguangyun.proto.CardProtobufMessage;
import com.yaoguangyun.config.JsonConfigParser;
import com.yaoguangyun.network.JsonDataProcessor;
import com.yaoguangyun.util.GsonUtils;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gson使用示例类
 * 
 * 展示如何使用Gson进行JSON序列化和反序列化
 * 包含各种数据类型的处理示例
 */
public class GsonExample {
    
    /**
     * 运行所有Gson示例
     */
    public static void runExamples() {
        System.out.println("=== Gson使用示例 ===");
        
        // 1. 基本序列化和反序列化
        basicSerializationExample();
        
        // 2. 设备信息序列化
        deviceInfoExample();
        
        // 3. 协议缓冲消息序列化
        protobufMessageExample();
        
        // 4. 复杂对象序列化
        complexObjectExample();
        
        // 5. JSON对象操作
        jsonObjectExample();
        
        // 6. 集合序列化
        collectionExample();
        
        // 7. 配置文件解析
        configParserExample();
        
        // 8. JSON数据处理器
        jsonDataProcessorExample();
        
        System.out.println("=== Gson示例完成 ===\n");
    }
    
    /**
     * 基本序列化和反序列化示例
     */
    private static void basicSerializationExample() {
        System.out.println("\n--- 基本序列化示例 ---");
        
        // 创建Gson实例
        Gson gson = new Gson();
        Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
        
        // 序列化基本类型
        String json1 = gson.toJson("Hello World");
        System.out.println("字符串序列化: " + json1);
        
        String json2 = gson.toJson(12345);
        System.out.println("整数序列化: " + json2);
        
        String json3 = gson.toJson(3.14);
        System.out.println("浮点数序列化: " + json3);
        
        String json4 = gson.toJson(true);
        System.out.println("布尔值序列化: " + json4);
        
        // 反序列化基本类型
        String str = gson.fromJson("\"Hello World\"", String.class);
        System.out.println("字符串反序列化: " + str);
        
        int num = gson.fromJson("12345", int.class);
        System.out.println("整数反序列化: " + num);
        
        double dbl = gson.fromJson("3.14", double.class);
        System.out.println("浮点数反序列化: " + dbl);
        
        boolean bool = gson.fromJson("true", boolean.class);
        System.out.println("布尔值反序列化: " + bool);
        
        // 数组序列化
        int[] numbers = {1, 2, 3, 4, 5};
        String jsonArray = gson.toJson(numbers);
        System.out.println("数组序列化: " + jsonArray);
        
        int[] numbers2 = gson.fromJson(jsonArray, int[].class);
        System.out.println("数组反序列化: " + java.util.Arrays.toString(numbers2));
    }
    
    /**
     * 设备信息序列化示例
     */
    private static void deviceInfoExample() {
        System.out.println("\n--- 设备信息序列化示例 ---");
        
        // 创建设备信息
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setBrand("Samsung");
        deviceInfo.setModel("SM-G950F");
        deviceInfo.setDevice("dreamlte");
        deviceInfo.setAndroidId("123456789");
        deviceInfo.setRelease("8.0.0");
        deviceInfo.setSdkInt("26");
        deviceInfo.setWidthPixels("1440");
        deviceInfo.setHeightPixels("2960");
        deviceInfo.setNetworkType("WIFI");
        deviceInfo.setMacAddress("02:00:00:00:00:00");
        
        // 序列化为JSON
        String json = GsonUtils.toJson(deviceInfo);
        System.out.println("设备信息JSON: " + json);
        
        // 格式化输出
        String prettyJson = GsonUtils.toPrettyJson(deviceInfo);
        System.out.println("格式化设备信息JSON:\n" + prettyJson);
        
        // 反序列化
        DeviceInfo deviceInfo2 = GsonUtils.fromJson(json, DeviceInfo.class);
        System.out.println("反序列化设备信息: " + deviceInfo2.getBrand() + " " + deviceInfo2.getModel());
        
        // 从JSON字符串创建设备信息
        String jsonStr = "{\"brand\":\"Apple\",\"model\":\"iPhone 12\",\"device\":\"iPhone\",\"release\":\"14.0\"}";
        DeviceInfo appleDevice = GsonUtils.fromJson(jsonStr, DeviceInfo.class);
        System.out.println("Apple设备: " + appleDevice.getBrand() + " " + appleDevice.getModel());
    }
    
    /**
     * 协议缓冲消息序列化示例
     */
    private static void protobufMessageExample() {
        System.out.println("\n--- 协议缓冲消息序列化示例 ---");
        
        // 创建基本设备信息
        BasicDeviceInfo basicInfo = new BasicDeviceInfo();
        basicInfo.setLocale("zh");
        basicInfo.setAndroidId("123456789");
        basicInfo.setVersion(1);
        basicInfo.setStatusMachine("Samsung(SM-G950F)");
        basicInfo.setTime(System.currentTimeMillis());
        
        // 创建卡片消息
        CardProtobufMessage cardMessage = new CardProtobufMessage();
        cardMessage.setBasic(basicInfo);
        cardMessage.setOp(CardOperationType.Verify);
        cardMessage.putData("key1", "value1");
        cardMessage.putData("key2", "value2");
        
        // 序列化
        String json = GsonUtils.toJson(cardMessage);
        System.out.println("卡片消息JSON: " + json);
        
        // 格式化输出
        String prettyJson = GsonUtils.toPrettyJson(cardMessage);
        System.out.println("格式化卡片消息JSON:\n" + prettyJson);
        
        // 反序列化
        CardProtobufMessage cardMessage2 = GsonUtils.fromJson(json, CardProtobufMessage.class);
        System.out.println("反序列化卡片消息: " + cardMessage2.getBasic().getStatusMachine());
        System.out.println("操作类型: " + cardMessage2.getOp());
        System.out.println("数据映射: " + cardMessage2.getData());
    }
    
    /**
     * 复杂对象序列化示例
     */
    private static void complexObjectExample() {
        System.out.println("\n--- 复杂对象序列化示例 ---");
        
        // 创建用户对象
        User user = new User();
        user.setId(1);
        user.setName("张三");
        user.setEmail("zhangsan@example.com");
        user.setAge(25);
        user.setActive(true);
        
        // 添加地址信息
        Address address = new Address();
        address.setStreet("中关村大街1号");
        address.setCity("北京");
        address.setState("北京市");
        address.setZipCode("100080");
        user.setAddress(address);
        
        // 添加标签
        user.addTag("开发者");
        user.addTag("Java");
        user.addTag("Android");
        
        // 序列化
        String json = GsonUtils.toPrettyJson(user);
        System.out.println("用户JSON:\n" + json);
        
        // 反序列化
        User user2 = GsonUtils.fromJson(json, User.class);
        System.out.println("反序列化用户: " + user2.getName() + ", " + user2.getEmail());
        System.out.println("地址: " + user2.getAddress().getCity() + ", " + user2.getAddress().getStreet());
        System.out.println("标签: " + user2.getTags());
    }
    
    /**
     * JSON对象操作示例
     */
    private static void jsonObjectExample() {
        System.out.println("\n--- JSON对象操作示例 ---");
        
        // 创建JSON对象
        JsonObject jsonObject = GsonUtils.createJsonObject();
        GsonUtils.putString(jsonObject, "name", "李四");
        GsonUtils.putInt(jsonObject, "age", 30);
        GsonUtils.putBoolean(jsonObject, "active", true);
        GsonUtils.putDouble(jsonObject, "salary", 15000.50);
        
        // 输出JSON
        String json = GsonUtils.toPrettyJson(jsonObject);
        System.out.println("创建的JSON对象:\n" + json);
        
        // 从JSON字符串解析
        String jsonStr = "{\"name\":\"王五\",\"age\":35,\"city\":\"上海\",\"scores\":[85,90,95]}";
        JsonObject parsed = GsonUtils.toJsonObject(jsonStr);
        
        // 获取值
        String name = GsonUtils.getString(parsed, "name");
        int age = GsonUtils.getInt(parsed, "age");
        String city = GsonUtils.getString(parsed, "city");
        
        System.out.println("解析的JSON: 姓名=" + name + ", 年龄=" + age + ", 城市=" + city);
        
        // 检查字段存在
        boolean hasName = GsonUtils.has(parsed, "name");
        boolean hasEmail = GsonUtils.has(parsed, "email");
        System.out.println("包含name字段: " + hasName);
        System.out.println("包含email字段: " + hasEmail);
    }
    
    /**
     * 集合序列化示例
     */
    private static void collectionExample() {
        System.out.println("\n--- 集合序列化示例 ---");
        
        // 创建Map
        Map<String, Object> map = new HashMap<>();
        map.put("name", "赵六");
        map.put("age", 28);
        map.put("city", "深圳");
        map.put("active", true);
        
        // 序列化Map
        String mapJson = GsonUtils.toJson(map);
        System.out.println("Map JSON: " + mapJson);
        
        // 反序列化Map
        Map<String, Object> map2 = GsonUtils.fromJsonMap(mapJson);
        System.out.println("反序列化Map: " + map2);
        
        // 创建设备信息Map
        Map<String, String> deviceMap = new HashMap<>();
        deviceMap.put("brand", "Huawei");
        deviceMap.put("model", "P40");
        deviceMap.put("os", "Android");
        
        // 序列化设备Map
        String deviceJson = GsonUtils.toJson(deviceMap);
        System.out.println("设备Map JSON: " + deviceJson);
        
        // JSON验证
        String validJson = "{\"key\":\"value\"}";
        String invalidJson = "{key: value}";
        
        System.out.println("有效JSON验证(应为true): " + GsonUtils.isValidJson(validJson));
        System.out.println("无效JSON验证(应为false): " + GsonUtils.isValidJson(invalidJson));
        
        // JSON格式化
        String compactJson = "{\"name\":\"test\",\"value\":123}";
        String formatted = GsonUtils.formatJson(compactJson);
        System.out.println("格式化JSON:\n" + formatted);
    }
    
    /**
     * 配置文件解析示例
     */
    private static void configParserExample() {
        System.out.println("\n--- 配置文件解析示例 ---");
        
        // 创建配置解析器
        JsonConfigParser config = new JsonConfigParser("config.json");
        
        // 加载配置文件
        boolean loaded = config.load();
        System.out.println("配置文件加载: " + (loaded ? "成功" : "失败"));
        
        if (loaded) {
            // 读取配置值
            String serverIp = config.getString("server.ip", "localhost");
            int tcpPort = config.getInt("server.tcp_port", 5000);
            int httpPort = config.getInt("server.http_port", 5600);
            int timeout = config.getInt("server.timeout", 5000);
            
            System.out.println("服务器IP: " + serverIp);
            System.out.println("TCP端口: " + tcpPort);
            System.out.println("HTTP端口: " + httpPort);
            System.out.println("超时时间: " + timeout);
            
            // 读取设备配置
            String brand = config.getString("device.brand", "Unknown");
            String model = config.getString("device.model", "Unknown");
            String release = config.getString("device.release", "Unknown");
            
            System.out.println("设备品牌: " + brand);
            System.out.println("设备型号: " + model);
            System.out.println("系统版本: " + release);
            
            // 读取网络配置
            int connectTimeout = config.getInt("network.connect_timeout", 5000);
            int readTimeout = config.getInt("network.read_timeout", 5000);
            int bufferSize = config.getInt("network.buffer_size", 131072);
            
            System.out.println("连接超时: " + connectTimeout);
            System.out.println("读取超时: " + readTimeout);
            System.out.println("缓冲区大小: " + bufferSize);
        }
    }
    
    /**
     * JSON数据处理器示例
     */
    private static void jsonDataProcessorExample() {
        System.out.println("\n--- JSON数据处理器示例 ---");
        
        // 创建设备信息
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setBrand("Samsung");
        deviceInfo.setModel("SM-G950F");
        deviceInfo.setDevice("dreamlte");
        deviceInfo.setAndroidId("123456789");
        deviceInfo.setRelease("8.0.0");
        deviceInfo.setSdkInt("26");
        deviceInfo.setWidthPixels("1440");
        deviceInfo.setHeightPixels("2960");
        deviceInfo.setNetworkType("WIFI");
        deviceInfo.setMacAddress("02:00:00:00:00:00");
        
        // 序列化设备信息
        String deviceJson = JsonDataProcessor.serializeDeviceInfo(deviceInfo);
        System.out.println("设备信息JSON: " + deviceJson);
        
        // 构建验证请求
        Map<String, String> requestData = new HashMap<>();
        requestData.put("action", "login");
        requestData.put("timestamp", String.valueOf(System.currentTimeMillis()));
        
        String requestJson = JsonDataProcessor.buildVerifyRequest(
            deviceInfo, 
            CardOperationType.Verify, 
            requestData
        );
        System.out.println("验证请求JSON: " + requestJson);
        
        // 构建心跳请求
        String heartbeatJson = JsonDataProcessor.buildHeartbeatRequest(deviceInfo);
        System.out.println("心跳请求JSON: " + heartbeatJson);
        
        // 解析服务器响应
        String responseJson = "{\"success\":true,\"data\":{\"user_id\":\"12345\",\"token\":\"abc123\"}}";
        Map<String, Object> response = JsonDataProcessor.parseServerResponse(responseJson);
        System.out.println("服务器响应: " + response);
        
        // 构建成功响应
        Map<String, String> responseData = new HashMap<>();
        responseData.put("user_id", "12345");
        responseData.put("token", "abc123");
        
        String successResponse = JsonDataProcessor.buildSuccessResponse(responseData);
        System.out.println("成功响应JSON: " + successResponse);
        
        // 构建错误响应
        String errorResponse = JsonDataProcessor.buildErrorResponse(400, "请求参数错误");
        System.out.println("错误响应JSON: " + errorResponse);
    }
    
    /**
     * 用户类（用于示例）
     */
    static class User {
        private int id;
        private String name;
        private String email;
        private int age;
        private boolean active;
        private Address address;
        private List<String> tags;
        
        public User() {
        }
        
        public int getId() {
            return id;
        }
        
        public void setId(int id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public int getAge() {
            return age;
        }
        
        public void setAge(int age) {
            this.age = age;
        }
        
        public boolean isActive() {
            return active;
        }
        
        public void setActive(boolean active) {
            this.active = active;
        }
        
        public Address getAddress() {
            return address;
        }
        
        public void setAddress(Address address) {
            this.address = address;
        }
        
        public List<String> getTags() {
            return tags;
        }
        
        public void setTags(List<String> tags) {
            this.tags = tags;
        }
        
        public void addTag(String tag) {
            if (this.tags == null) {
                this.tags = new java.util.ArrayList<>();
            }
            this.tags.add(tag);
        }
    }
    
    /**
     * 地址类（用于示例）
     */
    static class Address {
        private String street;
        private String city;
        private String state;
        private String zipCode;
        
        public Address() {
        }
        
        public String getStreet() {
            return street;
        }
        
        public void setStreet(String street) {
            this.street = street;
        }
        
        public String getCity() {
            return city;
        }
        
        public void setCity(String city) {
            this.city = city;
        }
        
        public String getState() {
            return state;
        }
        
        public void setState(String state) {
            this.state = state;
        }
        
        public String getZipCode() {
            return zipCode;
        }
        
        public void setZipCode(String zipCode) {
            this.zipCode = zipCode;
        }
    }
}