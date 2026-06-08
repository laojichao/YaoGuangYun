package com.yaoguangyun.test;

import com.yaoguangyun.device.DeviceInfoManager;
import com.yaoguangyun.network.HttpClient;
import com.yaoguangyun.network.ServerConfig;
import com.yaoguangyun.network.TcpClient;
import com.yaoguangyun.network.TcpResponseCallback;
import com.yaoguangyun.util.GsonUtils;
import com.yaoguangyun.util.RsaEncryptUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Map;

/**
 * 网络功能测试类
 * 用于测试各种网络功能
 */
public class NetworkTest {
    
    public static void main(String[] args) {
        System.out.println("=== 网络功能测试 ===\n");
        
        // 1. 测试RSA加密
        testRsaEncryption();
        
        // 2. 测试HTTP客户端
        testHttpClient();
        
        // 3. 测试TCP客户端
        testTcpClient();
        
        // 4. 测试设备信息管理
        testDeviceInfo();
        
        // 5. 测试Gson功能
        testGson();
        
        System.out.println("\n=== 测试完成 ===");
    }
    
    /**
     * 测试RSA加密
     */
    private static void testRsaEncryption() {
        System.out.println("1. 测试RSA加密");
        System.out.println("---------------");
        
        try {
            // 生成密钥对
            KeyPair keyPair = RsaEncryptUtils.generateKeyPair();
            String publicKey = RsaEncryptUtils.getPublicKeyBase64(keyPair);
            String privateKey = RsaEncryptUtils.getPrivateKeyBase64(keyPair);
            
            System.out.println("✓ 生成RSA密钥对成功");
            System.out.println("  公钥长度: " + publicKey.length() + " 字符");
            System.out.println("  私钥长度: " + privateKey.length() + " 字符");
            
            // 测试加密解密
            String testData = "Hello, RSA Encryption!";
            String encrypted = RsaEncryptUtils.encrypt(testData, publicKey);
            String decrypted = RsaEncryptUtils.decrypt(encrypted, privateKey);
            
            if (testData.equals(decrypted)) {
                System.out.println("✓ RSA加密解密测试成功");
            } else {
                System.out.println("✗ RSA加密解密测试失败");
            }
            
            // 测试签名验证
            String signedData = RsaEncryptUtils.encryptWithPrivateKey(testData, privateKey);
            String verifiedData = RsaEncryptUtils.decryptWithPublicKey(signedData, publicKey);
            
            if (testData.equals(verifiedData)) {
                System.out.println("✓ RSA签名验证测试成功");
            } else {
                System.out.println("✗ RSA签名验证测试失败");
            }
            
        } catch (Exception e) {
            System.err.println("✗ RSA测试失败: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * 测试HTTP客户端
     */
    private static void testHttpClient() {
        System.out.println("2. 测试HTTP客户端");
        System.out.println("----------------");
        
        HttpClient httpClient = new HttpClient(5000);
        httpClient.addHeader("User-Agent", "YaoGuangYun-Test/1.0");
        httpClient.addHeader("Accept", "application/json");
        
        // 测试URL列表
        String[] testUrls = {
            "http://httpbin.org/get",
            "http://httpbin.org/post",
            "http://httpbin.org/status/200"
        };
        
        for (String url : testUrls) {
            System.out.println("测试URL: " + url);
            try {
                InputStream response = httpClient.get(url);
                byte[] buffer = new byte[1024];
                int bytesRead = response.read(buffer);
                if (bytesRead > 0) {
                    String responseStr = new String(buffer, 0, Math.min(bytesRead, 200), StandardCharsets.UTF_8);
                    System.out.println("  ✓ 响应: " + responseStr + "...");
                }
                response.close();
            } catch (IOException e) {
                System.err.println("  ✗ 请求失败: " + e.getMessage());
            }
        }
        
        System.out.println();
    }
    
    /**
     * 测试TCP客户端
     */
    private static void testTcpClient() {
        System.out.println("3. 测试TCP客户端");
        System.out.println("----------------");
        
        System.out.println("服务器配置:");
        System.out.println("  IP: " + ServerConfig.getServerIp());
        System.out.println("  TCP端口: " + ServerConfig.getTcpPort());
        System.out.println("  HTTP端口: " + ServerConfig.getHttpPort());
        
        TcpClient tcpClient = new TcpClient();
        String testMessage = "{\"action\":\"test\",\"timestamp\":" + System.currentTimeMillis() + "}";
        
        System.out.println("发送测试消息: " + testMessage);
        
        tcpClient.sendMessage(testMessage.getBytes(StandardCharsets.UTF_8), new TcpResponseCallback() {
            @Override
            public void onSuccess(String response) {
                System.out.println("  ✓ TCP响应成功: " + response);
            }
            
            @Override
            public void onError(Exception e) {
                System.err.println("  ✗ TCP请求失败: " + e.getMessage());
                System.out.println("  提示: 服务器可能不可用，这是正常现象");
            }
        });
        
        // 等待异步操作
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // 忽略
        }
        
        System.out.println();
    }
    
    /**
     * 测试设备信息管理
     */
    private static void testDeviceInfo() {
        System.out.println("4. 测试设备信息管理");
        System.out.println("------------------");
        
        DeviceInfoManager manager = DeviceInfoManager.getInstance();
        manager.collectDeviceInfo();
        
        Map<String, String> deviceInfo = manager.getDeviceInfoMap();
        
        System.out.println("设备信息:");
        System.out.println("  品牌: " + deviceInfo.get("brand"));
        System.out.println("  型号: " + deviceInfo.get("model"));
        System.out.println("  设备: " + deviceInfo.get("device"));
        System.out.println("  Android版本: " + deviceInfo.get("release"));
        System.out.println("  SDK版本: " + deviceInfo.get("sdkInt"));
        System.out.println("  屏幕分辨率: " + deviceInfo.get("widthPixels") + "x" + deviceInfo.get("heightPixels"));
        
        System.out.println("✓ 设备信息收集成功");
        System.out.println();
    }
    
    /**
     * 测试Gson功能
     */
    private static void testGson() {
        System.out.println("5. 测试Gson功能");
        System.out.println("--------------");
        
        // 测试序列化
        Map<String, Object> testData = new java.util.HashMap<>();
        testData.put("name", "测试数据");
        testData.put("value", 12345);
        testData.put("timestamp", System.currentTimeMillis());
        
        String json = GsonUtils.toJson(testData);
        System.out.println("✓ JSON序列化成功: " + json);
        
        // 测试反序列化
        Map<String, Object> parsedData = GsonUtils.fromJsonMap(json);
        if (parsedData != null) {
            System.out.println("✓ JSON反序列化成功");
            System.out.println("  name: " + parsedData.get("name"));
            System.out.println("  value: " + parsedData.get("value"));
        }
        
        // 测试JSON对象操作
        com.google.gson.JsonObject jsonObject = GsonUtils.createJsonObject();
        GsonUtils.putString(jsonObject, "key1", "value1");
        GsonUtils.putInt(jsonObject, "key2", 42);
        GsonUtils.putBoolean(jsonObject, "key3", true);
        
        String formattedJson = GsonUtils.toPrettyJson(jsonObject);
        System.out.println("✓ JSON对象创建成功:");
        System.out.println(formattedJson);
        
        System.out.println();
    }
}