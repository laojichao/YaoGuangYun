package com.yaoguangyun.test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 简单HTTP服务器
 * 用于本地测试HTTP客户端功能
 *
 * <p>默认只监听 127.0.0.1，避免把未鉴权的测试端点暴露到局域网。
 * 如确实需要被其他主机访问，请使用 {@link #SimpleHttpServer(String, int)} 显式指定绑定地址。</p>
 *
 * <p>除基础端点外，还提供编码/重定向/大响应等端点，供回归测试使用：
 * {@code /api/large}、{@code /api/gzip}、{@code /api/deflate}、{@code /api/identity}、
 * {@code /api/encoding-bogus}、{@code /api/headers}、{@code /api/redirect}。</p>
 */
public class SimpleHttpServer {

    /** 单次请求体的最大读取字节数，防止恶意/异常客户端耗尽内存 */
    private static final int MAX_REQUEST_BODY_SIZE = 1024 * 1024;
    /** 超限请求体最多丢弃的字节数：让客户端先写完，才能稳定收到 413 */
    private static final int MAX_DRAIN_SIZE = 4 * 1024 * 1024;

    /** 大响应端点的固定尾标记，供测试断言响应未被截断 */
    private static final String LARGE_BODY_TAIL = "0123456789abcdef0123456789abcdef";

    private HttpServer server;
    private final int port;
    private final String bindAddress;
    private ExecutorService executor;

    /**
     * 使用回环地址和指定端口构造服务器
     *
     * @param port 监听端口（0 表示由系统分配）
     */
    public SimpleHttpServer(int port) {
        this(null, port);
    }

    /**
     * 使用指定绑定地址和端口构造服务器
     *
     * @param bindAddress 绑定地址，null 或空表示 127.0.0.1
     * @param port        监听端口（0 表示由系统分配）
     */
    public SimpleHttpServer(String bindAddress, int port) {
        this.bindAddress = bindAddress;
        this.port = port;
    }

    /**
     * 启动服务器
     *
     * @throws IllegalStateException 服务器已经启动
     */
    public void start() throws IOException {
        if (server != null) {
            throw new IllegalStateException("服务器已启动");
        }

        InetAddress address = resolveBindAddress();
        server = HttpServer.create(new InetSocketAddress(address, port), 0);

        // 添加处理器
        server.createContext("/", new RootHandler());
        server.createContext("/api/test", new TestHandler());
        server.createContext("/api/echo", new EchoHandler());
        server.createContext("/api/device", new DeviceInfoHandler());
        server.createContext("/api/large", new LargeHandler());
        server.createContext("/api/gzip", new EncodedHandler("gzip"));
        server.createContext("/api/deflate", new EncodedHandler("deflate"));
        server.createContext("/api/identity", new EncodedHandler("identity"));
        server.createContext("/api/encoding-bogus", new EncodedHandler("br"));
        server.createContext("/api/headers", new HeadersHandler());
        server.createContext("/api/redirect", new RedirectHandler());

        // 设置线程池：不设置时所有请求会在分发线程上串行处理，单个卡住的客户端会阻塞全部请求
        executor = Executors.newFixedThreadPool(4, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "yaoguangyun-http-server");
                thread.setDaemon(true);
                return thread;
            }
        });
        server.setExecutor(executor);

        server.start();

        InetSocketAddress bound = server.getAddress();
        System.out.println("HTTP服务器已启动: http://" + bound.getAddress().getHostAddress()
                + ":" + bound.getPort());
    }

    /**
     * 停止服务器（幂等）。
     */
    public void stop() {
        boolean wasRunning = server != null;
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        if (wasRunning) {
            System.out.println("HTTP服务器已停止");
        }
    }

    /**
     * 获取构造时指定的端口。
     *
     * @deprecated 端口为 0 时请使用 {@link #getBoundPort()} 获取实际端口
     */
    @Deprecated
    public int getPort() {
        return port;
    }

    /**
     * 获取实际绑定的地址。
     *
     * @return 绑定地址；服务器未启动时返回 null
     */
    public InetSocketAddress getBoundAddress() {
        return server != null ? server.getAddress() : null;
    }

    /**
     * 获取实际绑定端口。
     *
     * @return 实际绑定端口；服务器未启动时返回 -1
     */
    public int getBoundPort() {
        InetSocketAddress bound = getBoundAddress();
        return bound != null ? bound.getPort() : -1;
    }

    private InetAddress resolveBindAddress() throws IOException {
        if (bindAddress != null && !bindAddress.trim().isEmpty()) {
            return InetAddress.getByName(bindAddress.trim());
        }
        // 明确使用 IPv4 回环：InetAddress.getLoopbackAddress() 在双栈机器上可能是 ::1，
        // 与示例/测试里硬编码的 127.0.0.1 不一致。
        try {
            return InetAddress.getByName("127.0.0.1");
        } catch (IOException e) {
            return InetAddress.getLoopbackAddress();
        }
    }

    /**
     * 根路径处理器：仅 "/" 返回 200，其余未注册路径返回 404
     */
    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!requireMethod(exchange, "GET")) {
                return;
            }
            String path = exchange.getRequestURI().getPath();
            if (path == null || !"/".equals(path)) {
                sendResponse(exchange, 404,
                        "{\"status\":\"error\",\"message\":\"Not Found\",\"path\":\"" + escapeJson(path) + "\"}");
                return;
            }
            String response = "{\"status\":\"ok\",\"message\":\"YaoGuangYun测试服务器运行中\"}";
            sendResponse(exchange, 200, response);
        }
    }

    /**
     * 测试处理器
     */
    static class TestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!requireMethod(exchange, "GET")) {
                return;
            }
            String response = "{\"status\":\"ok\",\"message\":\"测试接口正常\",\"timestamp\":" + System.currentTimeMillis() + "}";
            sendResponse(exchange, 200, response);
        }
    }

    /**
     * 回显处理器
     */
    static class EchoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!requireMethod(exchange, "GET", "POST", "PUT")) {
                return;
            }
            String requestBody;
            try {
                requestBody = readBody(exchange);
            } catch (BodyTooLargeException e) {
                sendResponse(exchange, 413,
                        "{\"status\":\"error\",\"message\":\"请求体超过上限\",\"max\":" + MAX_REQUEST_BODY_SIZE + "}");
                return;
            }

            Map<String, String> responseMap = new LinkedHashMap<>();
            responseMap.put("method", exchange.getRequestMethod());
            responseMap.put("body", requestBody);
            responseMap.put("timestamp", String.valueOf(System.currentTimeMillis()));

            sendResponse(exchange, 200, mapToJson(responseMap));
        }
    }

    /**
     * 设备信息处理器
     */
    static class DeviceInfoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!requireMethod(exchange, "GET")) {
                return;
            }
            String response = "{"
                + "\"brand\":\"Samsung\","
                + "\"model\":\"SM-G950F\","
                + "\"device\":\"dreamlte\","
                + "\"release\":\"8.0.0\","
                + "\"sdk\":\"26\","
                + "\"timestamp\":" + System.currentTimeMillis()
                + "}";
            sendResponse(exchange, 200, response);
        }
    }

    /**
     * 大响应处理器：约 32KB，用于验证客户端完整读取而不截断。
     */
    static class LargeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!requireMethod(exchange, "GET")) {
                return;
            }
            StringBuilder data = new StringBuilder(32 * 1024);
            while (data.length() < 32 * 1024) {
                data.append("0123456789abcdef");
            }
            String response = "{\"status\":\"ok\",\"data\":\"" + data + "\",\"tail\":\""
                    + LARGE_BODY_TAIL + "\"}";
            sendResponse(exchange, 200, response);
        }
    }

    /**
     * 编码响应处理器：gzip/deflate/identity/未知编码。
     */
    static class EncodedHandler implements HttpHandler {
        private final String encoding;

        EncodedHandler(String encoding) {
            this.encoding = encoding;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!requireMethod(exchange, "GET")) {
                return;
            }
            byte[] plain = ("{\"status\":\"ok\",\"encoding\":\"" + encoding + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
            byte[] body = plain;
            if ("gzip".equals(encoding)) {
                body = gzip(plain);
            } else if ("deflate".equals(encoding)) {
                body = deflate(plain);
            }

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Encoding", encoding);
            sendBytes(exchange, 200, body, "application/json; charset=UTF-8", headers);
        }
    }

    /**
     * 请求头回显处理器（用于重定向时的敏感头剥离测试）。
     */
    static class HeadersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!requireMethod(exchange, "GET")) {
                return;
            }
            Map<String, String> responseMap = new LinkedHashMap<>();
            for (Map.Entry<String, java.util.List<String>> entry : exchange.getRequestHeaders().entrySet()) {
                responseMap.put(entry.getKey(), entry.getValue() == null ? "" : String.join(",", entry.getValue()));
            }
            sendResponse(exchange, 200, mapToJson(responseMap));
        }
    }

    /**
     * 重定向处理器。
     *
     * <p>查询参数：{@code code}（301/302/303/304/307/308）、{@code to}（目标 URL，绝对或相对）。
     * 304 不设置 Location。</p>
     */
    static class RedirectHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!requireMethod(exchange, "GET", "POST")) {
                return;
            }
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
            int code = parseInt(query.get("code"), 302);
            if (code == 304) {
                sendBytes(exchange, 304, new byte[0], "application/json; charset=UTF-8", null);
                return;
            }
            String target = query.get("to");
            if (target == null || target.isEmpty()) {
                target = "/api/test";
            }
            Map<String, String> headers = new HashMap<>();
            headers.put("Location", target);
            sendBytes(exchange, code, new byte[0], "application/json; charset=UTF-8", headers);
        }
    }

    /**
     * 读取请求体（Java 8 兼容实现，带长度上限）。
     */
    private static String readBody(HttpExchange exchange) throws IOException {
        InputStream inputStream = exchange.getRequestBody();
        long declaredLength = parseLong(exchange.getRequestHeaders().getFirst("Content-Length"), -1);
        if (declaredLength > MAX_REQUEST_BODY_SIZE) {
            // 先有界丢弃请求体，避免服务端提前关闭连接导致客户端只看到写失败
            drain(inputStream, MAX_DRAIN_SIZE);
            throw new BodyTooLargeException();
        }

        if (inputStream == null) {
            return "";
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int total = 0;
        int read;
        while ((read = inputStream.read(chunk)) != -1) {
            total += read;
            if (total > MAX_REQUEST_BODY_SIZE) {
                throw new BodyTooLargeException();
            }
            buffer.write(chunk, 0, read);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    /**
     * 有界丢弃输入流（用于拒绝超大请求体前让客户端写完）。
     */
    private static void drain(InputStream inputStream, int maxBytes) {
        if (inputStream == null) {
            return;
        }
        byte[] buffer = new byte[8192];
        int total = 0;
        try {
            while (total < maxBytes) {
                int read = inputStream.read(buffer, 0, Math.min(buffer.length, maxBytes - total));
                if (read == -1) {
                    return;
                }
                total += read;
            }
        } catch (IOException ignored) {
            // 客户端提前断开时无需处理
        }
    }

    /**
     * 发送HTTP响应
     */
    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        sendBytes(exchange, statusCode, response.getBytes(StandardCharsets.UTF_8),
                "application/json; charset=UTF-8", null);
    }

    /**
     * 发送字节响应；304/204 不写响应体。
     */
    private static void sendBytes(HttpExchange exchange, int statusCode, byte[] body,
                                  String contentType, Map<String, String> headers) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                exchange.getResponseHeaders().set(header.getKey(), header.getValue());
            }
        }

        if (statusCode == 204 || statusCode == 304) {
            exchange.sendResponseHeaders(statusCode, -1);
            exchange.close();
            return;
        }

        exchange.sendResponseHeaders(statusCode, body.length);
        OutputStream outputStream = exchange.getResponseBody();
        try {
            outputStream.write(body);
            outputStream.flush();
        } finally {
            outputStream.close();
        }
    }

    /**
     * 断言请求方法，不匹配时返回 405。
     */
    private static boolean requireMethod(HttpExchange exchange, String... allowed) throws IOException {
        String method = exchange.getRequestMethod();
        for (String candidate : allowed) {
            if (candidate.equalsIgnoreCase(method)) {
                return true;
            }
        }
        exchange.getResponseHeaders().set("Allow", String.join(", ", allowed));
        sendResponse(exchange, 405,
                "{\"status\":\"error\",\"message\":\"Method Not Allowed\",\"method\":\""
                        + escapeJson(method) + "\"}");
        return false;
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> result = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return result;
        }
        for (String pair : rawQuery.split("&")) {
            int index = pair.indexOf('=');
            String key = index >= 0 ? pair.substring(0, index) : pair;
            String value = index >= 0 ? pair.substring(index + 1) : "";
            try {
                result.put(URLDecoder.decode(key, "UTF-8"), URLDecoder.decode(value, "UTF-8"));
            } catch (IOException e) {
                result.put(key, value);
            }
        }
        return result;
    }

    private static int parseInt(String value, int defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static long parseLong(String value, long defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(out);
        try {
            gzip.write(data);
        } finally {
            gzip.close();
        }
        return out.toByteArray();
    }

    private static byte[] deflate(byte[] data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DeflaterOutputStream deflater = new DeflaterOutputStream(out);
        try {
            deflater.write(data);
        } finally {
            deflater.close();
        }
        return out.toByteArray();
    }

    /**
     * 将Map转换为JSON字符串
     */
    private static String mapToJson(Map<String, String> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":\"")
              .append(escapeJson(entry.getValue())).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 转义JSON字符串中的特殊字符
     */
    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * 请求体超过上限。
     */
    private static class BodyTooLargeException extends IOException {
        BodyTooLargeException() {
            super("请求体超过上限: " + MAX_REQUEST_BODY_SIZE + " 字节");
        }
    }

    /**
     * 测试服务器
     */
    public static void main(String[] args) {
        SimpleHttpServer server = new SimpleHttpServer(8080);
        try {
            server.start();
            System.out.println("按Enter键停止服务器...");
            System.in.read();
            server.stop();
        } catch (IOException e) {
            System.err.println("服务器启动失败: " + e.getMessage());
            System.exit(1);
        }
    }
}
