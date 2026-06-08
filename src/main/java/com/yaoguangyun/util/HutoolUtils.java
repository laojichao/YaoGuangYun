package com.yaoguangyun.util;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hutool工具类
 * 基于Hutool工具包的便捷方法封装
 * 
 * Hutool是一个Java工具包，提供了很多实用的工具类：
 * 1. 核心工具 (hutool-core)
 * 2. HTTP客户端 (hutool-http)
 * 3. JSON处理 (hutool-json)
 * 4. 加密解密 (hutool-crypto)
 * 5. 网络工具 (hutool-socket)
 */
public class HutoolUtils {
    
    // ==================== 字符串工具 ====================
    
    /**
     * 检查字符串是否为空
     * @param str 字符串
     * @return 是否为空
     */
    public static boolean isEmpty(String str) {
        return StrUtil.isEmpty(str);
    }
    
    /**
     * 检查字符串是否不为空
     * @param str 字符串
     * @return 是否不为空
     */
    public static boolean isNotEmpty(String str) {
        return StrUtil.isNotEmpty(str);
    }
    
    /**
     * 检查字符串是否为空白
     * @param str 字符串
     * @return 是否为空白
     */
    public static boolean isBlank(String str) {
        return StrUtil.isBlank(str);
    }
    
    /**
     * 检查字符串是否不为空白
     * @param str 字符串
     * @return 是否不为空白
     */
    public static boolean isNotBlank(String str) {
        return StrUtil.isNotBlank(str);
    }
    
    /**
     * 格式化字符串
     * @param template 模板
     * @param params 参数
     * @return 格式化后的字符串
     */
    public static String format(String template, Object... params) {
        return StrUtil.format(template, params);
    }
    
    /**
     * 截取字符串
     * @param str 字符串
     * @param from 起始位置
     * @param to 结束位置
     * @return 截取后的字符串
     */
    public static String sub(String str, int from, int to) {
        return StrUtil.sub(str, from, to);
    }
    
    // ==================== 日期工具 ====================
    
    /**
     * 获取当前时间字符串
     * @return 时间字符串
     */
    public static String now() {
        return DateUtil.now();
    }
    
    /**
     * 获取当前日期字符串
     * @return 日期字符串
     */
    public static String today() {
        return DateUtil.today();
    }
    
    /**
     * 格式化日期
     * @param date 日期
     * @param format 格式
     * @return 格式化后的日期字符串
     */
    public static String formatDate(Date date, String format) {
        return DateUtil.format(date, format);
    }
    
    /**
     * 解析日期字符串
     * @param dateStr 日期字符串
     * @param format 格式
     * @return Date对象
     */
    public static Date parseDate(String dateStr, String format) {
        return DateUtil.parse(dateStr, format);
    }
    
    // ==================== ID生成工具 ====================
    
    /**
     * 生成UUID
     * @return UUID字符串
     */
    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * 生成简单UUID（无横线）
     * @return 简单UUID字符串
     */
    public static String simpleUUID() {
        return IdUtil.simpleUUID();
    }
    
    /**
     * 生成雪花ID
     * @return 雪花ID
     */
    public static long snowflakeId() {
        return IdUtil.getSnowflakeNextId();
    }
    
    // ==================== 随机工具 ====================
    
    /**
     * 生成随机整数
     * @param min 最小值
     * @param max 最大值
     * @return 随机整数
     */
    public static int randomInt(int min, int max) {
        return RandomUtil.randomInt(min, max);
    }
    
    /**
     * 生成随机字符串
     * @param length 长度
     * @return 随机字符串
     */
    public static String randomString(int length) {
        return RandomUtil.randomString(length);
    }
    
    /**
     * 生成随机数字字符串
     * @param length 长度
     * @return 随机数字字符串
     */
    public static String randomNumbers(int length) {
        return RandomUtil.randomNumbers(length);
    }
    
    // ==================== 编码工具 ====================
    
    /**
     * Base64编码
     * @param data 数据
     * @return Base64字符串
     */
    public static String base64Encode(String data) {
        return Base64.encode(data, "UTF-8");
    }
    
    /**
     * Base64解码
     * @param base64 Base64字符串
     * @return 解码后的字符串
     */
    public static String base64Decode(String base64) {
        return Base64.decodeStr(base64, "UTF-8");
    }
    
    /**
     * 十六进制编码
     * @param data 数据
     * @return 十六进制字符串
     */
    public static String hexEncode(String data) {
        return HexUtil.encodeHexStr(data, StandardCharsets.UTF_8);
    }
    
    /**
     * 十六进制解码
     * @param hex 十六进制字符串
     * @return 解码后的字符串
     */
    public static String hexDecode(String hex) {
        return HexUtil.decodeHexStr(hex, StandardCharsets.UTF_8);
    }
    
    // ==================== 加密工具 ====================
    
    /**
     * MD5加密
     * @param data 数据
     * @return MD5字符串
     */
    public static String md5(String data) {
        return DigestUtil.md5Hex(data);
    }
    
    /**
     * SHA256加密
     * @param data 数据
     * @return SHA256字符串
     */
    public static String sha256(String data) {
        return DigestUtil.sha256Hex(data);
    }
    
    /**
     * 生成RSA密钥对
     * @return 密钥对
     */
    public static KeyPair generateRSAKeyPair() {
        return SecureUtil.generateKeyPair("RSA");
    }
    
    /**
     * RSA加密
     * @param data 数据
     * @param publicKeyBase64 公钥（Base64编码）
     * @return 加密后的数据
     */
    public static String rsaEncrypt(String data, String publicKeyBase64) {
        RSA rsa = new RSA(null, publicKeyBase64);
        return rsa.encryptBase64(data, KeyType.PublicKey);
    }
    
    /**
     * RSA解密
     * @param encryptedData 加密数据
     * @param privateKeyBase64 私钥（Base64编码）
     * @return 解密后的数据
     */
    public static String rsaDecrypt(String encryptedData, String privateKeyBase64) {
        RSA rsa = new RSA(privateKeyBase64, null);
        return rsa.decryptStr(encryptedData, KeyType.PrivateKey);
    }
    
    // ==================== HTTP工具 ====================
    
    /**
     * 发送GET请求
     * @param url URL
     * @return 响应内容
     */
    public static String httpGet(String url) {
        return HttpUtil.get(url);
    }
    
    /**
     * 发送POST请求
     * @param url URL
     * @param data 请求数据
     * @return 响应内容
     */
    public static String httpPost(String url, String data) {
        return HttpUtil.post(url, data);
    }
    
    /**
     * 发送POST请求（表单）
     * @param url URL
     * @param params 表单参数
     * @return 响应内容
     */
    public static String httpPostForm(String url, Map<String, Object> params) {
        return HttpUtil.post(url, params);
    }
    
    /**
     * 发送自定义HTTP请求
     * @param url URL
     * @param method 请求方法
     * @param headers 请求头
     * @param body 请求体
     * @return 响应对象
     */
    public static HttpResponse httpRequest(String url, String method, Map<String, String> headers, String body) {
        HttpRequest request = HttpRequest.of(url)
            .method(cn.hutool.http.Method.valueOf(method));
        
        // 设置请求头
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                request.header(entry.getKey(), entry.getValue());
            }
        }
        
        if (body != null && !body.isEmpty()) {
            request.body(body);
        }
        
        return request.execute();
    }
    
    // ==================== JSON工具 ====================
    
    /**
     * 对象转JSON字符串
     * @param obj 对象
     * @return JSON字符串
     */
    public static String toJson(Object obj) {
        return JSONUtil.toJsonStr(obj);
    }
    
    /**
     * JSON字符串转对象
     * @param json JSON字符串
     * @param clazz 目标类
     * @param <T> 目标类型
     * @return 对象实例
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        return JSONUtil.toBean(json, clazz);
    }
    
    /**
     * JSON字符串转Map
     * @param json JSON字符串
     * @return Map对象
     */
    public static Map<String, Object> toMap(String json) {
        return JSONUtil.toBean(json, Map.class);
    }
    
    /**
     * 格式化JSON
     * @param json JSON字符串
     * @return 格式化后的JSON
     */
    public static String formatJson(String json) {
        return JSONUtil.formatJsonStr(json);
    }
    
    // ==================== 集合工具 ====================
    
    /**
     * 创建List
     * @param values 值
     * @param <T> 类型
     * @return List对象
     */
    @SafeVarargs
    public static <T> List<T> listOf(T... values) {
        return CollUtil.toList(values);
    }
    
    /**
     * 创建Map
     * @return Map对象
     */
    public static <K, V> Map<K, V> mapOf() {
        return new HashMap<>();
    }
    
    /**
     * 创建Map
     * @param key 键
     * @param value 值
     * @return Map对象
     */
    public static <K, V> Map<K, V> mapOf(K key, V value) {
        Map<K, V> map = new HashMap<>();
        map.put(key, value);
        return map;
    }
    
    // ==================== 文件工具 ====================
    
    /**
     * 读取文件内容
     * @param filePath 文件路径
     * @return 文件内容
     */
    public static String readString(String filePath) {
        return FileUtil.readString(filePath, CharsetUtil.CHARSET_UTF_8);
    }
    
    /**
     * 写入文件内容
     * @param filePath 文件路径
     * @param content 内容
     */
    public static void writeString(String filePath, String content) {
        FileUtil.writeString(content, filePath, CharsetUtil.CHARSET_UTF_8);
    }
    
    /**
     * 检查文件是否存在
     * @param filePath 文件路径
     * @return 是否存在
     */
    public static boolean fileExists(String filePath) {
        return FileUtil.exist(filePath);
    }
    
    /**
     * 创建目录
     * @param dirPath 目录路径
     * @return 目录对象
     */
    public static File mkdir(String dirPath) {
        return FileUtil.mkdir(dirPath);
    }
}