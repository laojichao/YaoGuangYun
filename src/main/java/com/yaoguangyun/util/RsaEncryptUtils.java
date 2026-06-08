package com.yaoguangyun.util;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;

/**
 * RSA加密工具类
 * 基于bbc.me应用的RsaEncryptUtils实现
 * 对应原始类: bbc.me.RsaEncryptUtils
 * 
 * 功能特点：
 * 1. RSA公钥加密
 * 2. RSA私钥解密
 * 3. RSA密钥对生成
 * 4. Base64编码/解码
 * 5. 支持PKCS1Padding填充模式
 * 
 * RSA加密原理：
 * - 公钥加密，私钥解密
 * - 私钥签名，公钥验证
 * 
 * 使用场景：
 * - 敏感数据加密传输
 * - 设备信息加密
 * - 身份验证数据加密
 */
public class RsaEncryptUtils {
    
    /** RSA算法名称 */
    private static final String RSA_ALGORITHM = "RSA";
    /** RSA加密转换模式 */
    private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";
    /** 密钥长度 */
    private static final int KEY_SIZE = 2048;
    
    /**
     * 生成RSA密钥对
     * 
     * @return 包含公钥和私钥的KeyPair对象
     */
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            keyPairGenerator.initialize(KEY_SIZE);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate RSA key pair", e);
        }
    }
    
    /**
     * 获取公钥的Base64编码
     * 
     * @param keyPair 密钥对
     * @return Base64编码的公钥
     */
    public static String getPublicKeyBase64(KeyPair keyPair) {
        PublicKey publicKey = keyPair.getPublic();
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
    
    /**
     * 获取私钥的Base64编码
     * 
     * @param keyPair 密钥对
     * @return Base64编码的私钥
     */
    public static String getPrivateKeyBase64(KeyPair keyPair) {
        PrivateKey privateKey = keyPair.getPrivate();
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }
    
    /**
     * 使用RSA公钥加密数据
     * 
     * 将明文数据使用RSA公钥进行加密
     * 返回Base64编码的密文
     * 
     * @param data 待加密数据（明文）
     * @param publicKeyBase64 Base64编码的RSA公钥
     * @return 加密后的Base64字符串
     * @throws RuntimeException 加密失败时抛出异常
     */
    public static String encrypt(String data, String publicKeyBase64) {
        try {
            // 解码公钥
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            
            // 加密数据
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedData = cipher.doFinal(data.getBytes("UTF-8"));
            
            // 返回Base64编码的加密数据
            return Base64.getEncoder().encodeToString(encryptedData);
            
        } catch (Exception e) {
            throw new RuntimeException("RSA encryption failed", e);
        }
    }
    
    /**
     * 使用RSA私钥解密数据
     * 
     * 将Base64编码的密文使用RSA私钥进行解密
     * 返回解密后的明文数据
     * 
     * @param encryptedData Base64编码的加密数据
     * @param privateKeyBase64 Base64编码的RSA私钥
     * @return 解密后的字符串
     * @throws RuntimeException 解密失败时抛出异常
     */
    public static String decrypt(String encryptedData, String privateKeyBase64) {
        try {
            // 解码私钥
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            
            // 解密数据
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decodedData = Base64.getDecoder().decode(encryptedData);
            byte[] decryptedData = cipher.doFinal(decodedData);
            
            return new String(decryptedData, "UTF-8");
            
        } catch (Exception e) {
            throw new RuntimeException("RSA decryption failed", e);
        }
    }
    
    /**
     * 使用RSA私钥加密数据（用于签名）
     * 
     * @param data 待加密数据
     * @param privateKeyBase64 Base64编码的RSA私钥
     * @return 加密后的Base64字符串
     * @throws RuntimeException 加密失败时抛出异常
     */
    public static String encryptWithPrivateKey(String data, String privateKeyBase64) {
        try {
            // 解码私钥
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            
            // 加密数据
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            byte[] encryptedData = cipher.doFinal(data.getBytes("UTF-8"));
            
            // 返回Base64编码的加密数据
            return Base64.getEncoder().encodeToString(encryptedData);
            
        } catch (Exception e) {
            throw new RuntimeException("RSA encryption with private key failed", e);
        }
    }
    
    /**
     * 使用RSA公钥解密数据（用于验证签名）
     * 
     * @param encryptedData Base64编码的加密数据
     * @param publicKeyBase64 Base64编码的RSA公钥
     * @return 解密后的字符串
     * @throws RuntimeException 解密失败时抛出异常
     */
    public static String decryptWithPublicKey(String encryptedData, String publicKeyBase64) {
        try {
            // 解码公钥
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            
            // 解密数据
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            byte[] decodedData = Base64.getDecoder().decode(encryptedData);
            byte[] decryptedData = cipher.doFinal(decodedData);
            
            return new String(decryptedData, "UTF-8");
            
        } catch (Exception e) {
            throw new RuntimeException("RSA decryption with public key failed", e);
        }
    }
}