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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * 简单HTTP服务器
 * 用于本地测试HTTP客户端功能
 *
 * <p>默认只监听回环地址（127.0.0.1 / ::1），避免把未鉴权的测试端点暴露到局域网。
 * 如确实需要被其他主机访问，请使用 {@link #SimpleHttpServer(String, int)} 显式指定绑定地址。</p>
 */
public class SimpleHttpServer {

    /** 单次请求体的最大读取字节数，防止恶意/异常客户端耗尽内存 */
    private static final int MAX_REQUEST_BODY_SIZE = 1024 * 1024;

    private HttpServer server;
    private final int port;
    private final String bindAddress;
    private ExecutorService executor;

    /**
     * 使用回环地址和指定端口构造服务器
     *
     * @param port 监听端口
     */
    public SimpleHttpServer(int port) {
        this(null, port);
    }

    /**
     * 使用指定绑定地址和端口构造服务器
     *
     * @param bindAddress 绑定地址，null 或空表示仅回环地址
     * @param port        监听端口
     */
    public SimpleHttpServer(String bindAddress, int port) {
        this.bindAddress = bindAddress;
        this.port = port;
    }

    /**
     * 启动服务器
     */
    public void start() throws IOException {
        InetAddress address = (bindAddress == null || bindAddress.isEmpty())
                ? InetAddress.getLoopbackAddress()
                : InetAddress.getByName(bindAddress);

        server = HttpServer.create(new InetSocketAddress(address, port), 0);

        // 添加处理器
        server.createContext("/", new RootHandler());
        server.createContext("/api/test", new TestHandler());
        server.createContext("/api/echo", new EchoHandler());
        server.createContext("/api/device", new DeviceInfoHandler());

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

        // 启动服务器
        server.start();
        System.out.println("HTTP服务器已启动: http://" + address.getHostAddress() + ":" + port);
    }

    /**
     * 停止服务器
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        System.out.println("HTTP服务器已停止");
    }

    /**
     * 获取服务器端口
     */
    public int getPort() {
        return port;
    }

    /**
     * 获取实际绑定端口。
     *
     * <p>构造时传入 0 表示由系统分配空闲端口，此时 {@link #getPort()} 仍返回 0，
     * 需要本方法读取真实端口。</p>
     *
     * @return 实际绑定端口；服务器未启动时返回 -1
     */
    public int getBoundPort() {
        return server != null ? server.getAddress().getPort() : -1;
    }

    /**
     * 根路径处理器：仅 "/" 返回 200，其余未注册路径返回 404
     */
    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
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
            String method = exchange.getRequestMethod();
            String requestBody = readBody(exchange.getRequestBody());

            Map<String, String> responseMap = new HashMap<>();
            responseMap.put("method", method);
            responseMap.put("body", requestBody);
            responseMap.put("timestamp", String.valueOf(System.currentTimeMillis()));

            String response = mapToJson(responseMap);
            sendResponse(exchange, 200, response);
        }
    }

    /**
     * 设备信息处理器
     */
    static class DeviceInfoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
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
     * 读取请求体（Java 8 兼容实现，带长度上限）
     *
     * <p>注意：{@code InputStream.readAllBytes()} 是 Java 9+ API，本项目声明兼容 Java 8，
     * 因此这里必须使用手写循环。</p>
     */
    private static String readBody(InputStream inputStream) throws IOException {
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
                throw new IOException("请求体超过上限: " + MAX_REQUEST_BODY_SIZE + " 字节");
            }
            buffer.write(chunk, 0, read);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    /**
     * 发送HTTP响应
     */
    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] body = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, body.length);

        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(body);
        outputStream.flush();
        outputStream.close();
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
                case '\"':
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
        }
    }
}
