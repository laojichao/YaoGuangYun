package com.yaoguangyun;

import com.yaoguangyun.network.ServerConfig;

/**
 * 配置加载探针：由 YaoGuangYunTest 在独立 JVM 中启动，
 * 用于验证 ServerConfig 在类初始化时确实读取了 -Dyaoguangyun.config 指定的文件。
 *
 * <p>独立进程可以避免 ServerConfig 的静态状态污染测试 JVM。</p>
 */
public final class ConfigProbe {

    private ConfigProbe() {
    }

    public static void main(String[] args) {
        System.out.println("ip=" + ServerConfig.getServerIp());
        System.out.println("tcp=" + ServerConfig.getTcpPort());
        System.out.println("http=" + ServerConfig.getHttpPort());
        System.out.println("timeout=" + ServerConfig.getTimeout());
        System.out.println("connect=" + ServerConfig.getConnectTimeout());
        System.out.println("read=" + ServerConfig.getReadTimeout());
        System.out.println("buffer=" + ServerConfig.getBufferSize());
        System.out.println("redirects=" + ServerConfig.getMaxRedirects());
        System.out.println("cache=" + ServerConfig.isCacheEnabled());
        System.out.println("rsaLength=" + ServerConfig.getRsaPublicKey().length());
    }
}
