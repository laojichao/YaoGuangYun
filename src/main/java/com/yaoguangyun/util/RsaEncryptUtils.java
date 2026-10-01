package com.yaoguangyun.util;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
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
 * 4. 数字签名与验签（SHA256withRSA）
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
 *
 * <p><b>容量限制</b>：RSA 单块加密的明文长度上限为
 * {@code 密钥长度(字节) - 填充开销(11)}，2048 位密钥即 245 字节。
 * 超出会抛 {@link IllegalArgumentException}。</p>
 */
public class RsaEncryptUtils {
    
    /** RSA算法名称 */
    private static final String RSA_ALGORITHM = "RSA";
    /** RSA加密转换模式 */
    private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";
    /** 签名算法（带哈希，勿用纯 RSA 加密代替签名） */
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    /** 密钥长度 */
    private static final int KEY_SIZE = 2048;
    /** PKCS#1 v1.5 填充开销（字节） */
    private static final int PKCS1_PADDING_OVERHEAD = 11;
    
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
     * 获取 RSA 密钥模长的字节数（2048 位 → 256 字节）
     */
    private static int modulusByteLength(PublicKey publicKey) {
        if (publicKey instanceof java.security.interfaces.RSAPublicKey) {
            return (((java.security.interfaces.RSAPublicKey) publicKey).getModulus().bitLength() + 7) / 8;
        }
        return KEY_SIZE / 8;
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
     * @param data 待加密数据（明文），UTF-8 编码后不得超过 {@code 密钥字节数 - 11}（2048 位密钥为 245 字节）
     * @param publicKeyBase64 Base64编码的RSA公钥
     * @return 加密后的Base64字符串
     * @throws IllegalArgumentException 明文超出单块 RSA 加密容量
     * @throws RuntimeException 加密失败时抛出异常
     */
    public static String encrypt(String data, String publicKeyBase64) {
        byte[] plain = data.getBytes(StandardCharsets.UTF_8);
        try {
            // 解码公钥
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            int maxLength = modulusByteLength(publicKey) - PKCS1_PADDING_OVERHEAD;
            if (maxLength > 0 && plain.length > maxLength) {
                throw new IllegalArgumentException(
                        "RSA 单块加密最多支持 " + maxLength + " 字节，当前 " + plain.length + " 字节；请改用混合加密（RSA + AES）");
            }

            // 加密数据
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedData = cipher.doFinal(plain);
            
            // 返回Base64编码的加密数据
            return Base64.getEncoder().encodeToString(encryptedData);
            
        } catch (IllegalArgumentException e) {
            throw e;
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
            
            return new String(decryptedData, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            throw new RuntimeException("RSA decryption failed", e);
        }
    }
    
    /**
     * 使用RSA私钥做「原始 RSA 加密」（私钥加密、公钥解密）。
     *
     * <p><b>这不是数字签名</b>：该运算没有哈希、结果确定（同一明文每次得到相同密文），
     * 任何拿到公钥的人都能构造出「合法」结果，无法防篡改或防伪造。
     * 保留它是为了与原始 bbc.me 应用的行为兼容；新代码请使用
     * {@link #sign(String, String)} / {@link #verify(String, String, String)}。</p>
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
            byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            // 返回Base64编码的加密数据
            return Base64.getEncoder().encodeToString(encryptedData);
            
        } catch (Exception e) {
            throw new RuntimeException("RSA encryption with private key failed", e);
        }
    }
    
    /**
     * 使用RSA公钥做「原始 RSA 解密」（对应 {@link #encryptWithPrivateKey}）。
     *
     * <p><b>这不是验签</b>：篡改后的数据会抛异常而不是返回 false，
     * 且它只能解出由对应私钥「原始加密」的数据，不校验任何摘要。
     * 新代码请使用 {@link #verify(String, String, String)}。</p>
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
            
            return new String(decryptedData, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            throw new RuntimeException("RSA decryption with public key failed", e);
        }
    }

    /**
     * 使用RSA私钥对数据签名（SHA256withRSA）。
     *
     * <p>与 {@link #encryptWithPrivateKey} 不同，这是标准签名：先对数据做 SHA-256 摘要，
     * 再对摘要做 PKCS#1 v1.5 签名，具备防篡改与不可伪造性。</p>
     *
     * @param data 待签名数据（UTF-8）
     * @param privateKeyBase64 Base64编码的RSA私钥
     * @return Base64编码的签名
     * @throws RuntimeException 签名失败时抛出异常
     */
    public static String sign(String data, String privateKeyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());

        } catch (Exception e) {
            throw new RuntimeException("RSA sign failed", e);
        }
    }

    /**
     * 使用RSA公钥验证签名（SHA256withRSA）。
     *
     * @param data 原始数据（UTF-8）
     * @param signatureBase64 Base64编码的签名
     * @param publicKeyBase64 Base64编码的RSA公钥
     * @return 验证通过返回 true；签名不匹配、数据被篡改或签名格式非法均返回 false
     * @throws RuntimeException 公钥无法解析时抛出异常
     */
    public static boolean verify(String data, String signatureBase64, String publicKeyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));

            // 签名不匹配、编码非法、长度不对都属于「验证不通过」：
            // 必须返回 false，而不是把异常抛给调用方（签名长度错误会抛 SignatureException）
            try {
                return signature.verify(Base64.getDecoder().decode(signatureBase64));
            } catch (IllegalArgumentException | java.security.SignatureException e) {
                return false;
            }

        } catch (Exception e) {
            throw new RuntimeException("RSA verify failed", e);
        }
    }
}