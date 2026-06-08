package com.yaoguangyun.network;

import com.yaoguangyun.proto.BasicDeviceInfo;
import com.yaoguangyun.proto.CardOperationType;
import com.yaoguangyun.proto.CardProtobufMessage;
import com.yaoguangyun.proto.ProtobufMessage;
import com.yaoguangyun.proto.RequestDataMap;
import com.yaoguangyun.util.RsaEncryptUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;

/**
 * TCP客户端类
 * 基于bbc.me应用的TcpClient实现
 * 对应原始类: bbc.me.TcpClient
 * 
 * 功能特点：
 * 1. 自定义TCP协议实现
 * 2. 支持GZIP压缩
 * 3. 支持Protobuf消息格式
 * 4. 线程池管理
 * 5. DNS解析支持
 * 6. RSA加密响应数据
 * 
 * 协议格式：
 * [魔数: "Epic"][消息类型: 4字节][数据长度: 4字节][压缩数据: 变长]
 */
public class TcpClient {
    
    // ==================== 服务器配置 ====================
    /** 服务器主机地址 */
    private static final String SERVER_HOST = ServerConfig.getServerIp();
    /** 服务器TCP端口 */
    private static final int SERVER_PORT = ServerConfig.getTcpPort();
    /** 服务器HTTP端口 */
    private static final int HTTP_PORT = ServerConfig.getHttpPort();
    
    // ==================== 线程池配置 ====================
    /** 线程池，用于并发处理TCP请求 */
    public static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(16);
    
    // ==================== 超时设置 ====================
    /** 连接超时时间（毫秒） */
    private static final int CONNECT_TIMEOUT = 5000;
    /** Socket超时时间（毫秒） */
    private static final int SO_TIMEOUT = 5000;
    /** 缓冲区大小（字节） */
    private static final int BUFFER_SIZE = 131072;
    
    /**
     * 发送TCP消息
     * 
     * 将消息放入线程池异步执行，通过回调返回结果
     * 
     * @param message 消息内容（字节数组）
     * @param callback 回调接口，用于处理成功或失败情况
     */
    public void sendMessage(byte[] message, TcpResponseCallback callback) {
        THREAD_POOL.execute(() -> {
            try {
                String response = executeTcpRequest(message);
                if (callback != null) {
                    callback.onSuccess(response);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
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
                dataOutputStream.write("Epic".getBytes(StandardCharsets.UTF_8));
                
                // 发送消息类型
                TcpMessageType msgType = TcpMessageType.TCP_MESSAGE_TYPE_VERIFY;
                dataOutputStream.writeInt(msgType.getType());
                
                // 压缩并发送数据
                byte[] compressedData = compressWithGzip(message);
                dataOutputStream.writeInt(compressedData.length);
                dataOutputStream.write(compressedData);
                dataOutputStream.flush();
                
                // 读取响应
                byte[] magicBuffer = new byte["Epic".getBytes(StandardCharsets.UTF_8).length];
                dataInputStream.readFully(magicBuffer);
                
                if (!Arrays.equals("Epic".getBytes(StandardCharsets.UTF_8), magicBuffer)) {
                    throw new IOException("Magic Fail");
                }
                
                if (TcpMessageType.getType(dataInputStream.readInt()) != msgType) {
                    throw new IOException("Message Type Fail");
                }
                
                // 读取响应数据
                int responseLength = dataInputStream.readInt();
                ByteBuffer responseBuffer = ByteBuffer.allocate(responseLength);
                byte[] readBuffer = new byte[1024];
                
                int bytesRead;
                while ((bytesRead = dataInputStream.read(readBuffer)) != -1) {
                    responseBuffer.put(readBuffer, 0, bytesRead);
                }
                
                responseBuffer.flip();
                
                // RSA解密响应数据（模拟原始应用的实现）
                String responseData = new String(responseBuffer.array(), StandardCharsets.UTF_8);
                try {
                    // 尝试RSA解密（如果服务器使用RSA加密响应）
                    String decryptedResponse = RsaEncryptUtils.decryptWithPublicKey(
                        responseData, 
                        ServerConfig.getRsaPublicKey()
                    );
                    return decryptedResponse;
                } catch (Exception e) {
                    // 如果RSA解密失败，直接返回原始数据
                    // 这可能是因为服务器没有加密响应，或者使用了不同的加密方式
                    return responseData;
                }
                
            } finally {
                dataOutputStream.close();
                dataInputStream.close();
            }
        } finally {
            socket.close();
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
     * 解析DNS域名
     * @param hostname 主机名
     * @return IP地址
     */
    private String resolveDns(String hostname) {
        try {
            HttpsDnsClient dnsClient = new HttpsDnsClient(hostname);
            return dnsClient.resolve();
        } catch (Exception e) {
            return hostname;
        }
    }
}