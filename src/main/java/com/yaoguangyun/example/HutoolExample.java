package com.yaoguangyun.example;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Hutool使用示例
 * 
 * 演示Hutool工具包的各种功能
 */
public class HutoolExample {
    
    /**
     * 运行所有示例
     */
    public static void runExamples() {
        System.out.println("=== Hutool使用示例 ===\n");
        
        // 1. 字符串工具示例
        stringExample();
        
        // 2. 日期工具示例
        dateExample();
        
        // 3. ID生成示例
        idExample();
        
        // 4. 随机工具示例
        randomExample();
        
        // 5. 编码工具示例
        encodingExample();
        
        // 6. 加密工具示例
        cryptoExample();
        
        // 7. HTTP工具示例
        httpExample();
        
        // 8. JSON工具示例
        jsonExample();
        
        System.out.println("=== Hutool示例完成 ===\n");
    }
    
    /**
     * 字符串工具示例
     */
    private static void stringExample() {
        System.out.println("--- 字符串工具示例 ---");
        
        // 检查字符串
        System.out.println("isEmpty(null): " + StrUtil.isEmpty(null));
        System.out.println("isEmpty(\"\"): " + StrUtil.isEmpty(""));
        System.out.println("isEmpty(\"test\"): " + StrUtil.isEmpty("test"));
        System.out.println("isBlank(\"  \"): " + StrUtil.isBlank("  "));
        
        // 格式化字符串
        String formatted = StrUtil.format("Hello, {}! You are {} years old.", "张三", 25);
        System.out.println("格式化: " + formatted);
        
        // 截取字符串
        String sub = StrUtil.sub("Hello World", 0, 5);
        System.out.println("截取: " + sub);
        
        System.out.println();
    }
    
    /**
     * 日期工具示例
     */
    private static void dateExample() {
        System.out.println("--- 日期工具示例 ---");
        
        // 当前时间
        System.out.println("当前时间: " + DateUtil.now());
        System.out.println("今天日期: " + DateUtil.today());
        
        // 格式化日期
        Date now = new Date();
        String formatted = DateUtil.format(now, "yyyy-MM-dd HH:mm:ss");
        System.out.println("格式化日期: " + formatted);
        
        // 解析日期
        Date parsed = DateUtil.parse("2026-06-02 10:30:00", "yyyy-MM-dd HH:mm:ss");
        System.out.println("解析日期: " + parsed);
        
        System.out.println();
    }
    
    /**
     * ID生成示例
     */
    private static void idExample() {
        System.out.println("--- ID生成示例 ---");
        
        // UUID
        System.out.println("UUID: " + IdUtil.randomUUID());
        System.out.println("简单UUID: " + IdUtil.simpleUUID());
        
        // 雪花ID
        System.out.println("雪花ID: " + IdUtil.getSnowflakeNextId());
        
        System.out.println();
    }
    
    /**
     * 随机工具示例
     */
    private static void randomExample() {
        System.out.println("--- 随机工具示例 ---");
        
        // 随机整数
        System.out.println("随机整数(1-100): " + RandomUtil.randomInt(1, 100));
        
        // 随机字符串
        System.out.println("随机字符串(8位): " + RandomUtil.randomString(8));
        
        // 随机数字
        System.out.println("随机数字(6位): " + RandomUtil.randomNumbers(6));
        
        System.out.println();
    }
    
    /**
     * 编码工具示例
     */
    private static void encodingExample() {
        System.out.println("--- 编码工具示例 ---");
        
        // Base64编码
        String original = "Hello, Hutool!";
        String encoded = Base64.encode(original, "UTF-8");
        String decoded = Base64.decodeStr(encoded, "UTF-8");
        
        System.out.println("原始数据: " + original);
        System.out.println("Base64编码: " + encoded);
        System.out.println("Base64解码: " + decoded);
        
        System.out.println();
    }
    
    /**
     * 加密工具示例
     */
    private static void cryptoExample() {
        System.out.println("--- 加密工具示例 ---");
        
        // MD5加密
        String data = "Hello, Hutool!";
        String md5 = DigestUtil.md5Hex(data);
        System.out.println("MD5加密: " + md5);
        
        // SHA256加密
        String sha256 = DigestUtil.sha256Hex(data);
        System.out.println("SHA256加密: " + sha256);
        
        // RSA加密
        try {
            // 使用Hutool的RSA工具
            cn.hutool.crypto.asymmetric.RSA rsa = new cn.hutool.crypto.asymmetric.RSA();
            String publicKey = rsa.getPublicKeyBase64();
            String privateKey = rsa.getPrivateKeyBase64();
            
            System.out.println("RSA公钥长度: " + publicKey.length());
            System.out.println("RSA私钥长度: " + privateKey.length());
            
            String encrypted = rsa.encryptBase64(data, cn.hutool.crypto.asymmetric.KeyType.PublicKey);
            String decrypted = rsa.decryptStr(encrypted, cn.hutool.crypto.asymmetric.KeyType.PrivateKey);
            
            System.out.println("RSA加密成功: " + (encrypted != null && !encrypted.isEmpty()));
            System.out.println("RSA解密成功: " + data.equals(decrypted));
        } catch (Exception e) {
            System.err.println("RSA加密失败: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * HTTP工具示例
     */
    private static void httpExample() {
        System.out.println("--- HTTP工具示例 ---");
        
        try {
            // GET请求
            String getResponse = HttpUtil.get("http://httpbin.org/get");
            System.out.println("GET请求成功: " + (getResponse != null && !getResponse.isEmpty()));
            
            // POST请求
            Map<String, Object> params = new HashMap<>();
            params.put("username", "test");
            params.put("password", "123456");
            
            String postResponse = HttpUtil.post("http://httpbin.org/post", params);
            System.out.println("POST请求成功: " + (postResponse != null && !postResponse.isEmpty()));
            
            // 自定义请求
            HttpResponse response = HttpRequest.post("http://httpbin.org/post")
                .header("Content-Type", "application/json")
                .header("User-Agent", "Hutool-Test/1.0")
                .body("{\"key\":\"value\"}")
                .execute();
            
            System.out.println("自定义请求成功: " + response.isOk());
            System.out.println("响应状态码: " + response.getStatus());
        } catch (Exception e) {
            System.err.println("HTTP请求失败: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * JSON工具示例
     */
    private static void jsonExample() {
        System.out.println("--- JSON工具示例 ---");
        
        // 创建JSON对象
        JSONObject json = new JSONObject();
        json.set("name", "张三");
        json.set("age", 25);
        json.set("city", "北京");
        
        // 转换为字符串
        String jsonStr = json.toString();
        System.out.println("JSON字符串: " + jsonStr);
        
        // 格式化JSON
        String formatted = JSONUtil.formatJsonStr(jsonStr);
        System.out.println("格式化JSON:\n" + formatted);
        
        // 解析JSON
        JSONObject parsed = JSONUtil.parseObj(jsonStr);
        System.out.println("解析JSON: name=" + parsed.getStr("name") + ", age=" + parsed.getInt("age"));
        
        // 使用Bean
        Map<String, Object> map = new HashMap<>();
        map.put("brand", "Samsung");
        map.put("model", "SM-G950F");
        map.put("release", "8.0.0");
        
        String mapJson = JSONUtil.toJsonStr(map);
        System.out.println("Map转JSON: " + mapJson);
        
        Map<String, Object> parsedMap = JSONUtil.toBean(mapJson, Map.class);
        System.out.println("JSON转Map: brand=" + parsedMap.get("brand"));
        
        System.out.println();
    }
}