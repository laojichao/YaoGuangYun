package com.yaoguangyun.device;

import java.util.HashMap;
import java.util.Map;

/**
 * 设备信息管理器
 * 基于bbc.me应用的设备信息管理实现
 * 
 * 功能特点：
 * 1. 单例模式管理设备信息
 * 2. 支持设备信息收集和伪装
 * 3. 提供完整的设备参数映射
 * 4. 支持动态修改设备信息
 * 
 * 使用场景：
 * - 设备信息伪装
 * - 网络请求参数构建
 * - 设备特征分析
 * - 测试环境模拟
 */
public class DeviceInfoManager {
    
    /** 单例实例 */
    private static DeviceInfoManager instance;
    
    /** 设备信息对象 */
    private DeviceInfo deviceInfo;
    
    /** 设备信息映射表 */
    private final Map<String, String> deviceInfoMap;
    
    /**
     * 私有构造函数，防止外部实例化
     */
    private DeviceInfoManager() {
        this.deviceInfo = new DeviceInfo();
        this.deviceInfoMap = new HashMap<>();
    }
    
    /**
     * 获取单例实例
     * @return DeviceInfoManager实例
     */
    public static synchronized DeviceInfoManager getInstance() {
        if (instance == null) {
            instance = new DeviceInfoManager();
        }
        return instance;
    }
    
    /**
     * 收集设备信息
     * 
     * 在实际Android环境中，需要Context来获取真实设备信息
     * 当前为Java项目，使用模拟数据进行演示
     * 
     * 模拟数据基于Samsung Galaxy S8 (SM-G950F) 设备
     * 包含完整的设备参数，用于设备伪装功能
     * 
     * 注意：实际部署时应替换为真实的设备信息收集逻辑
     */
    public void collectDeviceInfo() {
        // ==================== 基本设备信息 ====================
        // 品牌和型号信息
        deviceInfo.setBrand("Samsung");
        deviceInfo.setModel("SM-G950F");
        deviceInfo.setDevice("dreamlte");
        deviceInfo.setProduct("dreamltexx");
        deviceInfo.setManufacturer("samsung");
        
        // 硬件信息
        deviceInfo.setBoard("universal8895");
        deviceInfo.setHardware("samsungexynos8895");
        deviceInfo.setBootloader("G950FXXU4CRI5");
        
        // 系统信息
        deviceInfo.setBuildID("R16NW");
        deviceInfo.setCodename("REL");
        deviceInfo.setDisplay("R16NW.G950FXXU4CRI5");
        deviceInfo.setFingerprint("samsung/dreamltexx/dreamlte:8.0.0/R16NW/G950FXXU4CRI5:user/release-keys");
        deviceInfo.setIncremental("G950FXXU4CRI5");
        deviceInfo.setRelease("8.0.0");
        deviceInfo.setSdk("26");
        deviceInfo.setSdkInt("26");
        deviceInfo.setTags("release-keys");
        deviceInfo.setUser("dpi");
        deviceInfo.setVersion("4");
        
        // 屏幕信息
        deviceInfo.setDensity("3.0");
        deviceInfo.setDensityDpi("480");
        deviceInfo.setHeightPixels("2960");
        deviceInfo.setWidthPixels("1440");
        deviceInfo.setScaledDensity("3.0");
        
        // 网络信息
        deviceInfo.setMacAddress("02:00:00:00:00:00");
        deviceInfo.setIpAddress("192.168.1.100");
        deviceInfo.setSsid("MyWiFi");
        deviceInfo.setBssid("00:11:22:33:44:55");
        deviceInfo.setNetworkOperator("46000");
        deviceInfo.setNetworkOperatorName("China Mobile");
        deviceInfo.setNetworkType("WIFI");
        deviceInfo.setSimOperator("46000");
        deviceInfo.setSimOperatorName("China Mobile");
        deviceInfo.setSimSerialNumber("8986012345678901234");
        deviceInfo.setSubscriberId("460001234567890");
        deviceInfo.setDeviceId("123456789012345");
        deviceInfo.setLine1Number("+8613800138000");
        
        // 位置信息
        deviceInfo.setLatitude("39.9042");
        deviceInfo.setLongitude("116.4074");
        deviceInfo.setAccuracy("10.0");
        
        // 其他信息
        deviceInfo.setHost("localhost");
        deviceInfo.setLocalHost("127.0.0.1");
        deviceInfo.setCanonicalHostName("localhost");
        deviceInfo.setHostAddress("127.0.0.1");
        deviceInfo.setHostName("localhost");
        deviceInfo.setTime(String.valueOf(System.currentTimeMillis()));
        deviceInfo.setType("WIFI");
        deviceInfo.setTypeName("WIFI");
        deviceInfo.setSubtype("0");
        deviceInfo.setSubtypeName("");
        deviceInfo.setRadioVersion("G950FXXU4CRI5");
        deviceInfo.setHeight("2960");
        deviceInfo.setWidth("1440");
        deviceInfo.setRotation("0");
        deviceInfo.setRssi("-50");
        deviceInfo.setNetworkId("1");
        deviceInfo.setProvider("gps");
        deviceInfo.setBestProvider("gps");
        deviceInfo.setReason("");
        
        // 更新设备信息映射
        updateDeviceInfoMap();
    }
    
    /**
     * 更新设备信息映射
     * 
     * 将DeviceInfo对象中的所有字段同步到Map结构中
     * 便于网络请求参数构建和数据序列化
     */
    private void updateDeviceInfoMap() {
        deviceInfoMap.clear();
        
        // ==================== 基本设备信息 ====================
        deviceInfoMap.put("androidId", deviceInfo.getAndroidId());
        deviceInfoMap.put("brand", deviceInfo.getBrand());
        deviceInfoMap.put("model", deviceInfo.getModel());
        deviceInfoMap.put("device", deviceInfo.getDevice());
        deviceInfoMap.put("product", deviceInfo.getProduct());
        deviceInfoMap.put("manufacturer", deviceInfo.getManufacturer());
        deviceInfoMap.put("board", deviceInfo.getBoard());
        deviceInfoMap.put("hardware", deviceInfo.getHardware());
        deviceInfoMap.put("bootloader", deviceInfo.getBootloader());
        deviceInfoMap.put("serial", deviceInfo.getSerial());
        deviceInfoMap.put("buildID", deviceInfo.getBuildID());
        deviceInfoMap.put("codename", deviceInfo.getCodename());
        deviceInfoMap.put("display", deviceInfo.getDisplay());
        deviceInfoMap.put("fingerprint", deviceInfo.getFingerprint());
        deviceInfoMap.put("incremental", deviceInfo.getIncremental());
        deviceInfoMap.put("release", deviceInfo.getRelease());
        deviceInfoMap.put("sdk", deviceInfo.getSdk());
        deviceInfoMap.put("sdkInt", deviceInfo.getSdkInt());
        deviceInfoMap.put("tags", deviceInfo.getTags());
        deviceInfoMap.put("user", deviceInfo.getUser());
        deviceInfoMap.put("version", deviceInfo.getVersion());
        deviceInfoMap.put("density", deviceInfo.getDensity());
        deviceInfoMap.put("densityDpi", deviceInfo.getDensityDpi());
        deviceInfoMap.put("heightPixels", deviceInfo.getHeightPixels());
        deviceInfoMap.put("widthPixels", deviceInfo.getWidthPixels());
        deviceInfoMap.put("scaledDensity", deviceInfo.getScaledDensity());
        deviceInfoMap.put("macAddress", deviceInfo.getMacAddress());
        deviceInfoMap.put("ipAddress", deviceInfo.getIpAddress());
        deviceInfoMap.put("ssid", deviceInfo.getSsid());
        deviceInfoMap.put("bssid", deviceInfo.getBssid());
        deviceInfoMap.put("networkOperator", deviceInfo.getNetworkOperator());
        deviceInfoMap.put("networkOperatorName", deviceInfo.getNetworkOperatorName());
        deviceInfoMap.put("networkType", deviceInfo.getNetworkType());
        deviceInfoMap.put("simOperator", deviceInfo.getSimOperator());
        deviceInfoMap.put("simOperatorName", deviceInfo.getSimOperatorName());
        deviceInfoMap.put("simSerialNumber", deviceInfo.getSimSerialNumber());
        deviceInfoMap.put("subscriberId", deviceInfo.getSubscriberId());
        deviceInfoMap.put("deviceId", deviceInfo.getDeviceId());
        deviceInfoMap.put("line1Number", deviceInfo.getLine1Number());
        deviceInfoMap.put("latitude", deviceInfo.getLatitude());
        deviceInfoMap.put("longitude", deviceInfo.getLongitude());
        deviceInfoMap.put("accuracy", deviceInfo.getAccuracy());
        deviceInfoMap.put("host", deviceInfo.getHost());
        deviceInfoMap.put("localHost", deviceInfo.getLocalHost());
        deviceInfoMap.put("canonicalHostName", deviceInfo.getCanonicalHostName());
        deviceInfoMap.put("hostAddress", deviceInfo.getHostAddress());
        deviceInfoMap.put("hostName", deviceInfo.getHostName());
        deviceInfoMap.put("time", deviceInfo.getTime());
        deviceInfoMap.put("type", deviceInfo.getType());
        deviceInfoMap.put("typeName", deviceInfo.getTypeName());
        deviceInfoMap.put("subtype", deviceInfo.getSubtype());
        deviceInfoMap.put("subtypeName", deviceInfo.getSubtypeName());
        deviceInfoMap.put("radioVersion", deviceInfo.getRadioVersion());
        deviceInfoMap.put("cellLocation", deviceInfo.getCellLocation());
        deviceInfoMap.put("dataActivity", deviceInfo.getDataActivity());
        deviceInfoMap.put("extraInfo", deviceInfo.getExtraInfo());
        deviceInfoMap.put("height", deviceInfo.getHeight());
        deviceInfoMap.put("width", deviceInfo.getWidth());
        deviceInfoMap.put("rotation", deviceInfo.getRotation());
        deviceInfoMap.put("rssi", deviceInfo.getRssi());
        deviceInfoMap.put("networkId", deviceInfo.getNetworkId());
        deviceInfoMap.put("provider", deviceInfo.getProvider());
        deviceInfoMap.put("bestProvider", deviceInfo.getBestProvider());
        deviceInfoMap.put("reason", deviceInfo.getReason());
        deviceInfoMap.put("scanResultsBSSID", deviceInfo.getScanResultsBSSID());
        deviceInfoMap.put("scanResultsCapabilities", deviceInfo.getScanResultsCapabilities());
        deviceInfoMap.put("scanResultsFrequency", deviceInfo.getScanResultsFrequency());
        deviceInfoMap.put("scanResultsLevel", deviceInfo.getScanResultsLevel());
        deviceInfoMap.put("scanResultsSSID", deviceInfo.getScanResultsSSID());
    }
    
    /**
     * 获取设备信息
     * @return DeviceInfo实例
     */
    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }
    
    /**
     * 获取设备信息映射
     * @return 设备信息映射
     */
    public Map<String, String> getDeviceInfoMap() {
        return deviceInfoMap;
    }
    
    /**
     * 获取特定设备信息
     * @param key 键
     * @return 值
     */
    public String getDeviceInfo(String key) {
        return deviceInfoMap.get(key);
    }
    
    /**
     * 设置设备信息
     * @param key 键
     * @param value 值
     */
    public void setDeviceInfo(String key, String value) {
        deviceInfoMap.put(key, value);
        
        // 同时更新DeviceInfo对象
        switch (key) {
            case "androidId":
                deviceInfo.setAndroidId(value);
                break;
            case "brand":
                deviceInfo.setBrand(value);
                break;
            case "model":
                deviceInfo.setModel(value);
                break;
            // ... 其他字段的设置
        }
    }
}