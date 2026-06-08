package com.yaoguangyun.network;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;

/**
 * HTTPS DNS客户端
 * 基于bbc.me应用的HttpsDnsClient实现
 * 对应原始类: bbc.me.HttpsDnsClient
 * 
 * 功能特点：
 * 1. 通过HTTPS协议进行DNS解析
 * 2. 支持DNS over HTTPS (DoH)
 * 3. 自定义DNS服务器支持
 * 4. 支持A记录解析
 * 
 * DNS over HTTPS (DoH) 是一种安全的DNS解析方式
 * 通过HTTPS协议发送DNS查询，避免DNS劫持
 */
public class HttpsDnsClient {
    
    /** 连接超时时间（毫秒） */
    private static final int CONNECT_TIMEOUT = 3000;
    /** 读取超时时间（毫秒） */
    private static final int READ_TIMEOUT = 5000;
    /** DNS服务器URL（Google DNS over HTTPS） */
    private static final String DNS_SERVER_URL = "https://dns.google/dns-query";
    /** 内容类型 */
    private static final String CONTENT_TYPE = "application/dns-message";
    
    /** 要解析的主机名 */
    private final String hostname;
    
    /**
     * 构造函数
     * @param hostname 要解析的主机名
     */
    public HttpsDnsClient(String hostname) {
        this.hostname = hostname;
    }
    
    /**
     * 解析域名
     * 
     * 通过HTTPS协议向DNS服务器发送查询请求
     * 解析域名对应的IP地址
     * 
     * @return IP地址字符串
     * @throws IOException 网络异常
     */
    public String resolve() throws IOException {
        // 构建DNS查询请求
        byte[] dnsQuery = buildDnsQuery(hostname);
        
        // 发送HTTPS请求
        HttpsURLConnection connection = (HttpsURLConnection) new URL(DNS_SERVER_URL).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", CONTENT_TYPE);
        connection.setRequestProperty("Accept", CONTENT_TYPE);
        connection.setRequestProperty("Accept-Encoding", "");
        
        try {
            // 发送请求
            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.write(dnsQuery);
            outputStream.close();
            
            // 检查响应
            if (connection.getResponseCode() != 200) {
                return null;
            }
            
            int contentLength = connection.getContentLength();
            if (contentLength <= 0 || contentLength > 1048576) {
                return null;
            }
            
            // 读取响应
            InputStream inputStream = connection.getInputStream();
            byte[] response = new byte[contentLength];
            int bytesRead = inputStream.read(response);
            inputStream.close();
            
            if (bytesRead <= 0) {
                return null;
            }
            
            // 解析响应
            return parseDnsResponse(response);
            
        } finally {
            connection.disconnect();
        }
    }
    
    /**
     * 构建DNS查询请求
     * @param hostname 主机名
     * @return DNS查询字节数组
     */
    private byte[] buildDnsQuery(String hostname) {
        // 简化的DNS查询构建
        // 实际实现需要完整的DNS协议支持
        ByteBuffer buffer = ByteBuffer.allocate(512);
        
        // DNS头部
        buffer.putShort((short) 0x1234); // Transaction ID
        buffer.putShort((short) 0x0100); // Flags: Standard query
        buffer.putShort((short) 1);      // Questions: 1
        buffer.putShort((short) 0);      // Answer RRs: 0
        buffer.putShort((short) 0);      // Authority RRs: 0
        buffer.putShort((short) 0);      // Additional RRs: 0
        
        // 查询域名
        String[] parts = hostname.split("\\.");
        for (String part : parts) {
            buffer.put((byte) part.length());
            buffer.put(part.getBytes(StandardCharsets.US_ASCII));
        }
        buffer.put((byte) 0); // 域名结束
        
        // 查询类型和类
        buffer.putShort((short) 1);  // Type: A
        buffer.putShort((short) 1);  // Class: IN
        
        buffer.flip();
        byte[] result = new byte[buffer.remaining()];
        buffer.get(result);
        return result;
    }
    
    /**
     * 解析DNS响应
     * @param response 响应字节数组
     * @return IP地址
     */
    private String parseDnsResponse(byte[] response) {
        ByteBuffer buffer = ByteBuffer.wrap(response);
        
        // 跳过DNS头部
        buffer.getShort(); // Transaction ID
        buffer.getShort(); // Flags
        int questions = buffer.getShort() & 0xFFFF;
        int answers = buffer.getShort() & 0xFFFF;
        buffer.getShort(); // Authority RRs
        buffer.getShort(); // Additional RRs
        
        // 跳过查询部分
        for (int i = 0; i < questions; i++) {
            skipDnsName(buffer);
            buffer.getShort(); // Type
            buffer.getShort(); // Class
        }
        
        // 解析回答部分
        for (int i = 0; i < answers; i++) {
            skipDnsName(buffer);
            int type = buffer.getShort() & 0xFFFF;
            buffer.getShort(); // Class
            buffer.getInt();   // TTL
            int dataLength = buffer.getShort() & 0xFFFF;
            
            if (type == 1 && dataLength == 4) { // A记录
                int ip = buffer.getInt();
                return String.format("%d.%d.%d.%d",
                    (ip >> 24) & 0xFF,
                    (ip >> 16) & 0xFF,
                    (ip >> 8) & 0xFF,
                    ip & 0xFF);
            } else {
                // 跳过其他记录
                for (int j = 0; j < dataLength; j++) {
                    buffer.get();
                }
            }
        }
        
        return null;
    }
    
    /**
     * 跳过DNS名称字段
     * @param buffer 缓冲区
     */
    private void skipDnsName(ByteBuffer buffer) {
        int length;
        while ((length = buffer.get() & 0xFF) != 0) {
            if ((length & 0xC0) == 0xC0) {
                // 压缩指针
                buffer.get();
                return;
            } else {
                for (int i = 0; i < length; i++) {
                    buffer.get();
                }
            }
        }
    }
}