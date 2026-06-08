package com.yaoguangyun.proto;

import com.google.gson.annotations.SerializedName;

/**
 * 基本设备信息类
 * 基于bbc.me应用的BasicDeviceInfo实现
 * 对应原始类: bbc.me.BasicDeviceInfo
 * 
 * 使用Gson注解支持JSON序列化和反序列化
 */
public class BasicDeviceInfo {
    
    // 字段号常量
    public static final int LOCALE_FIELD_NUMBER = 1;
    public static final int ANDROID_ID_FIELD_NUMBER = 2;
    public static final int VERSION_FIELD_NUMBER = 3;
    public static final int STATUS_MACHINE_FIELD_NUMBER = 4;
    public static final int TIME_FIELD_NUMBER = 5;
    
    // 设备信息字段，使用SerializedName注解支持JSON字段映射
    @SerializedName("locale")
    private String locale;           // 语言环境，如 "zh" 表示中文
    
    @SerializedName("android_id")
    private String androidId;        // Android设备唯一标识符
    
    @SerializedName("version")
    private int version;             // 应用版本号
    
    @SerializedName("status_machine")
    private String statusMachine;    // 设备型号，格式: "品牌(型号)"
    
    @SerializedName("time")
    private long time;               // 时间戳（毫秒）
    
    /**
     * 默认构造函数
     */
    public BasicDeviceInfo() {
        this.locale = "";
        this.androidId = "";
        this.version = 0;
        this.statusMachine = "";
        this.time = 0L;
    }
    
    /**
     * 构造函数
     * @param locale 语言环境
     * @param androidId Android ID
     * @param version 版本号
     * @param statusMachine 设备型号
     * @param time 时间戳
     */
    public BasicDeviceInfo(String locale, String androidId, int version, String statusMachine, long time) {
        this.locale = locale != null ? locale : "";
        this.androidId = androidId != null ? androidId : "";
        this.version = version;
        this.statusMachine = statusMachine != null ? statusMachine : "";
        this.time = time;
    }
    
    /**
     * 获取语言环境
     * @return 语言环境代码
     */
    public String getLocale() {
        return locale;
    }
    
    /**
     * 设置语言环境
     * @param locale 语言环境代码
     */
    public void setLocale(String locale) {
        this.locale = locale != null ? locale : "";
    }
    
    /**
     * 获取Android设备ID
     * @return Android ID
     */
    public String getAndroidId() {
        return androidId;
    }
    
    /**
     * 设置Android设备ID
     * @param androidId Android ID
     */
    public void setAndroidId(String androidId) {
        this.androidId = androidId != null ? androidId : "";
    }
    
    /**
     * 获取应用版本号
     * @return 版本号
     */
    public int getVersion() {
        return version;
    }
    
    /**
     * 设置应用版本号
     * @param version 版本号
     */
    public void setVersion(int version) {
        this.version = version;
    }
    
    /**
     * 获取设备型号
     * @return 设备型号字符串，格式: "品牌(型号)"
     */
    public String getStatusMachine() {
        return statusMachine;
    }
    
    /**
     * 设置设备型号
     * @param statusMachine 设备型号字符串
     */
    public void setStatusMachine(String statusMachine) {
        this.statusMachine = statusMachine != null ? statusMachine : "";
    }
    
    /**
     * 获取时间戳
     * @return 时间戳（毫秒）
     */
    public long getTime() {
        return time;
    }
    
    /**
     * 设置时间戳
     * @param time 时间戳（毫秒）
     */
    public void setTime(long time) {
        this.time = time;
    }
    
    /**
     * 检查语言环境字段是否有值
     * @return 是否有值
     */
    public boolean hasLocale() {
        return locale != null && !locale.isEmpty();
    }
    
    /**
     * 检查Android ID字段是否有值
     * @return 是否有值
     */
    public boolean hasAndroidId() {
        return androidId != null && !androidId.isEmpty();
    }
    
    /**
     * 检查版本字段是否有值
     * @return 是否有值
     */
    public boolean hasVersion() {
        return version != 0;
    }
    
    /**
     * 检查设备型号字段是否有值
     * @return 是否有值
     */
    public boolean hasStatusMachine() {
        return statusMachine != null && !statusMachine.isEmpty();
    }
    
    /**
     * 检查时间字段是否有值
     * @return 是否有值
     */
    public boolean hasTime() {
        return time != 0L;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        BasicDeviceInfo that = (BasicDeviceInfo) obj;
        
        if (version != that.version) {
            return false;
        }
        if (time != that.time) {
            return false;
        }
        if (!locale.equals(that.locale)) {
            return false;
        }
        if (!androidId.equals(that.androidId)) {
            return false;
        }
        return statusMachine.equals(that.statusMachine);
    }
    
    @Override
    public int hashCode() {
        int result = locale.hashCode();
        result = 31 * result + androidId.hashCode();
        result = 31 * result + version;
        result = 31 * result + statusMachine.hashCode();
        result = 31 * result + (int) (time ^ (time >>> 32));
        return result;
    }
    
    @Override
    public String toString() {
        return "BasicDeviceInfo{" +
                "locale='" + locale + '\'' +
                ", androidId='" + androidId + '\'' +
                ", version=" + version +
                ", statusMachine='" + statusMachine + '\'' +
                ", time=" + time +
                '}';
    }
}