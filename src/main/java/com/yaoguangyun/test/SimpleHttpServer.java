package com.yaoguangyun.test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 简单HTTP服务器
 * 用于本地测试HTTP客户端功能
 */
public class SimpleHttpServer {
    
    private HttpServer server;
    private int port;
    
    public SimpleHttpServer(int port) {
        this.port = port;
    }
    
    /**
     * 启动服务器
     */
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // 添加处理器
        server.createContext("/", new RootHandler());
        server.createContext("/api/test", new TestHandler());
        server.createContext("/api/echo", new EchoHandler());
        server.createContext("/api/device", new DeviceInfoHandler());
        
        // 启动服务器
        server.start();
        System.out.println("HTTP服务器已启动，端口: " + port);
    }
    
    /**
     * 停止服务器
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("HTTP服务器已停止");
        }
    }
    
    /**
     * 获取服务器端口
     */
    public int getPort() {
        return port;
    }
    
    /**
     * 根路径处理器
     */
    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
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
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            
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
     * 发送HTTP响应
     */
    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);
        
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(response.getBytes(StandardCharsets.UTF_8));
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
            sb.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
            first = false;
        }
        sb.append("}");
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