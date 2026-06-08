package com.yaoguangyun.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 应用工具类
 * 基于bbc.me应用的AppUtils实现
 * 对应原始类: bbc.me.AppUtils
 * 
 * 功能特点：
 * 1. 十六进制转换
 * 2. Base64编码/解码
 * 3. 字符串编码转换
 * 4. 随机字符串生成
 * 5. dp/sp/px转换（模拟Android）
 * 
 * 使用场景：
 * - 数据格式转换
 * - 编码处理
 * - 测试数据生成
 * - 屏幕适配计算
 */
public class AppUtils {
    
    /**
     * 将字节数组转换为十六进制字符串
     * 
     * 将每个字节转换为两位十六进制数
     * 例如: [0x48, 0x65, 0x6C, 0x6C, 0x6F] -> "48656c6c6f"
     * 
     * @param bytes 字节数组
     * @return 十六进制字符串（小写）
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    /**
     * 将十六进制字符串转换为字节数组
     * 
     * 将每两位十六进制数转换为一个字节
     * 例如: "48656c6c6f" -> [0x48, 0x65, 0x6C, 0x6C, 0x6F]
     * 
     * @param hex 十六进制字符串
     * @return 字节数组
     * @throws IllegalArgumentException 如果字符串包含无效的十六进制字符
     */
    public static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
    
    /**
     * 将字节数组转换为Base64字符串
     * 
     * @param bytes 字节数组
     * @return Base64编码的字符串
     */
    public static String bytesToBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
    
    /**
     * 将Base64字符串转换为字节数组
     * @param base64 Base64字符串
     * @return 字节数组
     */
    public static byte[] base64ToBytes(String base64) {
        return Base64.getDecoder().decode(base64);
    }
    
    /**
     * 将字符串转换为字节数组
     * @param str 字符串
     * @return 字节数组
     */
    public static byte[] stringToBytes(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }
    
    /**
     * 将字节数组转换为字符串
     * @param bytes 字节数组
     * @return 字符串
     */
    public static String bytesToString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    /**
     * 将字符串转换为Base64
     * @param str 字符串
     * @return Base64字符串
     */
    public static String stringToBase64(String str) {
        return bytesToBase64(stringToBytes(str));
    }
    
    /**
     * 将Base64转换为字符串
     * @param base64 Base64字符串
     * @return 字符串
     */
    public static String base64ToString(String base64) {
        return bytesToString(base64ToBytes(base64));
    }
    
    /**
     * 模拟Android的dp转px
     * @param dp dp值
     * @return px值
     */
    public static int dp2px(float dp) {
        // 在Android中，需要根据屏幕密度转换
        // 这里简化处理，假设密度为3.0
        return (int) (dp * 3.0f);
    }
    
    /**
     * 模拟Android的px转dp
     * @param px px值
     * @return dp值
     */
    public static int px2dp(float px) {
        // 在Android中，需要根据屏幕密度转换
        // 这里简化处理，假设密度为3.0
        return (int) (px / 3.0f);
    }
    
    /**
     * 模拟Android的sp转px
     * @param sp sp值
     * @return px值
     */
    public static int sp2px(float sp) {
        // 在Android中，需要根据屏幕密度和字体密度转换
        // 这里简化处理，假设密度为3.0
        return (int) (sp * 3.0f);
    }
    
    /**
     * 生成随机字符串
     * @param length 长度
     * @return 随机字符串
     */
    public static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) (chars.length() * Math.random());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }
    
    /**
     * 生成随机数字
     * @param length 长度
     * @return 随机数字
     */
    public static String generateRandomNumber(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append((int) (10 * Math.random()));
        }
        return sb.toString();
    }
}