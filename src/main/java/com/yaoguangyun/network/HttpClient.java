package com.yaoguangyun.network;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * HTTP客户端类
 * 基于bbc.me应用的HttpUrlFetcher实现
 * 对应原始类: bbc.me.HttpUrlFetcher
 *
 * 功能特点：
 * 1. 基于HttpURLConnection的实现
 * 2. 支持自动重定向（最多5次），307/308 保持原请求方法与请求体
 * 3. 自定义请求头；跨源重定向时自动剥离 Authorization/Cookie 等敏感头
 * 4. 超时设置（timeout 必须为正数）
 * 5. 自动解码 gzip/deflate 响应
 * 6. 支持GET和POST请求
 */
public class HttpClient {

    /** 默认超时时间（毫秒），取自 ServerConfig（可由 config.json 的 server.timeout 配置） */
    private static final int DEFAULT_TIMEOUT = ServerConfig.getTimeout();
    /** 最大重定向次数，取自 ServerConfig（可由 config.json 的 network.max_redirects 配置） */
    private static final int MAX_REDIRECTS = ServerConfig.getMaxRedirects();

    /** 跨源重定向时禁止转发的请求头（小写比较） */
    private static final String[] SENSITIVE_HEADERS = {
            "authorization", "cookie", "cookie2", "proxy-authorization"
    };

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
     *
     * @param timeout 超时时间（毫秒），必须为正数
     * @throws IllegalArgumentException timeout 小于等于 0（0 在 HttpURLConnection 中表示无限等待）
     */
    public HttpClient(int timeout) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("timeout 必须为正数（毫秒），实际为 " + timeout);
        }
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
        if (key == null || value == null) {
            throw new IllegalArgumentException("请求头 key/value 不能为 null");
        }
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
        return executeRequest(url, "GET", null, new HashMap<>(headers), 0);
    }

    /**
     * 执行POST请求
     * @param url 请求URL
     * @param data 请求数据
     * @return 响应输入流（调用方负责关闭以释放连接）
     * @throws IOException
     */
    public InputStream post(String url, byte[] data) throws IOException {
        return executeRequest(url, "POST", data, new HashMap<>(headers), 0);
    }

    /**
     * 执行请求
     *
     * @param url 请求URL
     * @param method HTTP方法
     * @param body 请求体，可为 null
     * @param requestHeaders 本次请求链路使用的请求头快照
     * @param redirectCount 重定向计数
     * @return 响应输入流
     * @throws IOException
     */
    private InputStream executeRequest(String url, String method, byte[] body,
                                       Map<String, String> requestHeaders, int redirectCount) throws IOException {
        HttpURLConnection connection = createConnection(url, method, requestHeaders);
        try {
            if ("POST".equals(method)) {
                // 使用try-with-resources确保请求体流关闭并被刷出
                connection.setDoOutput(true);
                IOException writeFailure = null;
                try (OutputStream outputStream = connection.getOutputStream()) {
                    if (body != null && body.length > 0) {
                        outputStream.write(body);
                    }
                } catch (IOException e) {
                    // 服务端可能已经提前返回（例如 413 拒绝超大请求体），
                    // 此时写失败不代表拿不到状态码；先尝试读取响应。
                    writeFailure = e;
                }

                if (writeFailure != null) {
                    try {
                        int status = connection.getResponseCode();
                        if (status >= 400) {
                            try {
                                return handleResponse(connection, url, method, body, requestHeaders, redirectCount);
                            } catch (IOException responseFailure) {
                                responseFailure.addSuppressed(writeFailure);
                                throw responseFailure;
                            }
                        }
                    } catch (IOException responseFailure) {
                        writeFailure.addSuppressed(responseFailure);
                        throw writeFailure;
                    }
                    throw writeFailure;
                }
            } else {
                connection.connect();
            }

            // 成功时直接返回响应流（连接由调用方关闭流时释放）；
            // 任何抛出路径都会由下面的 catch 统一断开连接，避免连接泄漏。
            return handleResponse(connection, url, method, body, requestHeaders, redirectCount);
        } catch (IOException | RuntimeException e) {
            connection.disconnect();
            throw e;
        }
    }

    /**
     * 创建连接。
     *
     * <p>先校验协议并确认底层连接确实是 HttpURLConnection：直接强转 ftp:/file:
     * 会抛 ClassCastException，绕过本方法声明的 throws IOException 契约。</p>
     */
    private HttpURLConnection createConnection(String url, String method,
                                               Map<String, String> requestHeaders) throws IOException {
        URL target = new URL(url);
        validateProtocol(target);

        URLConnection rawConnection = target.openConnection();
        if (!(rawConnection instanceof HttpURLConnection)) {
            throw new IOException("不是 HTTP 连接: " + url);
        }
        HttpURLConnection connection = (HttpURLConnection) rawConnection;

        try {
            // 设置超时
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);

            // 设置缓存
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod(method);

            // 添加自定义请求头
            if (requestHeaders != null) {
                for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
                    connection.addRequestProperty(header.getKey(), header.getValue());
                }
            }
            return connection;
        } catch (IOException | RuntimeException e) {
            connection.disconnect();
            throw e;
        }
    }

    /**
     * 处理响应
     *
     * @param connection 连接
     * @param url 当前URL
     * @param method 当前请求方法
     * @param body 当前请求体
     * @param requestHeaders 当前请求头快照
     * @param redirectCount 重定向计数
     * @return 响应输入流
     * @throws IOException
     */
    private InputStream handleResponse(HttpURLConnection connection, String url, String method, byte[] body,
                                       Map<String, String> requestHeaders, int redirectCount) throws IOException {
        int responseCode = connection.getResponseCode();

        if (responseCode / 100 == 2) {
            // 成功响应：先按 Content-Encoding 解码，再决定是否需要长度包装
            InputStream rawStream = connection.getInputStream();
            String contentEncoding = connection.getContentEncoding();
            if (contentEncoding == null || contentEncoding.trim().isEmpty()) {
                int contentLength = connection.getContentLength();
                if (contentLength > 0) {
                    return new ContentLengthInputStream(rawStream, contentLength);
                }
                return rawStream;
            }
            // 压缩响应的 Content-Length 是压缩后长度，不能套在解压流上
            return decodeContentEncoding(rawStream, contentEncoding);
        }

        if (responseCode == 304) {
            // 条件请求未修改：不是错误，返回空流；调用方继续使用本地缓存
            connection.disconnect();
            return new ByteArrayInputStream(new byte[0]);
        }

        if (isRedirect(responseCode)) {
            // 重定向
            String location = connection.getHeaderField("Location");
            if (location == null || location.isEmpty()) {
                connection.disconnect();
                throw new IOException("Received empty redirect url");
            }

            URL redirectUrl = new URL(new URL(url), location);
            validateProtocol(redirectUrl);

            // 上限判断放在「确实要跟随重定向」的分支里：
            // 否则 max_redirects = 0（即“不跟随重定向”）会在首次请求前就抛错，把客户端整个禁用。
            if (redirectCount + 1 > MAX_REDIRECTS) {
                connection.disconnect();
                throw new IOException("Too many redirects (max " + MAX_REDIRECTS + ")");
            }

            Map<String, String> nextHeaders = headersForRedirect(new URL(url), redirectUrl, requestHeaders);
            connection.disconnect();

            // 307/308 要求保持原请求方法和请求体；301/302/303 按惯例降级为 GET
            // 注意：HttpURLConnection.HTTP_TEMP_REDIRECT 是 Java 9+ 常量，本项目兼容 Java 8，故使用字面量
            if (responseCode == 307 || responseCode == 308) {
                return executeRequest(redirectUrl.toString(), method, body, nextHeaders, redirectCount + 1);
            }
            return executeRequest(redirectUrl.toString(), "GET", null, nextHeaders, redirectCount + 1);
        }

        // 错误响应，主动断开连接释放资源
        connection.disconnect();
        throw new IOException("HTTP error: " + responseCode);
    }

    /**
     * 解码 Content-Encoding。多个编码按声明顺序的逆序应用。
     */
    private static InputStream decodeContentEncoding(InputStream rawStream, String contentEncoding) throws IOException {
        String[] codings = contentEncoding.split(",");
        InputStream decoded = rawStream;
        for (int i = codings.length - 1; i >= 0; i--) {
            String coding = codings[i].trim().toLowerCase(Locale.ROOT);
            if (coding.isEmpty() || "identity".equals(coding)) {
                continue;
            }
            if ("gzip".equals(coding) || "x-gzip".equals(coding)) {
                decoded = new GZIPInputStream(decoded);
            } else if ("deflate".equals(coding)) {
                decoded = new InflaterInputStream(decoded);
            } else {
                throw new IOException("不支持的 Content-Encoding: " + coding);
            }
        }
        return decoded;
    }

    /**
     * 仅 301/302/303/307/308 视为需要跟随的重定向；304 单独处理，300/305/306 不跟随。
     */
    private static boolean isRedirect(int responseCode) {
        return responseCode == 301 || responseCode == 302 || responseCode == 303
                || responseCode == 307 || responseCode == 308;
    }

    /**
     * 校验 URL 协议，只允许 http/https。
     */
    private static void validateProtocol(URL url) throws IOException {
        String protocol = url.getProtocol() == null ? "" : url.getProtocol().toLowerCase(Locale.ROOT);
        if (!"http".equals(protocol) && !"https".equals(protocol)) {
            throw new IOException("不支持的协议: " + protocol + "（仅支持 http/https）");
        }
    }

    /**
     * 计算下一跳的请求头：同源保留全部；跨源移除敏感头。
     *
     * <p>返回新 Map，绝不修改实例级 headers。</p>
     */
    private static Map<String, String> headersForRedirect(URL from, URL to, Map<String, String> requestHeaders) {
        Map<String, String> next = new HashMap<>();
        if (requestHeaders == null) {
            return next;
        }
        boolean sameOrigin = isSameOrigin(from, to);
        for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
            if (!sameOrigin && isSensitiveHeader(header.getKey())) {
                continue;
            }
            next.put(header.getKey(), header.getValue());
        }
        return next;
    }

    private static boolean isSensitiveHeader(String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        for (String sensitive : SENSITIVE_HEADERS) {
            if (sensitive.equals(lower)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSameOrigin(URL from, URL to) {
        if (!equalsIgnoreCase(from.getProtocol(), to.getProtocol())) {
            return false;
        }
        if (!equalsIgnoreCase(from.getHost(), to.getHost())) {
            return false;
        }
        return effectivePort(from) == effectivePort(to);
    }

    private static boolean equalsIgnoreCase(String left, String right) {
        return left == null ? right == null : left.equalsIgnoreCase(right);
    }

    private static int effectivePort(URL url) {
        if (url.getPort() != -1) {
            return url.getPort();
        }
        return "https".equalsIgnoreCase(url.getProtocol()) ? 443 : 80;
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
