package com.yaoguangyun.network;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
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
 * 5. 每次查询使用随机事务ID并校验响应，拒绝不匹配的应答
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
    /** 响应体最大长度（字节） */
    private static final int MAX_RESPONSE_SIZE = 1048576;
    /** DNS 名称总长度上限（RFC 1035：253 个字符） */
    private static final int MAX_HOSTNAME_LENGTH = 253;
    /** DNS 单标签长度上限 */
    private static final int MAX_LABEL_LENGTH = 63;
    /** DNS 报文头部长度 */
    private static final int DNS_HEADER_SIZE = 12;

    /** 随机数源，用于生成 DNS 事务 ID */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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
     * @return IP地址字符串；服务器正常应答但没有 A 记录时返回 null
     * @throws IOException 网络异常，或响应报文格式非法
     */
    public String resolve() throws IOException {
        // 构建DNS查询请求
        int transactionId = SECURE_RANDOM.nextInt(0x10000);
        byte[] dnsQuery = buildDnsQuery(hostname, transactionId);

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

            byte[] response = readResponseBody(connection);
            if (response == null || response.length < DNS_HEADER_SIZE) {
                return null;
            }

            // 解析响应
            return parseDnsResponse(response, transactionId);

        } finally {
            connection.disconnect();
        }
    }

    /**
     * 读取响应体。
     *
     * <p>不依赖 Content-Length：DoH 服务端可能使用 chunked 编码（此时
     * {@code getContentLength()} 返回 -1），原先的实现会把这种合法响应直接丢弃。</p>
     *
     * @param connection 已连接的对象
     * @return 响应字节；为空时返回 null
     * @throws IOException 读取失败或响应过大
     */
    private byte[] readResponseBody(HttpsURLConnection connection) throws IOException {
        int contentLength = connection.getContentLength();
        if (contentLength > MAX_RESPONSE_SIZE) {
            return null;
        }

        InputStream inputStream = connection.getInputStream();
        try {
            if (contentLength > 0) {
                // 已知长度：精确读取
                byte[] response = new byte[contentLength];
                int offset = 0;
                while (offset < contentLength) {
                    int bytesRead = inputStream.read(response, offset, contentLength - offset);
                    if (bytesRead == -1) {
                        break;
                    }
                    offset += bytesRead;
                }
                if (offset <= 0) {
                    return null;
                }
                return offset == contentLength ? response : java.util.Arrays.copyOf(response, offset);
            }

            // 长度未知（chunked）：读到 EOF，带上限保护
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = inputStream.read(chunk)) != -1) {
                if (buffer.size() + read > MAX_RESPONSE_SIZE) {
                    throw new IOException("DNS响应超过上限: " + MAX_RESPONSE_SIZE + " 字节");
                }
                buffer.write(chunk, 0, read);
            }
            return buffer.size() == 0 ? null : buffer.toByteArray();
        } finally {
            inputStream.close();
        }
    }

    /**
     * 构建DNS查询请求
     * @param hostname 主机名
     * @param transactionId 事务ID
     * @return DNS查询字节数组
     */
    private byte[] buildDnsQuery(String hostname, int transactionId) {
        if (hostname == null || hostname.isEmpty()) {
            throw new IllegalArgumentException("hostname 不能为空");
        }
        if (hostname.length() > MAX_HOSTNAME_LENGTH) {
            throw new IllegalArgumentException("域名过长（上限 " + MAX_HOSTNAME_LENGTH + " 字符）: " + hostname);
        }

        // 按实际需要分配缓冲区，避免固定 512 字节在超长域名下溢出
        byte[] nameBytes = encodeDnsName(hostname);
        ByteBuffer buffer = ByteBuffer.allocate(DNS_HEADER_SIZE + nameBytes.length + 4);

        // DNS头部
        buffer.putShort((short) (transactionId & 0xFFFF)); // Transaction ID
        buffer.putShort((short) 0x0100); // Flags: Standard query
        buffer.putShort((short) 1);      // Questions: 1
        buffer.putShort((short) 0);      // Answer RRs: 0
        buffer.putShort((short) 0);      // Authority RRs: 0
        buffer.putShort((short) 0);      // Additional RRs: 0

        // 查询域名
        buffer.put(nameBytes);

        // 查询类型和类
        buffer.putShort((short) 1);  // Type: A
        buffer.putShort((short) 1);  // Class: IN

        return buffer.array();
    }

    /**
     * 将域名编码为 DNS 标签序列
     * @param hostname 主机名
     * @return 编码后的字节
     */
    private byte[] encodeDnsName(String hostname) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(hostname.length() + 2);
        String[] parts = hostname.split("\\.");
        for (String part : parts) {
            // DNS标签长度限制为1~63字节，防止长度字节溢出
            if (part.isEmpty() || part.length() > MAX_LABEL_LENGTH) {
                throw new IllegalArgumentException("Invalid DNS label: " + part);
            }
            byte[] labelBytes = part.getBytes(StandardCharsets.US_ASCII);
            out.write(labelBytes.length);
            out.write(labelBytes, 0, labelBytes.length);
        }
        out.write(0); // 域名结束
        return out.toByteArray();
    }

    /**
     * 解析DNS响应
     *
     * @param response 响应字节数组
     * @param expectedTransactionId 期望的事务ID
     * @return IP地址；无 A 记录时返回 null
     * @throws IOException 报文被截断或格式非法
     */
    private String parseDnsResponse(byte[] response, int expectedTransactionId) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(response);

        try {
            // 头部读取也必须纳入边界保护：否则长度不足时抛出的 BufferUnderflowException
            // 是未受检异常，会违反本方法声明的 throws IOException 契约
            requireRemaining(buffer, DNS_HEADER_SIZE, "header");
            int transactionId = buffer.getShort() & 0xFFFF;
            if (transactionId != (expectedTransactionId & 0xFFFF)) {
                throw new IOException("DNS事务ID不匹配: 期望 " + (expectedTransactionId & 0xFFFF)
                        + "，实际 " + transactionId);
            }
            buffer.getShort(); // Flags
            int questions = buffer.getShort() & 0xFFFF;
            int answers = buffer.getShort() & 0xFFFF;
            buffer.getShort(); // Authority RRs
            buffer.getShort(); // Additional RRs

            // 跳过查询部分
            for (int i = 0; i < questions; i++) {
                skipDnsName(buffer);
                requireRemaining(buffer, 4, "question");
                buffer.getShort(); // Type
                buffer.getShort(); // Class
            }

            // 解析回答部分
            for (int i = 0; i < answers; i++) {
                skipDnsName(buffer);
                requireRemaining(buffer, 10, "answer header");
                int type = buffer.getShort() & 0xFFFF;
                buffer.getShort(); // Class
                buffer.getInt();   // TTL
                int dataLength = buffer.getShort() & 0xFFFF;
                requireRemaining(buffer, dataLength, "answer data");

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
        } catch (java.nio.BufferUnderflowException e) {
            throw new IOException("DNS响应报文不完整", e);
        }

        return null;
    }

    /**
     * 断言缓冲区剩余可读字节数足够
     */
    private static void requireRemaining(ByteBuffer buffer, int needed, String section) throws IOException {
        if (buffer.remaining() < needed) {
            throw new IOException("DNS响应报文不完整（" + section + " 需要 " + needed
                    + " 字节，剩余 " + buffer.remaining() + "）");
        }
    }

    /**
     * 跳过DNS名称字段
     * @param buffer 缓冲区
     */
    private void skipDnsName(ByteBuffer buffer) throws IOException {
        int length;
        while (true) {
            requireRemaining(buffer, 1, "dns name");
            length = buffer.get() & 0xFF;
            if (length == 0) {
                return;
            }
            if ((length & 0xC0) == 0xC0) {
                // 压缩指针
                requireRemaining(buffer, 1, "compression pointer");
                buffer.get();
                return;
            }
            requireRemaining(buffer, length, "dns label");
            for (int i = 0; i < length; i++) {
                buffer.get();
            }
        }
    }
}
