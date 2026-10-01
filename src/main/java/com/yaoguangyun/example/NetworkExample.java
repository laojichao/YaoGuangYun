package com.yaoguangyun.example;

import com.yaoguangyun.device.DeviceInfoManager;
import com.yaoguangyun.network.HttpClient;
import com.yaoguangyun.network.ServerConfig;
import com.yaoguangyun.network.TcpClient;
import com.yaoguangyun.network.TcpResponseCallback;
import com.yaoguangyun.util.RsaEncryptUtils;
import com.yaoguangyun.util.AppUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 网络请求示例类
 * 
 * 展示如何使用从bbc.me应用迁移过来的网络请求功能
 * 包含TCP客户端、HTTP客户端、设备信息管理等功能的使用示例
 * 
 * 功能演示：
 * 1. TCP客户端连接和通信
 * 2. HTTP客户端请求
 * 3. 设备信息收集
 * 4. RSA加密功能
 * 5. 工具类功能
 */
public class NetworkExample {
    
    /**
     * 测试TCP连接
     * 
     * 演示TCP客户端的使用方法
     * 1. 创建TcpClient实例
     * 2. 构建JSON格式的测试消息
     * 3. 发送TCP消息并处理响应
     * 4. 使用回调处理成功和失败情况
     */
    public static void testTcpConnection() {
        System.out.println("\n=== 测试TCP连接 ===");
        
        // 显示服务器配置
        System.out.println("服务器地址: " + ServerConfig.getServerIp());
        System.out.println("TCP端口: " + ServerConfig.getTcpPort());
        System.out.println("HTTP端口: " + ServerConfig.getHttpPort());
        
        // 创建TCP客户端实例
        TcpClient tcpClient = new TcpClient();
        
        // 构建测试消息（JSON格式）
        String testMessage = "{\"action\":\"test\",\"timestamp\":" + System.currentTimeMillis() + "}";
        byte[] messageBytes = testMessage.getBytes(StandardCharsets.UTF_8);
        
        System.out.println("发送TCP消息: " + testMessage);
        
        // 发送TCP消息，使用回调处理结果
        tcpClient.sendMessage(messageBytes, new TcpResponseCallback() {
            @Override
            public void onSuccess(String response) {
                System.out.println("✓ TCP响应成功: " + response);
            }
            
            @Override
            public void onError(Exception e) {
                System.err.println("✗ TCP请求失败: " + describe(e));
                System.out.println("提示: 如果服务器不可用，这是正常现象");
            }
        });
        
        // 等待异步操作完成（真实场景应使用 CountDownLatch，见 NetworkTest）
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 测试HTTP请求
     *
     * <p>默认对本地测试服务器发起请求（离线可用、可断言）；
     * 外部服务仅作参考，避免示例在无网络环境下刷出无意义的红字。</p>
     */
    public static void testHttpRequest() {
        System.out.println("\n=== 测试HTTP请求 ===");
        
        HttpClient httpClient = new HttpClient(5000);
        httpClient.addHeader("User-Agent", "Mozilla/5.0");
        httpClient.addHeader("Accept", "application/json");
        
        // 启动本地测试服务器，作为确定性的测试目标
        com.yaoguangyun.test.SimpleHttpServer server = new com.yaoguangyun.test.SimpleHttpServer(0);
        try {
            server.start();
            int port = server.getBoundPort();
            String[] testUrls = {
                "http://127.0.0.1:" + port + "/api/test",
                "http://127.0.0.1:" + port + "/api/device"
            };
            for (String url : testUrls) {
                System.out.println("\n测试URL: " + url);
                try {
                    System.out.println("HTTP响应: " + readAll(httpClient.get(url)));
                    System.out.println("✓ HTTP请求成功");
                } catch (IOException e) {
                    System.err.println("✗ HTTP请求失败: " + describe(e));
                }
            }
        } catch (IOException e) {
            System.err.println("✗ 本地测试服务器启动失败: " + describe(e));
        } finally {
            server.stop();
        }
    }

    /**
     * 完整读取响应流。
     *
     * <p>单次 {@code read()} 只保证返回「至少一个字节」，不保证读满；
     * 原实现只读一次 1024 字节，超过该长度的响应会被静默截断（甚至切断 UTF-8 字符）
     * 却仍然报告“请求成功”。</p>
     *
     * @param inputStream 响应流
     * @return 完整响应文本
     * @throws IOException 读取失败
     */
    private static String readAll(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        try {
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = inputStream.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            inputStream.close();
        }
    }

    /**
     * 描述异常：getMessage() 为 null 时给出类名，避免打印出「失败: null」
     */
    private static String describe(Throwable e) {
        if (e == null) {
            return "null";
        }
        String message = e.getMessage();
        if (message == null || message.isEmpty()) {
            return e.getClass().getSimpleName() + "(无消息)";
        }
        return message;
    }
    
    /**
     * 测试设备信息收集
     */
    public static void testDeviceInfoCollection() {
        System.out.println("\n=== 测试设备信息收集 ===");
        
        DeviceInfoManager deviceInfoManager = DeviceInfoManager.getInstance();
        deviceInfoManager.collectDeviceInfo();
        
        // 显示部分设备信息（键名与 DeviceInfo 的 JSON 字段名一致，均为 snake_case）
        Map<String, String> deviceInfo = deviceInfoManager.getDeviceInfoMap();
        System.out.println("品牌: " + deviceInfo.get("brand"));
        System.out.println("型号: " + deviceInfo.get("model"));
        System.out.println("Android版本: " + deviceInfo.get("release"));
        System.out.println("SDK版本: " + deviceInfo.get("sdk_int"));
        System.out.println("屏幕分辨率: " + deviceInfo.get("width_pixels") + "x" + deviceInfo.get("height_pixels"));
        System.out.println("MAC地址: " + deviceInfo.get("mac_address"));
        System.out.println("IP地址: " + deviceInfo.get("ip_address"));
        System.out.println("网络类型: " + deviceInfo.get("network_type"));
    }
    
    /**
     * 测试RSA加密
     */
    public static void testRsaEncryption() {
        System.out.println("\n=== 测试RSA加密 ===");
        
        String testData = "{\"userId\":\"12345\",\"action\":\"login\"}";
        
        try {
            // 生成密钥对
            System.out.println("生成RSA密钥对...");
            java.security.KeyPair keyPair = RsaEncryptUtils.generateKeyPair();
            String publicKey = RsaEncryptUtils.getPublicKeyBase64(keyPair);
            String privateKey = RsaEncryptUtils.getPrivateKeyBase64(keyPair);
            
            System.out.println("公钥长度: " + publicKey.length() + " 字符");
            System.out.println("私钥长度: " + privateKey.length() + " 字符");
            
            // 使用公钥加密，私钥解密（标准加密流程）
            System.out.println("\n--- 公钥加密，私钥解密 ---");
            String encrypted = RsaEncryptUtils.encrypt(testData, publicKey);
            System.out.println("原始数据: " + testData);
            System.out.println("加密后: " + encrypted);
            
            String decrypted = RsaEncryptUtils.decrypt(encrypted, privateKey);
            System.out.println("解密后: " + decrypted);
            
            // 验证解密结果
            if (testData.equals(decrypted)) {
                System.out.println("✓ 加密解密测试成功！");
            } else {
                System.out.println("✗ 加密解密测试失败！");
            }
            
            // 数字签名与验签（SHA256withRSA）——这是正确的签名方式
            System.out.println("\n--- 数字签名与验签（SHA256withRSA） ---");
            String signature = RsaEncryptUtils.sign(testData, privateKey);
            System.out.println("签名: " + signature);
            System.out.println("验签（原始数据）: " + RsaEncryptUtils.verify(testData, signature, publicKey));
            System.out.println("验签（被篡改）  : " + RsaEncryptUtils.verify(testData + "x", signature, publicKey));

            if (RsaEncryptUtils.verify(testData, signature, publicKey)
                    && !RsaEncryptUtils.verify(testData + "x", signature, publicKey)) {
                System.out.println("✓ 签名验证测试成功！");
            } else {
                System.out.println("✗ 签名验证测试失败！");
            }

            // 测试使用服务器公钥（仅加密，无法解密）
            System.out.println("\n--- 使用服务器公钥加密（无法本地解密） ---");
            String serverPublicKey = ServerConfig.getRsaPublicKey();
            String serverEncrypted = RsaEncryptUtils.encrypt(testData, serverPublicKey);
            System.out.println("使用服务器公钥加密: " + serverEncrypted);
            System.out.println("注意：此数据只能由服务器私钥解密");
            
        } catch (Exception e) {
            // 不使用 printStackTrace：示例不应示范把原始栈打印到 stderr 的写法
            System.err.println("RSA加密测试失败: " + describe(e));
        }
    }
    
    /**
     * 测试工具类
     */
    public static void testUtils() {
        System.out.println("\n=== 测试工具类 ===");
        
        // 测试十六进制转换
        String hex = "48656c6c6f"; // "Hello"的十六进制
        byte[] bytes = AppUtils.hexToBytes(hex);
        System.out.println("十六进制转字节: " + AppUtils.bytesToString(bytes));
        
        // 测试Base64
        String base64 = AppUtils.stringToBase64("Hello World");
        System.out.println("字符串转Base64: " + base64);
        System.out.println("Base64转字符串: " + AppUtils.base64ToString(base64));
        
        // 测试随机字符串生成
        String randomStr = AppUtils.generateRandomString(10);
        System.out.println("随机字符串: " + randomStr);
        
        String randomNum = AppUtils.generateRandomNumber(8);
        System.out.println("随机数字: " + randomNum);
    }
    
    /**
     * 显示服务器配置
     */
    public static void showServerConfig() {
        System.out.println("\n=== 服务器配置 ===");
        System.out.println("服务器IP: " + ServerConfig.getServerIp());
        System.out.println("TCP端口: " + ServerConfig.getTcpPort());
        System.out.println("HTTP端口: " + ServerConfig.getHttpPort());
        System.out.println("缓存启用: " + ServerConfig.isCacheEnabled());
    }
    
    /**
     * 主方法
     */
    public static void main(String[] args) {
        System.out.println("YaoGuangYun 网络请求迁移示例");
        System.out.println("============================");
        
        System.out.println("\n注意: 以下测试可能因为服务器不可用而失败，这是正常现象。");
        System.out.println("项目的主要目的是展示从bbc.me应用迁移过来的代码结构和功能。");
        
        // 显示服务器配置
        showServerConfig();
        
        // 测试设备信息收集
        testDeviceInfoCollection();
        
        // 测试工具类
        testUtils();
        
        // 测试RSA加密（使用本地生成的密钥对）
        testRsaEncryption();
        
        // 测试HTTP请求
        testHttpRequest();
        
        // 测试TCP连接
        testTcpConnection();
        
        // 测试Gson功能
        GsonExample.runExamples();
        
        // 测试Hutool功能
        HutoolExample.runExamples();
        
        System.out.println("\n============================");
        System.out.println("所有测试完成！");
    }
}