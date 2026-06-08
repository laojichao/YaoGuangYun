package com.yaoguangyun.network;

/**
 * 服务器配置类
 * 基于bbc.me应用的ServerConfig实现
 * 对应原始类: bbc.me.ServerConfig
 * 
 * 包含服务器连接配置、密钥配置和缓存配置
 * 这些配置从原始bbc.me应用中提取，用于网络通信
 */
public class ServerConfig {
    
    // ==================== 服务器连接配置 ====================
    /** 服务器IP地址或域名 */
    private static final String SERVER_IP = "hc.t60.top";
    /** TCP通信端口 */
    private static final int TCP_PORT = 5000;
    /** HTTP通信端口 */
    private static final int HTTP_PORT = 5600;
    
    // ==================== 密钥配置 ====================
    /** RSA公钥，用于数据加密 */
    private static final String RSA_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiGcQ9YZC70OTUFdZtTfsqXgTwwK90bwU0p5sE9yi/kfebKs2JAi8JSeBSKJkaZ3T4+Q9oCWTnLrpkgqMCkNq/3GY2aTpyp/c4Tgl2ckf1Chfoy27fLcDJWjK/zBUzgLTjV0L0eOG7L7Gr+cJgcYrUPv7CXch773JKcNK0ce/uZD2+GChM+zOycaVbeJj1+WYGo0Dq8aTYqiJh99tKEKyGT7O3JKbyEb2jyeQO/TCCsAoVOulQcuGXUGageBrJnnod3J0QUdF30SMOLtlKk4tDjb+ptv7rk58EIYFcyzwAufiucQ8Vj0FgGm92/fZ9r29dnTDI+VTeZ7JGo+kiBIAkwIDAQAB";
    /** 应用密钥，用于身份验证 */
    private static final String APP_KEY = "WrefHO4WuLpoopOW2bg09SSaVEj8xpBh9IZ8fpXppPjUWlYYIeT82sB+UwpmHmJLxwLFXMu1thxBdi0FfxiD0grclLmmPFJvfV0Z/FBOKUqSkF884Ynh6uQiFX4XSkPL1y7SaEKpc8XBDJos+eCfDFa1L1aI/DlTAd6Rnhih9CPDHML4/pSa88BLqkVfh4t5f0DdiLS5N5D6VIvBUV48r2nW78Z14M5bw2/HW/NwwlUHTkZV4pkv+hLrwCVCf0onSHdNe7gP2oD0JC/6zNcKEbZ1aqvdNKRwol4bwk1gytrz6X6SHDIFmBwYFLY9UvoqqKrXDNe9L8RtKavYzTL4yg==";
    
    // ==================== 缓存配置 ====================
    /** 是否启用缓存 */
    private static final boolean ENABLE_CACHE = true;
    
    /**
     * 获取服务器IP地址
     * @return 服务器IP地址
     */
    public static String getServerIp() {
        return SERVER_IP;
    }
    
    /**
     * 获取TCP通信端口
     * @return TCP端口号
     */
    public static int getTcpPort() {
        return TCP_PORT;
    }
    
    /**
     * 获取HTTP通信端口
     * @return HTTP端口号
     */
    public static int getHttpPort() {
        return HTTP_PORT;
    }
    
    /**
     * 获取RSA公钥
     * @return RSA公钥字符串
     */
    public static String getRsaPublicKey() {
        return RSA_PUBLIC_KEY;
    }
    
    /**
     * 获取应用密钥
     */
    public static String getAppKey() {
        return APP_KEY;
    }
    
    /**
     * 是否启用缓存
     */
    public static boolean isCacheEnabled() {
        return ENABLE_CACHE;
    }
}