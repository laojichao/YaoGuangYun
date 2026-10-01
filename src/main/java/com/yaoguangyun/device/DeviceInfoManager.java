package com.yaoguangyun.device;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
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
 *
 * <p><b>关于两个视图</b>：{@link #getDeviceInfo()} 返回的对象是唯一权威数据源，
 * {@link #getDeviceInfoMap()} 的键名由 {@code DeviceInfo} 上的 {@code @SerializedName}
 * 派生（统一 snake_case），因此 map 视图与 JSON 视图的键名始终一致，不会漂移。</p>
 */
public class DeviceInfoManager {

    /** 序列化用 Gson：包含 null 字段，保证 map 视图始终具备全部字段键 */
    private static final Gson DEVICE_GSON = new GsonBuilder().serializeNulls().create();

    /** 单例实例 */
    private static DeviceInfoManager instance;

    /** 设备信息对象（唯一权威数据源） */
    private DeviceInfo deviceInfo;

    /**
     * 私有构造函数，防止外部实例化
     */
    private DeviceInfoManager() {
        this.deviceInfo = new DeviceInfo();
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
    public synchronized void collectDeviceInfo() {
        // ==================== 基本设备信息 ====================
        // 设备唯一标识：必须设置，否则所有请求的 android_id 都为空
        deviceInfo.setAndroidId("9774d56d682e549c");
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
        deviceInfo.setSerial("R58M40KXYZT");
        
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
        // ICCID 的 MNC 段必须与 simOperator 一致：898600... 对应中国移动（46000）
        deviceInfo.setSimSerialNumber("8986002345678901234");
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
        deviceInfo.setCellLocation("cid=12345,lac=6789");
        deviceInfo.setDataActivity("DATA_ACTIVITY_NONE");
        deviceInfo.setExtraInfo("cmnet");
        deviceInfo.setHeight("2960");
        deviceInfo.setWidth("1440");
        deviceInfo.setRotation("0");
        deviceInfo.setRssi("-50");
        deviceInfo.setNetworkId("1");
        deviceInfo.setProvider("gps");
        deviceInfo.setBestProvider("gps");
        deviceInfo.setReason("");
        
        // WiFi 扫描结果
        deviceInfo.setScanResultsBSSID("00:11:22:33:44:55,aa:bb:cc:dd:ee:ff");
        deviceInfo.setScanResultsCapabilities("[WPA2-PSK-CCMP][ESS],[WPA2-PSK-CCMP][ESS]");
        deviceInfo.setScanResultsFrequency("2412,2437");
        deviceInfo.setScanResultsLevel("-50,-67");
        deviceInfo.setScanResultsSSID("MyWiFi,NeighborWiFi");
    }
    
    /**
     * 获取设备信息
     *
     * <p>返回的是内部权威对象本身。直接修改它不会同步到 {@link #getDeviceInfoMap()}
     * 的缓存语义之外——两个视图都是从同一个对象派生，因此始终一致；
     * 但若需要按字段名修改，请使用 {@link #setDeviceInfo(String, String)} 以便校验键名。</p>
     *
     * @return DeviceInfo实例
     */
    public synchronized DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }
    
    /**
     * 获取设备信息映射
     *
     * <p>键名与 {@code DeviceInfo} 的 JSON 字段名一致（snake_case），
     * 由对象实时派生，因此不会与对象产生漂移。</p>
     *
     * @return 设备信息映射的副本（修改副本不影响内部状态）
     */
    public synchronized Map<String, String> getDeviceInfoMap() {
        return new LinkedHashMap<>(buildDeviceInfoMap());
    }
    
    /**
     * 获取特定设备信息
     * @param key 键（snake_case，与 JSON 字段名一致）
     * @return 值
     */
    public synchronized String getDeviceInfo(String key) {
        return buildDeviceInfoMap().get(key);
    }
    
    /**
     * 设置设备信息
     *
     * <p>按字段名修改权威对象，两个视图都会随之更新。</p>
     *
     * <p><b>对象身份保持</b>：这里直接在原 {@link DeviceInfo} 实例上写入字段，
     * 而不是重建对象。否则调用方此前通过 {@link #getDeviceInfo()} 取得的引用会变成
     * 「过期副本」，静默读到旧值。</p>
     *
     * @param key 键（snake_case，与 JSON 字段名一致）
     * @param value 值
     * @throws IllegalArgumentException 键不是 DeviceInfo 的已知字段
     */
    public synchronized void setDeviceInfo(String key, String value) {
        Field field = findFieldBySerializedName(key);
        if (field == null) {
            throw new IllegalArgumentException("未知的设备信息字段: " + key);
        }
        try {
            field.setAccessible(true);
            field.set(deviceInfo, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("设置设备信息失败: " + key, e);
        }
    }

    /**
     * 按 JSON 字段名（@SerializedName）查找 DeviceInfo 的字段。
     *
     * @param serializedName JSON 字段名
     * @return 对应字段；不存在时返回 null
     */
    private static Field findFieldBySerializedName(String serializedName) {
        if (serializedName == null || serializedName.isEmpty()) {
            return null;
        }
        for (Field field : DeviceInfo.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            SerializedName annotation = field.getAnnotation(SerializedName.class);
            String name = annotation != null ? annotation.value() : field.getName();
            if (serializedName.equals(name)) {
                return field;
            }
        }
        return null;
    }

    /**
     * 从权威对象派生 key → value 映射（键为 @SerializedName 值）。
     *
     * @return 包含全部字段（含 null 值）的映射
     */
    private Map<String, String> buildDeviceInfoMap() {
        JsonObject json = DEVICE_GSON.toJsonTree(deviceInfo).getAsJsonObject();
        Map<String, String> map = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            JsonElement value = entry.getValue();
            map.put(entry.getKey(), value.isJsonNull() ? null : value.getAsString());
        }
        return map;
    }
}
