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
                System.err.println("✗ TCP请求失败: " + e.getMessage());
                System.out.println("提示: 如果服务器不可用，这是正常现象");
            }
        });
        
        // 等待异步操作完成
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // 忽略中断异常
        }
    }
    
    /**
     * 测试HTTP请求
     */
    public static void testHttpRequest() {
        System.out.println("\n=== 测试HTTP请求 ===");
        
        // 测试URL列表
        String[] testUrls = {
            "http://httpbin.org/get",  // 公共测试API
            "http://" + ServerConfig.getServerIp() + ":" + ServerConfig.getHttpPort() + "/api/test"
        };
        
        HttpClient httpClient = new HttpClient(5000);
        
        // 添加请求头
        httpClient.addHeader("User-Agent", "Mozilla/5.0");
        httpClient.addHeader("Accept", "application/json");
        
        for (String url : testUrls) {
            System.out.println("\n测试URL: " + url);
            try {
                InputStream response = httpClient.get(url);
                
                // 读取响应
                byte[] buffer = new byte[1024];
                int bytesRead = response.read(buffer);
                if (bytesRead > 0) {
                    String responseStr = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                    System.out.println("HTTP响应: " + responseStr);
                } else {
                    System.out.println("HTTP响应: 空响应");
                }
                
                response.close();
                System.out.println("✓ HTTP请求成功");
                
            } catch (IOException e) {
                System.err.println("✗ HTTP请求失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 测试设备信息收集
     */
    public static void testDeviceInfoCollection() {
        System.out.println("\n=== 测试设备信息收集 ===");
        
        DeviceInfoManager deviceInfoManager = DeviceInfoManager.getInstance();
        deviceInfoManager.collectDeviceInfo();
        
        // 显示部分设备信息
        Map<String, String> deviceInfo = deviceInfoManager.getDeviceInfoMap();
        System.out.println("品牌: " + deviceInfo.get("brand"));
        System.out.println("型号: " + deviceInfo.get("model"));
        System.out.println("Android版本: " + deviceInfo.get("release"));
        System.out.println("SDK版本: " + deviceInfo.get("sdkInt"));
        System.out.println("屏幕分辨率: " + deviceInfo.get("widthPixels") + "x" + deviceInfo.get("heightPixels"));
        System.out.println("MAC地址: " + deviceInfo.get("macAddress"));
        System.out.println("IP地址: " + deviceInfo.get("ipAddress"));
        System.out.println("网络类型: " + deviceInfo.get("networkType"));
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
            
            // 使用私钥加密，公钥解密（签名流程）
            System.out.println("\n--- 私钥加密，公钥解密（签名） ---");
            String signedData = RsaEncryptUtils.encryptWithPrivateKey(testData, privateKey);
            System.out.println("签名后: " + signedData);
            
            String verifiedData = RsaEncryptUtils.decryptWithPublicKey(signedData, publicKey);
            System.out.println("验证后: " + verifiedData);
            
            // 验证签名结果
            if (testData.equals(verifiedData)) {
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
            System.err.println("RSA加密测试失败: " + e.getMessage());
            e.printStackTrace();
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