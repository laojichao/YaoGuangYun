package com.yaoguangyun.network;

import com.yaoguangyun.util.RsaEncryptUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * TCP客户端类
 * 基于bbc.me应用的TcpClient实现
 * 对应原始类: bbc.me.TcpClient
 *
 * 功能特点：
 * 1. 自定义TCP协议实现
 * 2. 请求体 GZIP 压缩，响应体按同一约定自动解压（明文响应自动回退）
 * 3. 支持Protobuf消息格式
 * 4. 线程池管理（守护线程，不会阻止JVM退出）
 * 5. RSA加密响应数据（仅在载荷确实为密文时尝试）
 *
 * 协议格式：
 * [魔数: "Epic"][消息类型: 4字节][数据长度: 4字节][压缩数据: 变长]
 *
 * 关于 DNS：域名解析由 JDK 默认解析器完成。若需要 DoH（DNS over HTTPS），
 * 请显式使用 {@link HttpsDnsClient} 并把解析结果作为连接目标。
 */
public class TcpClient {

    // ==================== 服务器配置 ====================
    /** 服务器主机地址 */
    private static final String SERVER_HOST = ServerConfig.getServerIp();
    /** 服务器TCP端口 */
    private static final int SERVER_PORT = ServerConfig.getTcpPort();

    // ==================== 线程池配置 ====================
    /** 线程池，用于并发处理TCP请求（守护线程，避免阻塞JVM正常退出） */
    private static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(16, r -> {
        Thread thread = new Thread(r, "yaoguangyun-tcp-pool");
        thread.setDaemon(true);
        return thread;
    });

    // ==================== 超时设置 ====================
    /** 连接超时时间（毫秒） */
    private static final int CONNECT_TIMEOUT = ServerConfig.getConnectTimeout();
    /** Socket超时时间（毫秒） */
    private static final int SO_TIMEOUT = ServerConfig.getReadTimeout();
    /** 缓冲区大小（字节） */
    private static final int BUFFER_SIZE = ServerConfig.getBufferSize();
    /** 响应最大长度（字节），防止异常声明长度导致内存溢出 */
    private static final int MAX_RESPONSE_SIZE = 64 * 1024 * 1024;
    /**
     * 解压后响应最大长度（字节）。
     *
     * <p>gzip 压缩比可达 ~1000:1，只限制压缩前的长度挡不住「压缩炸弹」：
     * 一个 200KB 的载荷就能膨胀到 200MB。必须同时限制解压后的字节数。</p>
     */
    private static final int MAX_DECOMPRESSED_SIZE = 64 * 1024 * 1024;
    /** 协议魔数 */
    private static final byte[] MAGIC = "Epic".getBytes(StandardCharsets.UTF_8);
    /** GZIP 魔数前两字节（0x1F 0x8B） */
    private static final int GZIP_MAGIC_0 = 0x1F;
    private static final int GZIP_MAGIC_1 = 0x8B;
    /**
     * 当前 RSA 公钥对应的密文 Base64 长度（2048 位 → 344 字符）。
     *
     * <p>用精确长度而不是「最小长度」，避免把恰好较长的 Base64 明文误判成密文；
     * 解析失败时为 -1，表示不做任何密文尝试。</p>
     */
    private static final int RSA_CIPHERTEXT_BASE64_LENGTH = computeRsaCiphertextBase64Length();

    /**
     * 发送TCP消息
     *
     * 将消息放入线程池异步执行，通过回调返回结果
     *
     * @param message 消息内容（字节数组）
     * @param callback 回调接口，用于处理成功或失败情况
     */
    public void sendMessage(byte[] message, TcpResponseCallback callback) {
        try {
            THREAD_POOL.execute(() -> {
                String response;
                try {
                    response = executeTcpRequest(message);
                } catch (Exception e) {
                    if (callback != null) {
                        callback.onError(e);
                    }
                    return;
                }
                // 成功回调放在 try 之外：回调自身抛出的异常不应被误判为「请求失败」
                if (callback != null) {
                    callback.onSuccess(response);
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException e) {
            // 线程池已被 shutdown()：以回调形式报告，而不是把未受检异常抛给调用方
            if (callback != null) {
                callback.onError(e);
            }
        }
    }

    /**
     * 关闭内部线程池。
     *
     * <p>线程池中的线程是守护线程，因此不调用此方法也不会阻止 JVM 退出；
     * 但在长时间运行或需要彻底释放资源时（例如测试收尾）应当显式调用。</p>
     */
    public static void shutdown() {
        THREAD_POOL.shutdown();
    }

    /**
     * 执行TCP请求
     * @param message 消息内容
     * @return 响应字符串
     * @throws IOException
     */
    private String executeTcpRequest(byte[] message) throws IOException {
        Socket socket = new Socket();
        try {
            socket.setSoTimeout(SO_TIMEOUT);
            socket.setReceiveBufferSize(BUFFER_SIZE);
            socket.setSendBufferSize(BUFFER_SIZE);
            socket.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT), CONNECT_TIMEOUT);

            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

            try {
                // 发送魔数
                dataOutputStream.write(MAGIC);

                // 发送消息类型
                TcpMessageType msgType = TcpMessageType.TCP_MESSAGE_TYPE_VERIFY;
                dataOutputStream.writeInt(msgType.getType());

                // 压缩并发送数据
                byte[] compressedData = compressWithGzip(message);
                dataOutputStream.writeInt(compressedData.length);
                dataOutputStream.write(compressedData);
                dataOutputStream.flush();

                // 读取响应
                byte[] magicBuffer = new byte[MAGIC.length];
                dataInputStream.readFully(magicBuffer);

                if (!Arrays.equals(MAGIC, magicBuffer)) {
                    throw new IOException("Magic Fail");
                }

                if (TcpMessageType.getType(dataInputStream.readInt()) != msgType) {
                    throw new IOException("Message Type Fail");
                }

                // 读取响应数据（按声明长度精确读取，避免越界或混入未填充数据）
                int responseLength = dataInputStream.readInt();
                if (responseLength < 0 || responseLength > MAX_RESPONSE_SIZE) {
                    throw new IOException("Invalid response length: " + responseLength);
                }
                byte[] responseDataBytes = new byte[responseLength];
                dataInputStream.readFully(responseDataBytes);

                return decodeResponseBody(responseDataBytes);

            } finally {
                dataOutputStream.close();
                dataInputStream.close();
            }
        } finally {
            socket.close();
        }
    }

    /**
     * 解析响应体。
     *
     * <p>帧格式把数据段定义为 gzip 压缩内容，因此这里先按 gzip 解压；
     * 若对端实际返回明文（无 gzip 魔数，或压缩流损坏），则按原始字节处理。</p>
     *
     * <p>随后仅当载荷长度恰好等于当前 RSA 公钥的密文长度（2048 位 → 344 个 Base64 字符）
     * 时才尝试解密。解密失败会打印告警并<b>原样返回载荷</b>——因为无法从协议上确认
     * 该载荷一定是密文，直接判定请求失败会误伤合法的明文响应。</p>
     *
     * @param payload 响应数据段
     * @return 响应字符串
     * @throws IOException 解压后体积超过上限（疑似压缩炸弹）
     */
    private String decodeResponseBody(byte[] payload) throws IOException {
        byte[] body = tryDecompressGzip(payload);
        String text = new String(body, StandardCharsets.UTF_8);

        if (!looksLikeRsaCiphertext(text)) {
            return text;
        }

        try {
            return RsaEncryptUtils.decryptWithPublicKey(text, ServerConfig.getRsaPublicKey());
        } catch (Exception e) {
            System.err.println("[TcpClient] 响应长度与 RSA 密文一致但解密失败，返回原始载荷: " + e);
            return text;
        }
    }

    /**
     * 使用GZIP压缩数据
     * @param data 原始数据
     * @return 压缩后的数据
     * @throws IOException
     */
    private byte[] compressWithGzip(byte[] data) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
        gzipOutputStream.write(data);
        gzipOutputStream.flush();
        gzipOutputStream.close();
        return outputStream.toByteArray();
    }

    /**
     * 尝试 GZIP 解压。
     *
     * <p>数据不带 gzip 魔数、或压缩流损坏（ZipException/EOFException）时原样返回；
     * 但解压后体积超过 {@link #MAX_DECOMPRESSED_SIZE} 时抛 {@link IOException}——
     * 这是明确的攻击/异常信号，不能退化成「把压缩字节当文本返回」。</p>
     *
     * @param data 响应数据段
     * @return 解压后的数据，或原始数据
     * @throws IOException 解压结果超过上限
     */
    private static byte[] tryDecompressGzip(byte[] data) throws IOException {
        if (data == null || data.length < 2
                || (data[0] & 0xFF) != GZIP_MAGIC_0
                || (data[1] & 0xFF) != GZIP_MAGIC_1) {
            // 不是 gzip 流，说明对端返回的是明文
            return data;
        }

        try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(data))) {
            // 初始容量取压缩长度的小值即可：不按倍数预分配，避免在炸弹场景下先把自己撑爆
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(Math.min(data.length, 8192));
            byte[] buffer = new byte[8192];
            int read;
            long total = 0;
            while ((read = gzipInputStream.read(buffer)) != -1) {
                total += read;
                if (total > MAX_DECOMPRESSED_SIZE) {
                    throw new IOException("gzip 解压后大小超过上限 " + MAX_DECOMPRESSED_SIZE
                            + " 字节（压缩比 " + (total / Math.max(1, data.length)) + ":1），已中止");
                }
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toByteArray();
        } catch (java.util.zip.ZipException | java.io.EOFException e) {
            // 魔数命中但压缩流损坏：按原始字节处理
            System.err.println("[TcpClient] 响应 gzip 流损坏，按原始字节处理: " + e.getMessage());
            return data;
        }
    }

    /**
     * 判断字符串长度是否恰好等于当前 RSA 公钥的密文 Base64 长度。
     *
     * <p>只做长度与字符集过滤；无法解析公钥时返回 false（不做密文尝试）。</p>
     *
     * @param text 响应文本
     * @return 是否可能是 RSA 密文
     */
    private static boolean looksLikeRsaCiphertext(String text) {
        if (text == null || RSA_CIPHERTEXT_BASE64_LENGTH <= 0
                || text.length() != RSA_CIPHERTEXT_BASE64_LENGTH) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean base64 = (c >= 'A' && c <= 'Z')
                    || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9')
                    || c == '+'
                    || c == '/'
                    || c == '=';
            if (!base64) {
                return false;
            }
        }
        return true;
    }

    /**
     * 计算当前 RSA 公钥对应的密文 Base64 长度。
     *
     * @return Base64 长度；公钥不可用时返回 -1
     */
    private static int computeRsaCiphertextBase64Length() {
        try {
            byte[] keyBytes = java.util.Base64.getDecoder().decode(ServerConfig.getRsaPublicKey());
            java.security.PublicKey key = java.security.KeyFactory.getInstance("RSA")
                    .generatePublic(new java.security.spec.X509EncodedKeySpec(keyBytes));
            int modulusBytes = (((java.security.interfaces.RSAPublicKey) key).getModulus().bitLength() + 7) / 8;
            // Base64 编码长度（含填充）
            return ((modulusBytes + 2) / 3) * 4;
        } catch (Exception e) {
            return -1;
        }
    }
}
