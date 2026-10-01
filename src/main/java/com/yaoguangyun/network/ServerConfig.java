package com.yaoguangyun.network;

import com.yaoguangyun.config.JsonConfigParser;

import java.io.File;

/**
 * 服务器配置类
 * 基于bbc.me应用的ServerConfig实现
 * 对应原始类: bbc.me.ServerConfig
 *
 * 包含服务器连接配置、密钥配置和缓存配置。
 *
 * <p>配置来源优先级（高 → 低）：</p>
 * <ol>
 *   <li>环境变量（用于把密钥排除在源码/仓库之外）：{@code YAOGUANGYUN_SERVER_IP}、
 *       {@code YAOGUANGYUN_TCP_PORT}、{@code YAOGUANGYUN_HTTP_PORT}、
 *       {@code YAOGUANGYUN_RSA_PUBLIC_KEY}、{@code YAOGUANGYUN_APP_KEY}；</li>
 *   <li>{@code config.json}（路径可由系统属性 {@code yaoguangyun.config} 或
 *       环境变量 {@code YAOGUANGYUN_CONFIG} 指定，默认取工作目录下的 config.json，
 *       再回退到 classpath 根）；</li>
 *   <li>本类内置默认值（与原始 bbc.me 应用一致的取值）。</li>
 * </ol>
 */
public final class ServerConfig {

    /** 默认配置文件（工作目录，或 jar 内的 classpath 资源） */
    private static final String DEFAULT_CONFIG_FILE = "config.json";

    // ==================== 内置默认值 ====================
    /** 服务器IP地址或域名 */
    private static final String DEFAULT_SERVER_IP = "hc.t60.top";
    /** TCP通信端口 */
    private static final int DEFAULT_TCP_PORT = 5000;
    /** HTTP通信端口 */
    private static final int DEFAULT_HTTP_PORT = 5600;
    /** 默认超时（毫秒） */
    private static final int DEFAULT_TIMEOUT = 5000;
    /** 默认缓冲区大小（字节） */
    private static final int DEFAULT_BUFFER_SIZE = 131072;
    /** 默认最大重定向次数 */
    private static final int DEFAULT_MAX_REDIRECTS = 5;
    /** 默认RSA公钥，用于数据加密 */
    private static final String DEFAULT_RSA_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiGcQ9YZC70OTUFdZtTfsqXgTwwK90bwU0p5sE9yi/kfebKs2JAi8JSeBSKJkaZ3T4+Q9oCWTnLrpkgqMCkNq/3GY2aTpyp/c4Tgl2ckf1Chfoy27fLcDJWjK/zBUzgLTjV0L0eOG7L7Gr+cJgcYrUPv7CXch773JKcNK0ce/uZD2+GChM+zOycaVbeJj1+WYGo0Dq8aTYqiJh99tKEKyGT7O3JKbyEb2jyeQO/TCCsAoVOulQcuGXUGageBrJnnod3J0QUdF30SMOLtlKk4tDjb+ptv7rk58EIYFcyzwAufiucQ8Vj0FgGm92/fZ9r29dnTDI+VTeZ7JGo+kiBIAkwIDAQAB";
    /** 默认应用密钥，用于身份验证（历史遗留值，建议改用环境变量 YAOGUANGYUN_APP_KEY 注入） */
    private static final String DEFAULT_APP_KEY = "WrefHO4WuLpoopOW2bg09SSaVEj8xpBh9IZ8fpXppPjUWlYYIeT82sB+UwpmHmJLxwLFXMu1thxBdi0FfxiD0grclLmmPFJvfV0Z/FBOKUqSkF884Ynh6uQiFX4XSkPL1y7SaEKpc8XBDJos+eCfDFa1L1aI/DlTAd6Rnhih9CPDHML4/pSa88BLqkVfh4t5f0DdiLS5N5D6VIvBUV48r2nW78Z14M5bw2/HW/NwwlUHTkZV4pkv+hLrwCVCf0onSHdNe7gP2oD0JC/6zNcKEbZ1aqvdNKRwol4bwk1gytrz6X6SHDIFmBwYFLY9UvoqqKrXDNe9L8RtKavYzTL4yg==";
    /** 默认是否启用缓存 */
    private static final boolean DEFAULT_ENABLE_CACHE = true;

    // ==================== 生效配置 ====================
    private static String serverIp = DEFAULT_SERVER_IP;
    private static int tcpPort = DEFAULT_TCP_PORT;
    private static int httpPort = DEFAULT_HTTP_PORT;
    private static int timeout = DEFAULT_TIMEOUT;
    private static int connectTimeout = DEFAULT_TIMEOUT;
    private static int readTimeout = DEFAULT_TIMEOUT;
    private static int bufferSize = DEFAULT_BUFFER_SIZE;
    private static int maxRedirects = DEFAULT_MAX_REDIRECTS;
    private static String rsaPublicKey = DEFAULT_RSA_PUBLIC_KEY;
    private static String appKey = DEFAULT_APP_KEY;
    private static boolean cacheEnabled = DEFAULT_ENABLE_CACHE;

    static {
        loadFromConfig();
        applyEnvironmentOverrides();
    }

    private ServerConfig() {
    }

    /**
     * 从 config.json 加载配置；文件缺失或非法时保留内置默认值。
     */
    private static void loadFromConfig() {
        String path = resolveConfigPath();
        if (path == null) {
            return;
        }

        JsonConfigParser parser = new JsonConfigParser(path);
        if (!parser.load()) {
            return;
        }

        serverIp = parser.getString("server.ip", DEFAULT_SERVER_IP);
        tcpPort = requirePositive(parser.getInt("server.tcp_port", DEFAULT_TCP_PORT), "server.tcp_port", DEFAULT_TCP_PORT);
        httpPort = requirePositive(parser.getInt("server.http_port", DEFAULT_HTTP_PORT), "server.http_port", DEFAULT_HTTP_PORT);
        timeout = requirePositive(parser.getInt("server.timeout", DEFAULT_TIMEOUT), "server.timeout", DEFAULT_TIMEOUT);
        connectTimeout = requirePositive(parser.getInt("network.connect_timeout", timeout), "network.connect_timeout", timeout);
        readTimeout = requirePositive(parser.getInt("network.read_timeout", timeout), "network.read_timeout", timeout);
        bufferSize = requirePositive(parser.getInt("network.buffer_size", DEFAULT_BUFFER_SIZE), "network.buffer_size", DEFAULT_BUFFER_SIZE);
        // max_redirects = 0 是合法配置（表示不跟随重定向），只拒绝负数
        maxRedirects = requireNonNegative(parser.getInt("network.max_redirects", DEFAULT_MAX_REDIRECTS), "network.max_redirects", DEFAULT_MAX_REDIRECTS);
        rsaPublicKey = parser.getString("security.rsa_public_key", DEFAULT_RSA_PUBLIC_KEY);
        appKey = parser.getString("security.app_key", DEFAULT_APP_KEY);
        cacheEnabled = parser.getBoolean("cache.enabled", DEFAULT_ENABLE_CACHE);
    }

    /**
     * 校验配置项为正数。
     *
     * <p>0 在套接字 API 里意味着「无限等待」/非法参数（例如 {@code setSoTimeout(0)}
     * 等于永不超时、{@code setReceiveBufferSize(0)} 直接抛异常），因此必须拒绝。</p>
     */
    private static int requirePositive(int value, String key, int fallback) {
        if (value > 0) {
            return value;
        }
        System.err.println("[ServerConfig] 配置项 " + key + " 必须为正数，实际为 " + value
                + "，回退到 " + fallback);
        return fallback;
    }

    /**
     * 校验配置项为非负数（0 合法）。
     */
    private static int requireNonNegative(int value, String key, int fallback) {
        if (value >= 0) {
            return value;
        }
        System.err.println("[ServerConfig] 配置项 " + key + " 不能为负数，实际为 " + value
                + "，回退到 " + fallback);
        return fallback;
    }

    /**
     * 解析配置文件路径：系统属性 → 环境变量 → 工作目录 → classpath。
     *
     * @return 可用的配置文件路径；都不可用时返回 null（此时使用内置默认值）
     */
    private static String resolveConfigPath() {
        String configured = System.getProperty("yaoguangyun.config");
        if (configured == null || configured.isEmpty()) {
            configured = System.getenv("YAOGUANGYUN_CONFIG");
        }
        if (configured != null && !configured.isEmpty()) {
            File file = new File(configured);
            if (file.isFile()) {
                return file.getPath();
            }
            // 显式指定但读不到：告警并继续沿默认链查找，避免一个拼写错误静默禁用全部配置
            System.err.println("[ServerConfig] 指定的配置文件不存在，继续查找默认位置: " + configured);
        }

        File local = new File(DEFAULT_CONFIG_FILE);
        if (local.isFile()) {
            return local.getPath();
        }

        // 回退到随包发布的 classpath 资源（jar 内 config.json）
        if (ServerConfig.class.getClassLoader().getResource(DEFAULT_CONFIG_FILE) != null) {
            return DEFAULT_CONFIG_FILE;
        }
        return null;
    }

    /**
     * 环境变量覆盖：便于把密钥从源码与仓库中移出。
     */
    private static void applyEnvironmentOverrides() {
        serverIp = envOr("YAOGUANGYUN_SERVER_IP", serverIp);
        tcpPort = envIntOr("YAOGUANGYUN_TCP_PORT", tcpPort);
        httpPort = envIntOr("YAOGUANGYUN_HTTP_PORT", httpPort);
        rsaPublicKey = envOr("YAOGUANGYUN_RSA_PUBLIC_KEY", rsaPublicKey);
        appKey = envOr("YAOGUANGYUN_APP_KEY", appKey);
    }

    private static String envOr(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isEmpty()) ? fallback : value;
    }

    private static int envIntOr(String name, int fallback) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            System.err.println("[ServerConfig] 环境变量 " + name + " 不是合法整数，忽略: " + value);
            return fallback;
        }
    }

    /**
     * 获取服务器IP地址
     * @return 服务器IP地址
     */
    public static String getServerIp() {
        return serverIp;
    }

    /**
     * 获取TCP通信端口
     * @return TCP端口号
     */
    public static int getTcpPort() {
        return tcpPort;
    }

    /**
     * 获取HTTP通信端口
     * @return HTTP端口号
     */
    public static int getHttpPort() {
        return httpPort;
    }

    /**
     * 获取通用超时时间（毫秒）
     * @return 超时时间
     */
    public static int getTimeout() {
        return timeout;
    }

    /**
     * 获取连接超时时间（毫秒）
     * @return 连接超时时间
     */
    public static int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * 获取读取超时时间（毫秒）
     * @return 读取超时时间
     */
    public static int getReadTimeout() {
        return readTimeout;
    }

    /**
     * 获取套接字缓冲区大小（字节）
     * @return 缓冲区大小
     */
    public static int getBufferSize() {
        return bufferSize;
    }

    /**
     * 获取最大重定向次数
     * @return 最大重定向次数
     */
    public static int getMaxRedirects() {
        return maxRedirects;
    }

    /**
     * 获取RSA公钥
     * @return RSA公钥字符串
     */
    public static String getRsaPublicKey() {
        return rsaPublicKey;
    }

    /**
     * 获取应用密钥
     * @return 应用密钥字符串
     */
    public static String getAppKey() {
        return appKey;
    }

    /**
     * 是否启用缓存
     * @return 是否启用缓存
     */
    public static boolean isCacheEnabled() {
        return cacheEnabled;
    }
}
