package com.yaoguangyun.test;

import com.yaoguangyun.device.DeviceInfoManager;
import com.yaoguangyun.network.HttpClient;
import com.yaoguangyun.network.ServerConfig;
import com.yaoguangyun.network.TcpClient;
import com.yaoguangyun.network.TcpResponseCallback;
import com.yaoguangyun.util.GsonUtils;
import com.yaoguangyun.util.RsaEncryptUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 网络功能测试类
 *
 * <p><b>结果语义</b>：本地可确定性验证的检查（RSA、设备信息、本地 HTTP 服务器、Gson）
 * 计入失败数并影响退出码；依赖外部服务的检查（真实 TCP 服务器、httpbin.org）
 * 只作为参考信息打印，不计入失败——否则离线环境下会永远“红”，失去信号价值。</p>
 *
 * <p>用法：{@code java ... com.yaoguangyun.test.NetworkTest}，退出码为失败项数量（0 表示全部通过）。</p>
 */
public class NetworkTest {

    /** 失败项计数 */
    private static int failures;
    /** 检查项计数 */
    private static int checks;

    /**
     * 执行全部测试
     *
     * @return 失败项数量（0 表示全部通过）
     */
    public static int run() {
        failures = 0;
        checks = 0;

        System.out.println("=== 网络功能测试 ===\n");

        testRsaEncryption();
        testDeviceInfo();
        testLocalHttpServer();
        testGson();
        testTcpClient();
        testExternalHttp();

        System.out.println("\n=== 测试完成: " + (checks - failures) + "/" + checks + " 通过，失败 " + failures + " 项 ===");
        TcpClient.shutdown();
        return failures;
    }

    public static void main(String[] args) {
        System.exit(run());
    }

    /**
     * 断言辅助方法
     */
    private static void check(String name, boolean condition) {
        checks++;
        if (condition) {
            System.out.println("  ✓ " + name);
        } else {
            failures++;
            System.out.println("  ✗ " + name);
        }
    }

    /**
     * 测试RSA加密与签名
     */
    private static void testRsaEncryption() {
        System.out.println("1. 测试RSA加密与签名");
        System.out.println("--------------------");

        try {
            KeyPair keyPair = RsaEncryptUtils.generateKeyPair();
            String publicKey = RsaEncryptUtils.getPublicKeyBase64(keyPair);
            String privateKey = RsaEncryptUtils.getPrivateKeyBase64(keyPair);

            check("生成RSA密钥对", publicKey != null && !publicKey.isEmpty());

            // 公钥加密 / 私钥解密
            String testData = "Hello, RSA Encryption!";
            String encrypted = RsaEncryptUtils.encrypt(testData, publicKey);
            String decrypted = RsaEncryptUtils.decrypt(encrypted, privateKey);
            check("公钥加密→私钥解密往返一致", testData.equals(decrypted));

            // 真正的数字签名（SHA256withRSA）
            String signature = RsaEncryptUtils.sign(testData, privateKey);
            check("签名验证通过", RsaEncryptUtils.verify(testData, signature, publicKey));
            check("篡改数据后验证失败", !RsaEncryptUtils.verify(testData + "!", signature, publicKey));
            check("非法签名返回 false（不抛异常）", !RsaEncryptUtils.verify(testData, "!!!notbase64!!!", publicKey));

            // 容量上限：2048 位单块最多 245 字节
            try {
                RsaEncryptUtils.encrypt(repeat('a', 300), publicKey);
                check("超出245字节应被拒绝", false);
            } catch (IllegalArgumentException e) {
                check("超出245字节被拒绝", true);
            }

        } catch (Exception e) {
            failures++;
            checks++;
            System.err.println("  ✗ RSA测试异常: " + e);
        }

        System.out.println();
    }

    /**
     * 测试设备信息管理
     */
    private static void testDeviceInfo() {
        System.out.println("2. 测试设备信息管理");
        System.out.println("------------------");

        DeviceInfoManager manager = DeviceInfoManager.getInstance();
        manager.collectDeviceInfo();

        Map<String, String> deviceInfo = manager.getDeviceInfoMap();

        System.out.println("  品牌: " + deviceInfo.get("brand"));
        System.out.println("  型号: " + deviceInfo.get("model"));
        System.out.println("  设备: " + deviceInfo.get("device"));
        System.out.println("  Android版本: " + deviceInfo.get("release"));
        System.out.println("  SDK版本: " + deviceInfo.get("sdk_int"));
        System.out.println("  屏幕分辨率: " + deviceInfo.get("width_pixels") + "x" + deviceInfo.get("height_pixels"));
        System.out.println("  Android ID: " + deviceInfo.get("android_id"));

        check("android_id 已填充（不能为空）", isNotBlank(deviceInfo.get("android_id")));
        check("map 视图键名与 JSON 一致（sdk_int）", deviceInfo.containsKey("sdk_int"));
        check("两个视图同步（setDeviceInfo 生效）", setAndVerify(manager));

        System.out.println();
    }

    private static boolean setAndVerify(DeviceInfoManager manager) {
        manager.setDeviceInfo("latitude", "1.2345");
        boolean synced = "1.2345".equals(manager.getDeviceInfoMap().get("latitude"))
                && "1.2345".equals(manager.getDeviceInfo().getLatitude());
        manager.setDeviceInfo("latitude", "39.9042");
        return synced;
    }

    /**
     * 测试本地HTTP服务器与HttpClient（离线可确定性验证）
     */
    private static void testLocalHttpServer() {
        System.out.println("3. 测试HTTP客户端（本地服务器）");
        System.out.println("------------------------------");

        SimpleHttpServer server = new SimpleHttpServer(0);
        try {
            server.start();
            int port = server.getBoundPort();
            String base = "http://127.0.0.1:" + port;

            HttpClient httpClient = new HttpClient(5000);
            httpClient.addHeader("User-Agent", "YaoGuangYun-Test/1.0");

            String body = readAll(httpClient.get(base + "/api/test"));
            check("GET /api/test 返回200内容", body != null && body.contains("测试接口正常"));

            String echo = readAll(httpClient.post(base + "/api/echo", "hello-body".getBytes(StandardCharsets.UTF_8)));
            check("POST /api/echo 回显请求体", echo != null && echo.contains("hello-body"));

            try {
                httpClient.get(base + "/api/not-exist");
                check("未知路径应返回404", false);
            } catch (IOException e) {
                check("未知路径返回404", e.getMessage() != null && e.getMessage().contains("404"));
            }

            // 大响应必须完整读取（验证不再被单次 read 截断）
            String big = readAll(httpClient.get(base + "/api/device"));
            check("响应完整读取", big != null && big.trim().endsWith("}"));

        } catch (Exception e) {
            failures++;
            checks++;
            System.err.println("  ✗ 本地HTTP测试异常: " + e);
        } finally {
            server.stop();
        }

        System.out.println();
    }

    /**
     * 测试Gson功能
     */
    private static void testGson() {
        System.out.println("4. 测试Gson功能");
        System.out.println("--------------");

        Map<String, Object> testData = new java.util.HashMap<>();
        testData.put("name", "测试数据");
        testData.put("value", 12345);

        String json = GsonUtils.toJson(testData);
        check("JSON序列化", json != null && json.contains("测试数据"));

        Map<String, Object> parsedData = GsonUtils.fromJsonMap(json);
        check("JSON反序列化", parsedData != null && "测试数据".equals(parsedData.get("name")));

        check("严格校验拒绝 {key: value}", !GsonUtils.isValidJson("{key: value}"));
        check("严格校验接受合法JSON", GsonUtils.isValidJson("{\"k\":\"v\"}"));

        com.google.gson.JsonObject jsonObject = GsonUtils.createJsonObject();
        GsonUtils.putString(jsonObject, "key1", "value1");
        GsonUtils.putInt(jsonObject, "key2", 42);
        GsonUtils.putBoolean(jsonObject, "key3", true);
        check("JSON对象操作", GsonUtils.toPrettyJson(jsonObject).contains("key1"));

        System.out.println();
    }

    /**
     * 测试TCP客户端（依赖外部服务器，仅作参考，不计入失败）
     */
    private static void testTcpClient() {
        System.out.println("5. 测试TCP客户端（外部服务器，仅供参考）");
        System.out.println("--------------------------------------");

        System.out.println("  服务器配置: " + ServerConfig.getServerIp()
                + " TCP=" + ServerConfig.getTcpPort()
                + " HTTP=" + ServerConfig.getHttpPort());

        TcpClient tcpClient = new TcpClient();
        String testMessage = "{\"action\":\"test\",\"timestamp\":" + System.currentTimeMillis() + "}";
        System.out.println("  发送: " + testMessage);

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> outcome = new AtomicReference<>("未知");

        tcpClient.sendMessage(testMessage.getBytes(StandardCharsets.UTF_8), new TcpResponseCallback() {
            @Override
            public void onSuccess(String response) {
                outcome.set("成功: " + response);
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                outcome.set("失败: " + describe(e));
                latch.countDown();
            }
        });

        // 用 CountDownLatch 取代固定 sleep：既不会提前返回，也不会无限等待
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                outcome.set("超时（10秒内无回调）");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("  TCP 结果（不影响退出码）: " + outcome.get());
        System.out.println();
    }

    /**
     * 外部HTTP服务探测（仅作参考，不计入失败）
     */
    private static void testExternalHttp() {
        System.out.println("6. 外部HTTP探测（仅供参考）");
        System.out.println("--------------------------");

        String url = "http://httpbin.org/get";
        System.out.println("  GET " + url);
        try {
            HttpClient client = new HttpClient(5000);
            String body = readAll(client.get(url));
            System.out.println("  结果（不影响退出码）: "
                    + (body == null ? "空响应" : (body.length() > 120 ? body.substring(0, 120) + "..." : body)));
        } catch (IOException e) {
            System.out.println("  结果（不影响退出码）: 不可用 - " + describe(e));
        }

        // 端口连通性快检，便于区分「服务不可达」与「协议错误」
        System.out.println("  " + ServerConfig.getServerIp() + ":" + ServerConfig.getTcpPort()
                + " 可达性: " + isReachable(ServerConfig.getServerIp(), ServerConfig.getTcpPort()));
        System.out.println();
    }

    // ==================== 辅助方法 ====================

    /**
     * 完整读取响应流（单次 read 可能只返回部分数据）
     */
    private static String readAll(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return null;
        }
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = inputStream.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            inputStream.close();
        }
    }

    /**
     * 描述异常：getMessage() 为 null 时给出类名与 cause，避免出现「失败: null」
     */
    private static String describe(Throwable e) {
        if (e == null) {
            return "null";
        }
        String message = e.getMessage();
        if (message == null || message.isEmpty()) {
            message = "(无消息)";
        }
        return e.getClass().getSimpleName() + ": " + message;
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    private static boolean isReachable(String host, int port) {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), 3000);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
                // 忽略
            }
        }
    }
}
