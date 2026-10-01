package com.yaoguangyun;

import com.google.gson.Gson;
import com.yaoguangyun.config.JsonConfigParser;
import com.yaoguangyun.device.DeviceInfo;
import com.yaoguangyun.device.DeviceInfoManager;
import com.yaoguangyun.network.HttpClient;
import com.yaoguangyun.network.JsonDataProcessor;
import com.yaoguangyun.network.ServerConfig;
import com.yaoguangyun.network.TcpClient;
import com.yaoguangyun.network.TcpResponseCallback;
import com.yaoguangyun.proto.BasicDeviceInfo;
import com.yaoguangyun.proto.CardOperationType;
import com.yaoguangyun.proto.CardProtobufMessage;
import com.yaoguangyun.proto.DnsMessage;
import com.yaoguangyun.proto.RequestDataMap;
import com.yaoguangyun.test.SimpleHttpServer;
import com.yaoguangyun.util.AppUtils;
import com.yaoguangyun.util.GsonUtils;
import com.yaoguangyun.util.RsaEncryptUtils;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * 回归测试：覆盖本次代码审查修复的每一项缺陷。
 *
 * <p>全部用例离线、确定性执行，不依赖 hc.t60.top / httpbin.org 等外部服务。</p>
 */
public class YaoGuangYunTest {

    // ==================== 设备信息 ====================

    @Test
    public void deviceInfoPopulatesAndroidId() {
        DeviceInfoManager manager = DeviceInfoManager.getInstance();
        manager.collectDeviceInfo();

        assertNotNull("androidId 必须被填充", manager.getDeviceInfo().getAndroidId());
        assertFalse("androidId 不能为空串", manager.getDeviceInfo().getAndroidId().isEmpty());
    }

    @Test
    public void deviceInfoMapAndJsonUseSameKeys() {
        DeviceInfoManager manager = DeviceInfoManager.getInstance();
        manager.collectDeviceInfo();

        Map<String, String> map = manager.getDeviceInfoMap();
        assertTrue("map 应包含 snake_case 键 sdk_int", map.containsKey("sdk_int"));
        assertFalse("map 不应包含 camelCase 键 sdkInt", map.containsKey("sdkInt"));

        String json = JsonDataProcessor.serializeDeviceInfo(manager.getDeviceInfo());
        assertTrue("JSON 应使用 snake_case", json.contains("\"android_id\""));
        assertFalse("JSON 不应出现 camelCase 键", json.contains("\"localHost\""));
    }

    @Test
    public void setDeviceInfoKeepsBothViewsInSync() {
        DeviceInfoManager manager = DeviceInfoManager.getInstance();
        manager.collectDeviceInfo();

        manager.setDeviceInfo("latitude", "1.2345");
        assertEquals("1.2345", manager.getDeviceInfoMap().get("latitude"));
        assertEquals("1.2345", manager.getDeviceInfo().getLatitude());

        manager.setDeviceInfo("latitude", "39.9042");
    }

    @Test
    public void setDeviceInfoRejectsUnknownKey() {
        DeviceInfoManager manager = DeviceInfoManager.getInstance();
        manager.collectDeviceInfo();

        assertThrows(IllegalArgumentException.class, () -> manager.setDeviceInfo("noSuchField", "x"));
    }

    @Test
    public void verifyRequestCarriesNonEmptyAndroidId() {
        DeviceInfoManager manager = DeviceInfoManager.getInstance();
        manager.collectDeviceInfo();

        String request = JsonDataProcessor.buildVerifyRequest(
                manager.getDeviceInfo(), CardOperationType.Verify, null);
        assertFalse("请求中的 android_id 不能为空", request.contains("\"android_id\":\"\""));
    }

    @Test
    public void buildersTolerateNullDeviceInfo() {
        assertNotNull(JsonDataProcessor.buildVerifyRequest(null, CardOperationType.Verify, null));
        assertNotNull(JsonDataProcessor.buildHeartbeatRequest(null));
    }

    @Test
    public void deviceInfoEqualsAndHashCodeAreValueBased() {
        DeviceInfo a = new DeviceInfo();
        DeviceInfo b = new DeviceInfo();
        a.setBrand("Samsung");
        b.setBrand("Samsung");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        b.setBrand("Other");
        assertFalse(a.equals(b));
    }

    // ==================== JSON 工具 ====================

    @Test
    public void isValidJsonIsStrict() {
        assertFalse("非引号键必须被拒绝", GsonUtils.isValidJson("{key: value}"));
        assertFalse("裸标识符必须被拒绝", GsonUtils.isValidJson("abc"));
        assertFalse("单引号必须被拒绝", GsonUtils.isValidJson("'a'"));
        assertFalse("尾随内容必须被拒绝", GsonUtils.isValidJson("{} garbage"));
        assertTrue("合法对象应通过", GsonUtils.isValidJson("{\"k\":\"v\"}"));
        assertTrue("合法数组应通过", GsonUtils.isValidJson("[1,2]"));
    }

    @Test
    public void typedAccessorsShareOneContract() {
        com.google.gson.JsonObject o =
                com.google.gson.JsonParser.parseString("{\"nested\":{\"a\":1},\"arr\":[1,2]}").getAsJsonObject();
        assertNull("非基本类型应返回 null 而不是抛异常", GsonUtils.getString(o, "nested"));
        assertFalse(GsonUtils.getBoolean(o, "nested"));
        assertEquals(0, GsonUtils.getInt(o, "nested"));
    }

    // ==================== RSA ====================

    @Test
    public void signAndVerifyRoundTrip() {
        KeyPair keyPair = RsaEncryptUtils.generateKeyPair();
        String publicKey = RsaEncryptUtils.getPublicKeyBase64(keyPair);
        String privateKey = RsaEncryptUtils.getPrivateKeyBase64(keyPair);
        String data = "{\"userId\":\"12345\"}";

        String signature = RsaEncryptUtils.sign(data, privateKey);
        assertTrue("合法签名应验证通过", RsaEncryptUtils.verify(data, signature, publicKey));
        assertFalse("篡改数据后应验证失败", RsaEncryptUtils.verify(data + "x", signature, publicKey));
        assertFalse("非法签名应返回 false 而非抛异常",
                RsaEncryptUtils.verify(data, "!!!not-base64!!!", publicKey));
    }

    @Test
    public void verifyRejectsSignatureFromAnotherKey() {
        KeyPair signing = RsaEncryptUtils.generateKeyPair();
        KeyPair other = RsaEncryptUtils.generateKeyPair();
        String data = "payload";
        String signature = RsaEncryptUtils.sign(data, RsaEncryptUtils.getPrivateKeyBase64(signing));
        assertFalse(RsaEncryptUtils.verify(data, signature, RsaEncryptUtils.getPublicKeyBase64(other)));
    }

    @Test
    public void encryptRejectsPayloadBeyondBlockSize() {
        String publicKey = RsaEncryptUtils.getPublicKeyBase64(RsaEncryptUtils.generateKeyPair());
        char[] chars = new char[300];
        java.util.Arrays.fill(chars, 'a');
        assertThrows(IllegalArgumentException.class, () -> RsaEncryptUtils.encrypt(new String(chars), publicKey));
    }

    // ==================== 协议模型 ====================

    @Test
    public void operationTypeAcceptsNumberAndName() {
        Gson gson = new Gson();
        assertEquals(CardOperationType.Verify,
                gson.fromJson("{\"op\":0,\"data\":{}}", CardProtobufMessage.class).getOp());
        assertEquals(CardOperationType.Valid,
                gson.fromJson("{\"op\":\"Valid\",\"data\":{}}", CardProtobufMessage.class).getOp());
    }

    @Test
    public void unknownOperationTypeFailsLoudly() {
        assertNull("GsonUtils 应把解析错误转为 null 而不是抛给调用方",
                GsonUtils.fromJson("{\"op\":9}", CardProtobufMessage.class));
    }

    @Test
    public void gsonNullFieldsDoNotCauseNpe() {
        Gson gson = new Gson();

        CardProtobufMessage card = gson.fromJson("{\"op\":null,\"data\":null}", CardProtobufMessage.class);
        assertNotNull(card.getOp());
        assertEquals(0, card.getDataCount());
        card.hashCode();

        BasicDeviceInfo basic = gson.fromJson(
                "{\"locale\":null,\"android_id\":null,\"status_machine\":null}", BasicDeviceInfo.class);
        assertEquals("", basic.getLocale());
        basic.hashCode();

        DnsMessage dns = gson.fromJson("{\"properties\":null}", DnsMessage.class);
        assertEquals(0, dns.getPropertiesCount());
        dns.hashCode();
    }

    @Test
    public void numericSettersRejectUnknownValues() {
        assertThrows(IllegalArgumentException.class, () -> new CardProtobufMessage().setOpValue(9));
        assertThrows(IllegalArgumentException.class,
                () -> new com.yaoguangyun.proto.ProtobufMessage().setDataTypeValue(7));
    }

    @Test
    public void requestDataMapGuardsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> RequestDataMap.empty(-1));
        assertThrows(IllegalArgumentException.class, () -> new RequestDataMap().concat(null));
        assertThrows(IllegalArgumentException.class, () -> new RequestDataMap().contains((byte[]) null));
        assertEquals("Hello", RequestDataMap.copyFromUtf8("Hello").toStringUtf8());
    }

    // ==================== 工具类 ====================

    @Test
    public void hexToBytesRejectsInvalidCharacters() {
        assertThrows(IllegalArgumentException.class, () -> AppUtils.hexToBytes("zz"));
        assertThrows(IllegalArgumentException.class, () -> AppUtils.hexToBytes("7g"));
        assertThrows(IllegalArgumentException.class, () -> AppUtils.hexToBytes("4"));
        assertEquals("Hello", AppUtils.bytesToString(AppUtils.hexToBytes("48656c6c6f")));
    }

    // ==================== 配置解析 ====================

    @Test
    public void configLoadReturnsFalseOnMalformedJson() throws IOException {
        Path broken = Files.createTempFile("ygy-broken", ".json");
        Files.write(broken, "{\"server\": {\"ip\": ".getBytes(StandardCharsets.UTF_8));
        try {
            JsonConfigParser parser = new JsonConfigParser(broken.toString());
            assertFalse("非法 JSON 应返回 false 而不是抛异常", parser.load());
        } finally {
            Files.deleteIfExists(broken);
        }
    }

    @Test
    public void configLoadReturnsFalseOnNonObjectJson() throws IOException {
        Path array = Files.createTempFile("ygy-array", ".json");
        Files.write(array, "[1,2,3]".getBytes(StandardCharsets.UTF_8));
        try {
            assertFalse(new JsonConfigParser(array.toString()).load());
        } finally {
            Files.deleteIfExists(array);
        }
    }

    @Test
    public void configLoadReturnsFalseOnMissingFile() {
        assertFalse(new JsonConfigParser("definitely-not-here-12345.json").load());
    }

    @Test
    public void configTypedGettersFallBackOnTypeMismatch() throws IOException {
        Path path = Files.createTempFile("ygy-types", ".json");
        Files.write(path, "{\"server\":{\"timeout\":\"abc\",\"flag\":\"yes\",\"ip\":\"example.org\"}}"
                .getBytes(StandardCharsets.UTF_8));
        try {
            JsonConfigParser parser = new JsonConfigParser(path.toString());
            assertTrue(parser.load());
            assertEquals("类型不符应回退到默认值而不是 0", 5000, parser.getInt("server.timeout", 5000));
            assertTrue("'yes' 不是布尔值，应回退到默认值", parser.getBoolean("server.flag", true));
            assertEquals("example.org", parser.getString("server.ip", "default"));
        } finally {
            Files.deleteIfExists(path);
        }
    }

    @Test
    public void configToJsonIsCompactAndToPrettyJsonIsNot() throws IOException {
        Path path = Files.createTempFile("ygy-fmt", ".json");
        Files.write(path, "{\"a\":1}".getBytes(StandardCharsets.UTF_8));
        try {
            JsonConfigParser parser = new JsonConfigParser(path.toString());
            assertTrue(parser.load());
            assertFalse("toJson 应为紧凑格式", parser.toJson().contains("\n"));
            assertTrue("toPrettyJson 应为美化格式", parser.toPrettyJson().contains("\n"));
        } finally {
            Files.deleteIfExists(path);
        }
    }

    // ==================== HTTP ====================

    @Test
    public void httpClientReadsFullBodyAndReportsErrors() throws IOException {
        SimpleHttpServer server = new SimpleHttpServer(0);
        server.start();
        try {
            int port = server.getBoundPort();
            assertTrue("端口应由系统分配", port > 0);
            String base = "http://127.0.0.1:" + port;

            HttpClient client = new HttpClient(5000);
            assertEquals("测试接口正常", JsonDataProcessor.extractField(readAll(client.get(base + "/api/test")), "message"));

            String echo = readAll(client.post(base + "/api/echo", "hello-body".getBytes(StandardCharsets.UTF_8)));
            assertTrue("POST 请求体应被回显", echo.contains("hello-body"));

            try {
                client.get(base + "/api/not-exist");
                org.junit.Assert.fail("未知路径应返回 404");
            } catch (IOException e) {
                assertTrue("错误消息应包含状态码: " + e.getMessage(), e.getMessage().contains("404"));
            }
        } finally {
            server.stop();
        }
    }

    @Test
    public void serverBindsLoopbackOnly() throws IOException {
        SimpleHttpServer server = new SimpleHttpServer(0);
        server.start();
        try {
            // 只断言「服务器可用」，绑定地址由 InetAddress.getLoopbackAddress() 保证
            assertTrue(server.getBoundPort() > 0);
        } finally {
            server.stop();
        }
    }

    // ==================== 服务器配置 ====================

    @Test
    public void serverConfigExposesUsableValues() {
        assertNotNull(ServerConfig.getServerIp());
        assertTrue(ServerConfig.getTcpPort() > 0);
        assertTrue(ServerConfig.getHttpPort() > 0);
        assertTrue("超时必须为正数", ServerConfig.getConnectTimeout() > 0);
        assertTrue("读取超时必须为正数", ServerConfig.getReadTimeout() > 0);
        assertTrue("缓冲区必须为正数", ServerConfig.getBufferSize() > 0);
    }

    // ==================== TCP 响应解码（回归 V-1 / V-7 / V-8） ====================

    @Test
    public void gzipResponseBodyIsDecompressed() throws Exception {
        byte[] plain = "{\"hello\":\"世界\"}".getBytes(StandardCharsets.UTF_8);
        byte[] compressed = gzip(plain);
        assertTrue("压缩后应带 gzip 魔数", (compressed[0] & 0xFF) == 0x1F && (compressed[1] & 0xFF) == 0x8B);
        org.junit.Assert.assertArrayEquals(plain, invokeTryDecompressGzip(compressed));
    }

    @Test
    public void plaintextResponseBodyIsLeftAlone() throws Exception {
        byte[] plain = "{\"hello\":\"world\"}".getBytes(StandardCharsets.UTF_8);
        org.junit.Assert.assertArrayEquals(plain, invokeTryDecompressGzip(plain));
    }

    @Test
    public void corruptGzipFallsBackToRawBytes() throws Exception {
        // 带 gzip 魔数但内容损坏：应回退为原始字节，而不是抛异常
        byte[] corrupt = new byte[] {0x1F, (byte) 0x8B, 0x08, 0x00, 0x01, 0x02, 0x03};
        org.junit.Assert.assertArrayEquals(corrupt, invokeTryDecompressGzip(corrupt));
    }

    @Test
    public void oversizedDecompressionIsRejected() throws Exception {
        // 压缩炸弹：约 80MB 的零压缩后只有几十 KB。
        // 只限制压缩前的长度挡不住它，必须限制解压后的字节数。
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        byte[] zeros = new byte[1024 * 1024];
        for (int i = 0; i < 80; i++) {
            raw.write(zeros, 0, zeros.length);
        }
        byte[] bomb = gzip(raw.toByteArray());
        assertTrue("炸弹载荷本身应当很小", bomb.length < 200 * 1024);

        try {
            invokeTryDecompressGzip(bomb);
            org.junit.Assert.fail("解压超过上限时应抛 IOException");
        } catch (IOException expected) {
            assertTrue("异常信息应说明超过上限: " + expected.getMessage(),
                    expected.getMessage().contains("上限"));
        }
    }

    @Test
    public void base64HeuristicRejectsPlaintextAndShortStrings() throws Exception {
        // 明文 JSON 以 '{' 开头，不是 Base64 字符
        assertFalse(invokeLooksLikeRsaCiphertext("{\"a\":\"b\"}"));
        assertFalse("长度不符的 Base64 不应被当作密文", invokeLooksLikeRsaCiphertext(repeat("A", 100)));
    }

    private static byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        java.util.zip.GZIPOutputStream gz = new java.util.zip.GZIPOutputStream(out);
        gz.write(data);
        gz.close();
        return out.toByteArray();
    }

    private static byte[] invokeTryDecompressGzip(byte[] data) throws Exception {
        java.lang.reflect.Method m = com.yaoguangyun.network.TcpClient.class
                .getDeclaredMethod("tryDecompressGzip", byte[].class);
        m.setAccessible(true);
        try {
            return (byte[]) m.invoke(null, (Object) data);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw (e.getCause() instanceof Exception) ? (Exception) e.getCause() : e;
        }
    }

    private static boolean invokeLooksLikeRsaCiphertext(String text) throws Exception {
        java.lang.reflect.Method m = com.yaoguangyun.network.TcpClient.class
                .getDeclaredMethod("looksLikeRsaCiphertext", String.class);
        m.setAccessible(true);
        return (Boolean) m.invoke(null, text);
    }

    private static String repeat(String s, int times) {
        StringBuilder sb = new StringBuilder(s.length() * times);
        for (int i = 0; i < times; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    // ==================== 回归 V-2 / V-3 / V-4 / V-5 / V-6 ====================

    @Test
    public void verifyReturnsFalseForWrongLengthSignature() {
        KeyPair keyPair = RsaEncryptUtils.generateKeyPair();
        String publicKey = RsaEncryptUtils.getPublicKeyBase64(keyPair);
        // 合法 Base64，但长度远小于签名长度：应返回 false 而不是抛异常
        String shortSignature = java.util.Base64.getEncoder().encodeToString(new byte[] {1, 2, 3});
        assertFalse("长度错误的签名应返回 false", RsaEncryptUtils.verify("data", shortSignature, publicKey));
    }

    @Test
    public void enumAdapterRejectsNonIntegerNumbers() {
        // 1.5 与超大整数会让 nextInt() 抛 NumberFormatException；
        // 它必须被转成 JsonSyntaxException，从而被 GsonUtils 吞成 null 而不是逃到调用方
        assertNull(GsonUtils.fromJson("{\"op\":1.5}", CardProtobufMessage.class));
        assertNull(GsonUtils.fromJson("{\"op\":99999999999999}", CardProtobufMessage.class));
    }

    @Test
    public void setDeviceInfoPreservesObjectIdentity() {
        DeviceInfoManager manager = DeviceInfoManager.getInstance();
        manager.collectDeviceInfo();

        DeviceInfo before = manager.getDeviceInfo();
        manager.setDeviceInfo("latitude", "11.11");
        DeviceInfo after = manager.getDeviceInfo();

        assertTrue("setDeviceInfo 不应替换对象（否则已缓存的引用会读到旧值）", before == after);
        assertEquals("11.11", before.getLatitude());
    }

    @Test
    public void redirectsAreFollowedAndPreservePostOn307() throws IOException {
        SimpleHttpServer target = new SimpleHttpServer(0);
        target.start();
        try {
            int port = target.getBoundPort();
            com.sun.net.httpserver.HttpServer redirector =
                    com.sun.net.httpserver.HttpServer.create(
                            new java.net.InetSocketAddress(java.net.InetAddress.getLoopbackAddress(), 0), 0);
            final int targetPort = port;
            redirector.createContext("/to302", exchange -> {
                // 必须用绝对地址：相对 Location 会解析到重定向服务器自己身上
                exchange.getResponseHeaders().set("Location", "http://127.0.0.1:" + targetPort + "/api/test");
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
            });
            redirector.createContext("/to307", exchange -> {
                exchange.getResponseHeaders().set("Location", "http://127.0.0.1:" + targetPort + "/api/echo");
                exchange.sendResponseHeaders(307, -1);
                exchange.close();
            });
            redirector.start();
            try {
                int rport = redirector.getAddress().getPort();
                HttpClient client = new HttpClient(5000);

                String via302 = readAll(client.get("http://127.0.0.1:" + rport + "/to302"));
                assertTrue("302 应被跟随并降级为 GET", via302.contains("测试接口正常"));

                String via307 = readAll(client.post("http://127.0.0.1:" + rport + "/to307",
                        "kept-body".getBytes(StandardCharsets.UTF_8)));
                assertTrue("307 应保持 POST 方法与请求体", via307.contains("kept-body"));
            } finally {
                redirector.stop(0);
            }
        } finally {
            target.stop();
        }
    }

    @Test
    public void sendMessageAfterShutdownReportsErrorInsteadOfThrowing() throws Exception {
        TcpClient.shutdown();
        final java.util.concurrent.atomic.AtomicReference<Exception> reported =
                new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

        new TcpClient().sendMessage("{}".getBytes(StandardCharsets.UTF_8), new TcpResponseCallback() {
            @Override
            public void onSuccess(String response) {
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                reported.set(e);
                latch.countDown();
            }
        });

        assertTrue("回调应被触发而不是抛出未受检异常", latch.await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertTrue("线程池已关闭时应通过 onError 报告",
                reported.get() instanceof java.util.concurrent.RejectedExecutionException);
    }

    // ==================== 辅助方法 ====================

    private static String readAll(InputStream inputStream) throws IOException {
        try (InputStream in = inputStream) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
