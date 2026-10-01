package com.yaoguangyun.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP客户端类
 * 基于bbc.me应用的HttpUrlFetcher实现
 * 对应原始类: bbc.me.HttpUrlFetcher
 *
 * 功能特点：
 * 1. 基于HttpURLConnection的实现
 * 2. 支持自动重定向（最多5次），307/308 保持原请求方法与请求体
 * 3. 自定义请求头
 * 4. 超时设置
 * 5. 内容长度处理
 * 6. 支持GET和POST请求
 */
public class HttpClient {

    /** 默认超时时间（毫秒），取自 ServerConfig（可由 config.json 的 server.timeout 配置） */
    private static final int DEFAULT_TIMEOUT = ServerConfig.getTimeout();
    /** 最大重定向次数，取自 ServerConfig（可由 config.json 的 network.max_redirects 配置） */
    private static final int MAX_REDIRECTS = ServerConfig.getMaxRedirects();

    /** 超时时间 */
    private final int timeout;
    /** 请求头映射（并发安全，允许实例被多线程共享） */
    private final Map<String, String> headers;

    /**
     * 默认构造函数，使用默认超时时间
     */
    public HttpClient() {
        this(DEFAULT_TIMEOUT);
    }

    /**
     * 带超时参数的构造函数
     * @param timeout 超时时间（毫秒）
     */
    public HttpClient(int timeout) {
        this.timeout = timeout;
        this.headers = new ConcurrentHashMap<>();
    }

    /**
     * 添加请求头
     *
     * 使用链式调用模式，便于连续添加多个请求头
     *
     * @param key 请求头键
     * @param value 请求头值
     * @return HttpClient实例（支持链式调用）
     */
    public HttpClient addHeader(String key, String value) {
        headers.put(key, value);
        return this;
    }

    /**
     * 执行GET请求
     *
     * @param url 请求URL
     * @return 响应输入流（调用方负责关闭以释放连接）
     * @throws IOException 网络异常
     */
    public InputStream get(String url) throws IOException {
        return executeRequest(url, "GET", null, 0);
    }

    /**
     * 执行POST请求
     * @param url 请求URL
     * @param data 请求数据
     * @return 响应输入流（调用方负责关闭以释放连接）
     * @throws IOException
     */
    public InputStream post(String url, byte[] data) throws IOException {
        return executeRequest(url, "POST", data, 0);
    }

    /**
     * 执行请求
     *
     * @param url 请求URL
     * @param method HTTP方法
     * @param body 请求体，可为 null
     * @param redirectCount 重定向计数
     * @return 响应输入流
     * @throws IOException
     */
    private InputStream executeRequest(String url, String method, byte[] body, int redirectCount) throws IOException {
        HttpURLConnection connection = createConnection(url, method);
        try {
            if ("POST".equals(method)) {
                // 使用try-with-resources确保请求体流关闭并被刷出
                connection.setDoOutput(true);
                try (OutputStream outputStream = connection.getOutputStream()) {
                    if (body != null && body.length > 0) {
                        outputStream.write(body);
                    }
                }
            } else {
                connection.connect();
            }

            // 成功时直接返回响应流（连接由调用方关闭流时释放）；
            // 任何抛出路径都会由下面的 catch 统一断开连接，避免连接泄漏。
            return handleResponse(connection, url, method, body, redirectCount);
        } catch (IOException e) {
            connection.disconnect();
            throw e;
        }
    }

    /**
     * 创建连接
     * @param url 请求URL
     * @param method HTTP方法
     * @return HttpURLConnection实例
     * @throws IOException
     */
    private HttpURLConnection createConnection(String url, String method) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

        // 设置超时
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);

        // 设置缓存
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod(method);

        // 添加自定义请求头
        for (Map.Entry<String, String> header : headers.entrySet()) {
            connection.addRequestProperty(header.getKey(), header.getValue());
        }

        return connection;
    }

    /**
     * 处理响应
     * @param connection 连接
     * @param url 当前URL
     * @param method 当前请求方法
     * @param body 当前请求体
     * @param redirectCount 重定向计数
     * @return 响应输入流
     * @throws IOException
     */
    private InputStream handleResponse(HttpURLConnection connection, String url, String method, byte[] body,
                                       int redirectCount) throws IOException {
        int responseCode = connection.getResponseCode();
        int statusGroup = responseCode / 100;

        if (statusGroup == 2) {
            // 成功响应
            String contentEncoding = connection.getContentEncoding();
            if (contentEncoding != null && !contentEncoding.isEmpty()) {
                return connection.getInputStream();
            } else {
                int contentLength = connection.getContentLength();
                if (contentLength > 0) {
                    return new ContentLengthInputStream(connection.getInputStream(), contentLength);
                } else {
                    return connection.getInputStream();
                }
            }
        } else if (statusGroup == 3) {
            // 重定向
            String location = connection.getHeaderField("Location");
            if (location == null || location.isEmpty()) {
                throw new IOException("Received empty redirect url");
            }

            URL redirectUrl = new URL(new URL(url), location);
            connection.disconnect();

            // 上限判断放在「确实要跟随重定向」的分支里：
            // 否则 max_redirects = 0（即“不跟随重定向”）会在首次请求前就抛错，把客户端整个禁用。
            if (redirectCount + 1 > MAX_REDIRECTS) {
                throw new IOException("Too many redirects (max " + MAX_REDIRECTS + ")");
            }

            // 307/308 要求保持原请求方法和请求体；301/302/303 按惯例降级为 GET
            // 注意：HttpURLConnection.HTTP_TEMP_REDIRECT 是 Java 9+ 常量，本项目兼容 Java 8，故使用字面量
            if (responseCode == 307 || responseCode == 308) {
                return executeRequest(redirectUrl.toString(), method, body, redirectCount + 1);
            }
            return executeRequest(redirectUrl.toString(), "GET", null, redirectCount + 1);
        } else {
            // 错误响应，主动断开连接释放资源
            connection.disconnect();
            throw new IOException("HTTP error: " + responseCode);
        }
    }

    /**
     * 内容长度输入流
     */
    private static class ContentLengthInputStream extends InputStream {
        private final InputStream wrapped;
        private int remaining;

        public ContentLengthInputStream(InputStream wrapped, int contentLength) {
            this.wrapped = wrapped;
            this.remaining = contentLength;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int b = wrapped.read();
            if (b != -1) {
                remaining--;
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int toRead = Math.min(len, remaining);
            int bytesRead = wrapped.read(b, off, toRead);
            if (bytesRead != -1) {
                remaining -= bytesRead;
            }
            return bytesRead;
        }

        @Override
        public int available() throws IOException {
            return Math.min(wrapped.available(), remaining);
        }

        @Override
        public void close() throws IOException {
            wrapped.close();
        }
    }
}
