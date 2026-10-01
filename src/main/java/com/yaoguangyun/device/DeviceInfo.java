package com.yaoguangyun.device;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * 设备信息类
 * 基于bbc.me应用的DeviceInfoMap实现
 * 对应原始类: bbc.me.DeviceInfoMap
 * 
 * 包含完整的设备信息，用于设备伪装和信息收集
 * 收集70+个设备参数，涵盖硬件、系统、网络、位置等信息
 * 
 * 使用Gson注解支持JSON序列化和反序列化。
 * 所有字段统一使用 snake_case 的 {@code @SerializedName}，与请求载荷中的键名保持一致。
 */
public class DeviceInfo {
    
    // ==================== 基本设备信息 ====================
    @SerializedName("android_id")
    private String androidId;
    
    @SerializedName("brand")
    private String brand;
    
    @SerializedName("model")
    private String model;
    
    @SerializedName("device")
    private String device;
    
    @SerializedName("product")
    private String product;
    
    @SerializedName("manufacturer")
    private String manufacturer;
    
    @SerializedName("board")
    private String board;
    
    @SerializedName("hardware")
    private String hardware;
    
    @SerializedName("bootloader")
    private String bootloader;
    
    @SerializedName("serial")
    private String serial;
    
    // ==================== 系统信息 ====================
    @SerializedName("build_id")
    private String buildID;
    
    @SerializedName("codename")
    private String codename;
    
    @SerializedName("display")
    private String display;
    
    @SerializedName("fingerprint")
    private String fingerprint;
    
    @SerializedName("incremental")
    private String incremental;
    
    @SerializedName("release")
    private String release;
    
    @SerializedName("sdk")
    private String sdk;
    
    @SerializedName("sdk_int")
    private String sdkInt;
    
    @SerializedName("tags")
    private String tags;
    
    @SerializedName("user")
    private String user;
    
    @SerializedName("version")
    private String version;
    
    // ==================== 屏幕信息 ====================
    @SerializedName("density")
    private String density;
    
    @SerializedName("density_dpi")
    private String densityDpi;
    
    @SerializedName("height_pixels")
    private String heightPixels;
    
    @SerializedName("width_pixels")
    private String widthPixels;
    
    @SerializedName("scaled_density")
    private String scaledDensity;
    
    // ==================== 网络信息 ====================
    @SerializedName("mac_address")
    private String macAddress;
    
    @SerializedName("ip_address")
    private String ipAddress;
    
    @SerializedName("ssid")
    private String ssid;
    
    @SerializedName("bssid")
    private String bssid;
    
    @SerializedName("network_operator")
    private String networkOperator;
    
    @SerializedName("network_operator_name")
    private String networkOperatorName;
    
    @SerializedName("network_type")
    private String networkType;
    
    @SerializedName("sim_operator")
    private String simOperator;
    
    @SerializedName("sim_operator_name")
    private String simOperatorName;
    
    @SerializedName("sim_serial_number")
    private String simSerialNumber;
    
    @SerializedName("subscriber_id")
    private String subscriberId;
    
    @SerializedName("device_id")
    private String deviceId;
    
    @SerializedName("line1_number")
    private String line1Number;
    
    // ==================== 位置信息 ====================
    @SerializedName("latitude")
    private String latitude;
    
    @SerializedName("longitude")
    private String longitude;
    
    @SerializedName("accuracy")
    private String accuracy;
    
    // ==================== 网络详细信息 ====================
    /** 主机名 */
    @SerializedName("host")
    private String host;
    /** 本地主机名 */
    @SerializedName("local_host")
    private String localHost;
    /** 规范主机名 */
    @SerializedName("canonical_host_name")
    private String canonicalHostName;
    /** 主机地址 */
    @SerializedName("host_address")
    private String hostAddress;
    /** 主机名称 */
    @SerializedName("host_name")
    private String hostName;
    /** 时间戳 */
    @SerializedName("time")
    private String time;
    /** 网络类型 */
    @SerializedName("type")
    private String type;
    /** 网络类型名称 */
    @SerializedName("type_name")
    private String typeName;
    /** 网络子类型 */
    @SerializedName("subtype")
    private String subtype;
    /** 网络子类型名称 */
    @SerializedName("subtype_name")
    private String subtypeName;
    /** 基带版本 */
    @SerializedName("radio_version")
    private String radioVersion;
    /** 基站位置 */
    @SerializedName("cell_location")
    private String cellLocation;
    /** 数据活动状态 */
    @SerializedName("data_activity")
    private String dataActivity;
    /** 额外信息 */
    @SerializedName("extra_info")
    private String extraInfo;
    /** 屏幕高度 */
    @SerializedName("height")
    private String height;
    /** 屏幕宽度 */
    @SerializedName("width")
    private String width;
    /** 屏幕旋转角度 */
    @SerializedName("rotation")
    private String rotation;
    /** 信号强度 */
    @SerializedName("rssi")
    private String rssi;
    /** 网络ID */
    @SerializedName("network_id")
    private String networkId;
    /** 定位提供者 */
    @SerializedName("provider")
    private String provider;
    /** 最佳定位提供者 */
    @SerializedName("best_provider")
    private String bestProvider;
    /** 网络原因 */
    @SerializedName("reason")
    private String reason;
    
    // ==================== WiFi扫描结果 ====================
    /** 扫描结果BSSID列表 */
    @SerializedName("scan_results_bssid")
    private String scanResultsBSSID;
    /** 扫描结果能力列表 */
    @SerializedName("scan_results_capabilities")
    private String scanResultsCapabilities;
    /** 扫描结果频率列表 */
    @SerializedName("scan_results_frequency")
    private String scanResultsFrequency;
    /** 扫描结果信号强度列表 */
    @SerializedName("scan_results_level")
    private String scanResultsLevel;
    /** 扫描结果SSID列表 */
    @SerializedName("scan_results_ssid")
    private String scanResultsSSID;
    
    /**
     * 默认构造函数
     */
    public DeviceInfo() {
    }
    
    // Getters and Setters
    public String getAndroidId() {
        return androidId;
    }
    
    public void setAndroidId(String androidId) {
        this.androidId = androidId;
    }
    
    public String getBrand() {
        return brand;
    }
    
    public void setBrand(String brand) {
        this.brand = brand;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public String getDevice() {
        return device;
    }
    
    public void setDevice(String device) {
        this.device = device;
    }
    
    public String getProduct() {
        return product;
    }
    
    public void setProduct(String product) {
        this.product = product;
    }
    
    public String getManufacturer() {
        return manufacturer;
    }
    
    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }
    
    public String getBoard() {
        return board;
    }
    
    public void setBoard(String board) {
        this.board = board;
    }
    
    public String getHardware() {
        return hardware;
    }
    
    public void setHardware(String hardware) {
        this.hardware = hardware;
    }
    
    public String getBootloader() {
        return bootloader;
    }
    
    public void setBootloader(String bootloader) {
        this.bootloader = bootloader;
    }
    
    public String getSerial() {
        return serial;
    }
    
    public void setSerial(String serial) {
        this.serial = serial;
    }
    
    public String getBuildID() {
        return buildID;
    }
    
    public void setBuildID(String buildID) {
        this.buildID = buildID;
    }
    
    public String getCodename() {
        return codename;
    }
    
    public void setCodename(String codename) {
        this.codename = codename;
    }
    
    public String getDisplay() {
        return display;
    }
    
    public void setDisplay(String display) {
        this.display = display;
    }
    
    public String getFingerprint() {
        return fingerprint;
    }
    
    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }
    
    public String getIncremental() {
        return incremental;
    }
    
    public void setIncremental(String incremental) {
        this.incremental = incremental;
    }
    
    public String getRelease() {
        return release;
    }
    
    public void setRelease(String release) {
        this.release = release;
    }
    
    public String getSdk() {
        return sdk;
    }
    
    public void setSdk(String sdk) {
        this.sdk = sdk;
    }
    
    public String getSdkInt() {
        return sdkInt;
    }
    
    public void setSdkInt(String sdkInt) {
        this.sdkInt = sdkInt;
    }
    
    public String getTags() {
        return tags;
    }
    
    public void setTags(String tags) {
        this.tags = tags;
    }
    
    public String getUser() {
        return user;
    }
    
    public void setUser(String user) {
        this.user = user;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getDensity() {
        return density;
    }
    
    public void setDensity(String density) {
        this.density = density;
    }
    
    public String getDensityDpi() {
        return densityDpi;
    }
    
    public void setDensityDpi(String densityDpi) {
        this.densityDpi = densityDpi;
    }
    
    public String getHeightPixels() {
        return heightPixels;
    }
    
    public void setHeightPixels(String heightPixels) {
        this.heightPixels = heightPixels;
    }
    
    public String getWidthPixels() {
        return widthPixels;
    }
    
    public void setWidthPixels(String widthPixels) {
        this.widthPixels = widthPixels;
    }
    
    public String getScaledDensity() {
        return scaledDensity;
    }
    
    public void setScaledDensity(String scaledDensity) {
        this.scaledDensity = scaledDensity;
    }
    
    public String getMacAddress() {
        return macAddress;
    }
    
    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getSsid() {
        return ssid;
    }
    
    public void setSsid(String ssid) {
        this.ssid = ssid;
    }
    
    public String getBssid() {
        return bssid;
    }
    
    public void setBssid(String bssid) {
        this.bssid = bssid;
    }
    
    public String getNetworkOperator() {
        return networkOperator;
    }
    
    public void setNetworkOperator(String networkOperator) {
        this.networkOperator = networkOperator;
    }
    
    public String getNetworkOperatorName() {
        return networkOperatorName;
    }
    
    public void setNetworkOperatorName(String networkOperatorName) {
        this.networkOperatorName = networkOperatorName;
    }
    
    public String getNetworkType() {
        return networkType;
    }
    
    public void setNetworkType(String networkType) {
        this.networkType = networkType;
    }
    
    public String getSimOperator() {
        return simOperator;
    }
    
    public void setSimOperator(String simOperator) {
        this.simOperator = simOperator;
    }
    
    public String getSimOperatorName() {
        return simOperatorName;
    }
    
    public void setSimOperatorName(String simOperatorName) {
        this.simOperatorName = simOperatorName;
    }
    
    public String getSimSerialNumber() {
        return simSerialNumber;
    }
    
    public void setSimSerialNumber(String simSerialNumber) {
        this.simSerialNumber = simSerialNumber;
    }
    
    public String getSubscriberId() {
        return subscriberId;
    }
    
    public void setSubscriberId(String subscriberId) {
        this.subscriberId = subscriberId;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getLine1Number() {
        return line1Number;
    }
    
    public void setLine1Number(String line1Number) {
        this.line1Number = line1Number;
    }
    
    public String getLatitude() {
        return latitude;
    }
    
    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }
    
    public String getLongitude() {
        return longitude;
    }
    
    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }
    
    public String getAccuracy() {
        return accuracy;
    }
    
    public void setAccuracy(String accuracy) {
        this.accuracy = accuracy;
    }
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public String getLocalHost() {
        return localHost;
    }
    
    public void setLocalHost(String localHost) {
        this.localHost = localHost;
    }
    
    public String getCanonicalHostName() {
        return canonicalHostName;
    }
    
    public void setCanonicalHostName(String canonicalHostName) {
        this.canonicalHostName = canonicalHostName;
    }
    
    public String getHostAddress() {
        return hostAddress;
    }
    
    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }
    
    public String getHostName() {
        return hostName;
    }
    
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }
    
    public String getTime() {
        return time;
    }
    
    public void setTime(String time) {
        this.time = time;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getTypeName() {
        return typeName;
    }
    
    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }
    
    public String getSubtype() {
        return subtype;
    }
    
    public void setSubtype(String subtype) {
        this.subtype = subtype;
    }
    
    public String getSubtypeName() {
        return subtypeName;
    }
    
    public void setSubtypeName(String subtypeName) {
        this.subtypeName = subtypeName;
    }
    
    public String getRadioVersion() {
        return radioVersion;
    }
    
    public void setRadioVersion(String radioVersion) {
        this.radioVersion = radioVersion;
    }
    
    public String getCellLocation() {
        return cellLocation;
    }
    
    public void setCellLocation(String cellLocation) {
        this.cellLocation = cellLocation;
    }
    
    public String getDataActivity() {
        return dataActivity;
    }
    
    public void setDataActivity(String dataActivity) {
        this.dataActivity = dataActivity;
    }
    
    public String getExtraInfo() {
        return extraInfo;
    }
    
    public void setExtraInfo(String extraInfo) {
        this.extraInfo = extraInfo;
    }
    
    public String getHeight() {
        return height;
    }
    
    public void setHeight(String height) {
        this.height = height;
    }
    
    public String getWidth() {
        return width;
    }
    
    public void setWidth(String width) {
        this.width = width;
    }
    
    public String getRotation() {
        return rotation;
    }
    
    public void setRotation(String rotation) {
        this.rotation = rotation;
    }
    
    public String getRssi() {
        return rssi;
    }
    
    public void setRssi(String rssi) {
        this.rssi = rssi;
    }
    
    public String getNetworkId() {
        return networkId;
    }
    
    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public String getBestProvider() {
        return bestProvider;
    }
    
    public void setBestProvider(String bestProvider) {
        this.bestProvider = bestProvider;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getScanResultsBSSID() {
        return scanResultsBSSID;
    }
    
    public void setScanResultsBSSID(String scanResultsBSSID) {
        this.scanResultsBSSID = scanResultsBSSID;
    }
    
    public String getScanResultsCapabilities() {
        return scanResultsCapabilities;
    }
    
    public void setScanResultsCapabilities(String scanResultsCapabilities) {
        this.scanResultsCapabilities = scanResultsCapabilities;
    }
    
    public String getScanResultsFrequency() {
        return scanResultsFrequency;
    }
    
    public void setScanResultsFrequency(String scanResultsFrequency) {
        this.scanResultsFrequency = scanResultsFrequency;
    }
    
    public String getScanResultsLevel() {
        return scanResultsLevel;
    }
    
    public void setScanResultsLevel(String scanResultsLevel) {
        this.scanResultsLevel = scanResultsLevel;
    }
    
    public String getScanResultsSSID() {
        return scanResultsSSID;
    }
    
    public void setScanResultsSSID(String scanResultsSSID) {
        this.scanResultsSSID = scanResultsSSID;
    }

    // ==================== 通用方法 ====================

    /** 用于 equals/hashCode/toString 的 Gson 实例（字段全部由 @SerializedName 驱动） */
    private static final Gson GSON = new Gson();

    /**
     * 判断两个设备信息是否相等。
     *
     * <p>本类有 69 个字段，逐字段手写比较极易漏项或错位，因此直接比较序列化结果——
     * 序列化键由 {@code @SerializedName} 唯一决定，等价于逐字段比较且与 JSON 契约一致。</p>
     *
     * @param obj 待比较对象
     * @return 是否相等
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return GSON.toJson(this).equals(GSON.toJson(obj));
    }

    @Override
    public int hashCode() {
        return GSON.toJson(this).hashCode();
    }

    @Override
    public String toString() {
        return "DeviceInfo" + GSON.toJson(this);
    }
}