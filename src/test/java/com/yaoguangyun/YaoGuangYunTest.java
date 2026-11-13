package com.yaoguangyun;

import com.google.gson.Gson;
import com.yaoguangyun.config.JsonConfigParser;
import com.yaoguangyun.device.DeviceInfo;
import com.yaoguangyun.device.DeviceInfoManager;
import com.yaoguangyun.network.HttpClient;
import com.yaoguangyun.network.HttpsDnsClient;
import com.yaoguangyun.network.JsonDataProcessor;
import com.yaoguangyun.network.ServerConfig;
import com.yaoguangyun.network.TcpClient;
import com.yaoguangyun.network.TcpResponseCallback;
import com.yaoguangyun.proto.BasicDeviceInfo;
import com.yaoguangyun.proto.CardOperationType;
import com.yaoguangyun.proto.CardProtobufMessage;
import com.yaoguangyun.proto.CardType;
import com.yaoguangyun.proto.DnsMessage;
import com.yaoguangyun.proto.ProtobufMessage;
import com.yaoguangyun.proto.RequestDataMap;
import com.yaoguangyun.test.SimpleHttpServer;
import com.yaoguangyun.util.AppUtils;
import com.yaoguangyun.util.GsonUtils;
import com.yaoguangyun.util.HutoolUtils;
import com.yaoguangyun.util.RsaEncryptUtils;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.IDN;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
        assertTrue("android_id 应等于模拟值: " + request,
                request.contains("\"android_id\":\"9774d56d682e549c\""));
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
            assertTrue("端口应由系统分配", server.getBoundPort() > 0);
            assertNotNull("应能读到实际绑定地址", server.getBoundAddress());
            assertTrue("服务器必须只绑定回环地址，实际: " + server.getBoundAddress(),
                    server.getBoundAddress().getAddress().isLoopbackAddress());
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
        // 直接流式写入 GZIPOutputStream，避免测试自身先分配 200MB 堆。
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        java.util.zip.GZIPOutputStream gz = new java.util.zip.GZIPOutputStream(compressed);
        byte[] zeros = new byte[8 * 1024];
        for (int i = 0; i < (80 * 1024) / 8; i++) {
            gz.write(zeros, 0, zeros.length);
        }
        gz.close();
        byte[] bomb = compressed.toByteArray();
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
        // 使用注入的本地线程池，避免关闭全局共享池影响其他测试（JUnit 方法顺序不保证）
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newSingleThreadExecutor();
        final java.util.concurrent.atomic.AtomicReference<Exception> reported =
                new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

        try {
            TcpClient client = new TcpClient(pool);
            pool.shutdown();
            client.sendMessage("{}".getBytes(StandardCharsets.UTF_8), new TcpResponseCallback() {
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
        } finally {
            pool.shutdownNow();
        }
    }

    // ==================== 第三轮排查回归（R-1 / R-2 / R-6 / R-8） ====================

    @Test
    public void nullBrandDoesNotProduceLiteralNullInStatusMachine() {
        // 品牌与型号均为 null 时，status_machine 不能是 "null(null)"
        String request = JsonDataProcessor.buildVerifyRequest(new DeviceInfo(), CardOperationType.Verify, null);
        assertFalse("status_machine 不能含字面量 null", request.contains("null(null)"));
        assertFalse("status_machine 不能是 \"null\"", request.contains(":\"null\""));
    }

    @Test
    public void partiallyNullDeviceFieldsProduceSanitizedStatusMachine() {
        // 仅型号存在：应输出 "()" 空品牌 + 型号，而不是 "null(SM-G950F)"
        DeviceInfo partial = new DeviceInfo();
        partial.setModel("SM-G950F");
        String request = JsonDataProcessor.buildVerifyRequest(partial, CardOperationType.Verify, null);
        assertTrue("应包含 (SM-G950F)，实际: " + request,
                request.contains("(SM-G950F)"));
    }

    @Test
    public void nullDeviceFieldsAreOmittedFromDataMap() {
        // 全 null 设备信息：data 映射不应出现任何值为 null 的键（即 "brand":null）
        String request = JsonDataProcessor.buildVerifyRequest(new DeviceInfo(), CardOperationType.Verify, null);
        assertFalse("data 映射不应包含 null 值字段", request.contains("brand\":null"));
        assertFalse(request.contains("model\":null"));
        assertFalse(request.contains("sdk_int\":null"));
    }

    @Test
    public void nullDataMapValuesAreSkipped() {
        // 调用方传入的 data map 含 null 值：应跳过而不是写出 JSON null
        Map<String, String> data = new java.util.HashMap<>();
        data.put("keep", "v");
        data.put("drop", null);
        String request = JsonDataProcessor.buildVerifyRequest(null, CardOperationType.Verify, data);
        assertTrue(request.contains("\"keep\":\"v\""));
        assertFalse("null 值应被跳过", request.contains("drop"));
    }

    @Test
    public void setAndGetDeviceInfoAcceptCamelCaseAlias() {
        DeviceInfoManager manager = DeviceInfoManager.getInstance();
        manager.collectDeviceInfo();

        manager.setDeviceInfo("androidId", "probe-alias");
        assertEquals("Java 字段名应被 setDeviceInfo 接受",
                "probe-alias", manager.getDeviceInfo().getAndroidId());
        assertEquals("getDeviceInfo 也应按 Java 字段名取值",
                "probe-alias", manager.getDeviceInfo("androidId"));

        // 恢复模拟值，避免影响其他用例
        manager.setDeviceInfo("androidId", "9774d56d682e549c");
    }

    @Test
    public void extractFieldReturnsNullForNonPrimitiveWithoutStderrNoise() {
        assertNull("对象字段应返回 null", JsonDataProcessor.extractField("{\"a\":{\"b\":1}}", "a"));
        assertNull("数组字段应返回 null", JsonDataProcessor.extractField("{\"a\":[1]}", "a"));
        assertEquals("基本类型仍正常提取", "123",
                JsonDataProcessor.extractField("{\"a\":123}", "a"));
        assertNull("缺失键返回 null", JsonDataProcessor.extractField("{}", "nope"));
    }

    // ==================== 第四轮回归（HTTP / 协议 / 加密 / 配置 / DNS） ====================

    @Test
    public void httpClientDecodesCompressedBodies() throws IOException {
        SimpleHttpServer server = new SimpleHttpServer(0);
        server.start();
        try {
            String base = baseUrl(server);
            HttpClient client = new HttpClient(5000);

            assertTrue("gzip 应被自动解压",
                    readAll(client.get(base + "/api/gzip")).contains("\"encoding\":\"gzip\""));
            assertTrue("deflate 应被自动解压",
                    readAll(client.get(base + "/api/deflate")).contains("\"encoding\":\"deflate\""));
            assertTrue("identity 应原样返回",
                    readAll(client.get(base + "/api/identity")).contains("\"encoding\":\"identity\""));
        } finally {
            server.stop();
        }
    }

    @Test
    public void httpClientRejectsUnknownContentEncoding() throws IOException {
        SimpleHttpServer server = new SimpleHttpServer(0);
        server.start();
        try {
            HttpClient client = new HttpClient(5000);
            try {
                client.get(baseUrl(server) + "/api/encoding-bogus");
                org.junit.Assert.fail("未知 Content-Encoding 应抛 IOException");
            } catch (IOException e) {
                assertTrue("异常信息应说明编码不支持: " + e.getMessage(),
                        e.getMessage().contains("Content-Encoding"));
            }
        } finally {
            server.stop();
        }
    }

    @Test
    public void httpClientRejectsNonHttpRedirect() throws IOException {
        SimpleHttpServer server = new SimpleHttpServer(0);
        server.start();
        try {
            HttpClient client = new HttpClient(5000);
            String target = URLEncoder.encode("ftp://example.com/x", "UTF-8");
            try {
                client.get(baseUrl(server) + "/api/redirect?code=302&to=" + target);
                org.junit.Assert.fail("非 http(s) 重定向应抛 IOException");
            } catch (IOException e) {
                assertTrue("应是协议错误而不是 ClassCastException: " + e.getMessage(),
                        e.getMessage().contains("协议"));
            }
        } finally {
            server.stop();
        }
    }

    @Test
    public void httpClientReturnsEmptyBodyFor304() throws IOException {
        SimpleHttpServer server = new SimpleHttpServer(0);
        server.start();
        try {
            HttpClient client = new HttpClient(5000);
            assertEquals("304 应返回空流", "",
                    readAll(client.get(baseUrl(server) + "/api/redirect?code=304")));
        } finally {
            server.stop();
        }
    }

    @Test
    public void httpClientPreservesMethodOn308AndDowngradesOn303() throws IOException {
        SimpleHttpServer server = new SimpleHttpServer(0);
        server.start();
        try {
            String base = baseUrl(server);
            HttpClient client = new HttpClient(5000);

            String toEcho = URLEncoder.encode(base + "/api/echo", "UTF-8");
            String via308 = readAll(client.post(base + "/api/redirect?code=308&to=" + toEcho,
                    "keep308".getBytes(StandardCharsets.UTF_8)));
            assertTrue("308 应保留 POST 与请求体: " + via308, via308.contains("keep308"));

            String via303 = readAll(client.post(base + "/api/redirect?code=303&to=" + toEcho,
                    "drop303".getBytes(StandardCharsets.UTF_8)));
            assertTrue("303 应降级为 GET: " + via303, via303.contains("\"method\":\"GET\""));
            assertFalse("303 不应携带原请求体: " + via303, via303.contains("drop303"));
        } finally {
            server.stop();
        }
    }

    @Test
    public void httpClientStripsSensitiveHeadersAcrossOrigins() throws IOException {
        SimpleHttpServer headerServer = new SimpleHttpServer(0);
        SimpleHttpServer redirectServer = new SimpleHttpServer(0);
        headerServer.start();
        redirectServer.start();
        try {
            String crossTarget = URLEncoder.encode(baseUrl(headerServer) + "/api/headers", "UTF-8");
            HttpClient crossClient = new HttpClient(5000);
            crossClient.addHeader("Authorization", "Bearer secret");
            String crossBody = readAll(crossClient.get(
                    baseUrl(redirectServer) + "/api/redirect?code=302&to=" + crossTarget));
            assertFalse("跨源重定向不能转发 Authorization: " + crossBody,
                    crossBody.toLowerCase().contains("secret"));

            String sameTarget = URLEncoder.encode("/api/headers", "UTF-8");
            HttpClient sameClient = new HttpClient(5000);
            sameClient.addHeader("Authorization", "Bearer secret");
            String sameBody = readAll(sameClient.get(
                    baseUrl(redirectServer) + "/api/redirect?code=302&to=" + sameTarget));
            assertTrue("同源重定向应保留 Authorization: " + sameBody,
                    sameBody.contains("secret"));
        } finally {
            redirectServer.stop();
            headerServer.stop();
        }
    }

    @Test
    public void httpClientReadsLargeResponseFully() throws IOException {
        SimpleHttpServer server = new SimpleHttpServer(0);
        server.start();
        try {
            String body = readAll(new HttpClient(5000).get(baseUrl(server) + "/api/large"));
            assertTrue("大响应不能被截断，实际长度 " + body.length(), body.length() > 32 * 1024);
            assertTrue("尾部标记应完整: " + body.substring(Math.max(0, body.length() - 80)),
                    body.endsWith("\"tail\":\"0123456789abcdef0123456789abcdef\"}"));
        } finally {
            server.stop();
        }
    }

    @Test
    public void httpClientRejectsNonPositiveTimeout() {
        assertThrows(IllegalArgumentException.class, () -> new HttpClient(0));
        assertThrows(IllegalArgumentException.class, () -> new HttpClient(-1));
    }

    @Test
    public void httpClientReportsMethodAndBodyLimitErrors() throws IOException {
        SimpleHttpServer server = new SimpleHttpServer(0);
        server.start();
        try {
            String base = baseUrl(server);
            HttpClient client = new HttpClient(5000);

            try {
                client.post(base + "/api/test", "x".getBytes(StandardCharsets.UTF_8));
                org.junit.Assert.fail("错误方法应返回 405");
            } catch (IOException e) {
                assertTrue("应包含 405: " + e.getMessage(), e.getMessage().contains("405"));
            }

            byte[] oversized = new byte[2 * 1024 * 1024];
            try {
                client.post(base + "/api/echo", oversized);
                org.junit.Assert.fail("超大请求体应返回 413");
            } catch (IOException e) {
                assertTrue("应包含 413: " + e.getMessage(), e.getMessage().contains("413"));
            }
        } finally {
            server.stop();
        }
    }

    @Test
    public void enumSentinelIsRejectedEverywhere() {
        assertThrows(IllegalArgumentException.class, () -> GsonUtils.toJson(CardOperationType.UNRECOGNIZED));
        assertThrows(IllegalArgumentException.class, () -> GsonUtils.toJson(CardType.UNRECOGNIZED));
        assertNull("字符串 UNRECOGNIZED 应被 GsonUtils 吞成 null",
                GsonUtils.fromJson("{\"op\":\"UNRECOGNIZED\"}", CardProtobufMessage.class));
        assertThrows(IllegalArgumentException.class,
                () -> new CardProtobufMessage().setOp(CardOperationType.valueOf(99)));
        assertThrows(IllegalArgumentException.class,
                () -> new ProtobufMessage().setDataType(CardType.valueOf(7)));
    }

    @Test
    public void protobufMessageInfersOneofBranch() {
        ProtobufMessage parsed = GsonUtils.fromJson("{\"card\":{\"op\":\"Verify\"}}", ProtobufMessage.class);
        assertNotNull(parsed);
        assertNotNull("反序列化后 card 分支应可达", parsed.getCard());
        assertEquals(CardType.Card, parsed.getDataType());

        ProtobufMessage message = new ProtobufMessage();
        message.setSoft(new DnsMessage());
        message.setCard(new CardProtobufMessage());
        assertFalse("setCard 应清空 soft 分支", message.hasSoft());
        assertTrue(message.hasCard());

        String json = GsonUtils.toJson(message);
        assertTrue("应包含 card: " + json, json.contains("\"card\""));
        assertFalse("不应同时包含 soft: " + json, json.contains("\"soft\""));
    }

    @Test
    public void mapContractsTreatNullAsAbsent() {
        CardProtobufMessage card = GsonUtils.fromJson(
                "{\"data\":{\"a\":null,\"b\":\"v\"}}", CardProtobufMessage.class);
        assertNotNull(card);
        assertEquals(1, card.getDataCount());
        assertFalse(card.containsData("a"));
        assertEquals("d", card.getDataOrDefault("a", "d"));
        assertEquals(1, card.getDataMap().size());
        assertThrows(IllegalArgumentException.class, () -> card.putData(null, "v"));
        assertThrows(IllegalArgumentException.class, () -> card.putData("k", null));

        DnsMessage dns = GsonUtils.fromJson(
                "{\"properties\":{\"a\":null,\"b\":\"v\"}}", DnsMessage.class);
        assertNotNull(dns);
        assertEquals(1, dns.getPropertiesCount());
        assertFalse(dns.containsProperty("a"));
        assertEquals("d", dns.getPropertyOrDefault("a", "d"));
        assertThrows(IllegalArgumentException.class, () -> dns.setProperty("k", null));
    }

    @Test
    public void verifyReturnsFalseForNullDataOrSignature() {
        KeyPair keyPair = RsaEncryptUtils.generateKeyPair();
        String publicKey = RsaEncryptUtils.getPublicKeyBase64(keyPair);
        String signature = RsaEncryptUtils.sign("data", RsaEncryptUtils.getPrivateKeyBase64(keyPair));

        assertFalse("data 为 null 应返回 false", RsaEncryptUtils.verify(null, signature, publicKey));
        assertFalse("签名为 null 应返回 false", RsaEncryptUtils.verify("data", null, publicKey));
        assertFalse("签名为空串应返回 false", RsaEncryptUtils.verify("data", "", publicKey));
    }

    @Test
    public void rsaKeyMaterialAcceptsPemAndWhitespace() {
        KeyPair keyPair = RsaEncryptUtils.generateKeyPair();
        String publicKey = RsaEncryptUtils.getPublicKeyBase64(keyPair);
        String pem = "-----BEGIN PUBLIC KEY-----\n"
                + wrapBase64(publicKey, 64)
                + "\n-----END PUBLIC KEY-----\n";

        String encrypted = RsaEncryptUtils.encrypt("hello-pem", pem);
        assertEquals("hello-pem",
                RsaEncryptUtils.decrypt(encrypted, RsaEncryptUtils.getPrivateKeyBase64(keyPair)));
        assertThrows(IllegalArgumentException.class, () -> RsaEncryptUtils.decodeKeyMaterial("  \n "));
    }

    @Test
    public void encryptWithPrivateKeyRejectsOversizedPayload() {
        KeyPair keyPair = RsaEncryptUtils.generateKeyPair();
        String privateKey = RsaEncryptUtils.getPrivateKeyBase64(keyPair);
        assertThrows(IllegalArgumentException.class,
                () -> RsaEncryptUtils.encryptWithPrivateKey(repeat("a", 300), privateKey));
    }

    @Test
    public void isValidJsonRejectsWhitespaceOnly() {
        assertFalse(GsonUtils.isValidJson("   "));
        assertFalse(GsonUtils.isValidJson("\n\t"));
    }

    @Test
    public void jsonNumericGettersAreExact() {
        com.google.gson.JsonObject object = com.google.gson.JsonParser
                .parseString("{\"big\":4294967297,\"fraction\":1.9,\"inf\":1e400}")
                .getAsJsonObject();
        assertEquals("溢出应返回 0", 0, GsonUtils.getInt(object, "big"));
        assertEquals("小数应返回 0", 0, GsonUtils.getInt(object, "fraction"));
        assertEquals("Infinity 应返回 0", 0.0, GsonUtils.getDouble(object, "inf"), 0.0);
    }

    @Test
    public void configNumericGettersAreExact() throws IOException {
        Path path = Files.createTempFile("ygy-numbers", ".json");
        Files.write(path, "{\"big\":4294967297,\"fraction\":1.9,\"inf\":1e400}"
                .getBytes(StandardCharsets.UTF_8));
        try {
            JsonConfigParser parser = new JsonConfigParser(path.toString());
            assertTrue(parser.load());
            assertEquals(7, parser.getInt("big", 7));
            assertEquals(7, parser.getInt("fraction", 7));
            assertEquals(7.0, parser.getDouble("inf", 7.0), 0.0);
        } finally {
            Files.deleteIfExists(path);
        }
    }

    @Test
    public void configKeysAndMergeContracts() {
        JsonConfigParser parser = new JsonConfigParser("unused.json");
        assertTrue(parser.fromJson("{\"a\":1}"));
        assertThrows(IllegalArgumentException.class, () -> parser.setString(null, "v"));
        assertThrows(IllegalArgumentException.class, () -> parser.setString("", "v"));
        assertThrows(IllegalArgumentException.class, () -> parser.setString("a..b", "v"));
        assertThrows(IllegalStateException.class, () -> parser.setInt("a.b", 2));
        assertThrows(UnsupportedOperationException.class, () -> parser.getKeys().clear());

        JsonConfigParser base = new JsonConfigParser("base.json");
        assertTrue(base.fromJson("{\"server\":{\"ip\":\"a\",\"port\":1}}"));
        JsonConfigParser other = new JsonConfigParser("other.json");
        assertTrue(other.fromJson("{\"server\":{\"port\":2}}"));
        base.merge(other);
        assertEquals("深合并应保留未覆盖的键", "a", base.getString("server.ip", ""));
        assertEquals(2, base.getInt("server.port", 0));
    }

    @Test
    public void createDefaultConfigRejectsNull() throws IOException {
        Path path = Files.createTempFile("ygy-default", ".json");
        try {
            assertFalse(JsonConfigParser.createDefaultConfig(path.toString(), null));
        } finally {
            Files.deleteIfExists(path);
        }
    }

    @Test
    public void configLoadsFromExplicitPathInChildJvm() throws Exception {
        Path config = writeProbeConfig();
        try {
            String output = runConfigProbe(config, null);
            assertTrue("server.ip 应来自配置文件: " + output, output.contains("ip=config-probe.invalid"));
            assertTrue("tcp_port 应来自配置文件: " + output, output.contains("tcp=5101"));
            assertTrue("http_port 应来自配置文件: " + output, output.contains("http=5102"));
            assertTrue("timeout 应来自配置文件: " + output, output.contains("timeout=4321"));
            assertTrue("cache.enabled 应来自配置文件: " + output, output.contains("cache=false"));
        } finally {
            Files.deleteIfExists(config);
        }
    }

    @Test
    public void configEnvironmentOverridesAreValidated() throws Exception {
        Path config = writeProbeConfig();
        try {
            Map<String, String> env = new java.util.HashMap<>();
            env.put("YAOGUANGYUN_TCP_PORT", "70000");
            env.put("YAOGUANGYUN_SERVER_IP", "   ");
            String output = runConfigProbe(config, env);
            assertTrue("非法端口应回退到配置文件值: " + output, output.contains("tcp=5101"));
            assertTrue("空白 IP 应回退到配置文件值: " + output, output.contains("ip=config-probe.invalid"));
        } finally {
            Files.deleteIfExists(config);
        }
    }

    @Test
    public void dnsParserHandlesAnswersAndErrors() throws Exception {
        byte[] valid = dnsResponse(0x1234, 0x8180, "1.2.3.4");
        assertEquals("1.2.3.4", invokeParseDnsResponse(valid, 0x1234));

        assertThrows(IOException.class, () -> invokeParseDnsResponse(new byte[] {0, 1}, 0x1234));
        assertThrows(IOException.class, () -> invokeParseDnsResponse(valid, 0x1235));
        assertThrows(IOException.class,
                () -> invokeParseDnsResponse(dnsResponse(0x1234, 0x8380, "1.2.3.4"), 0x1234));
        assertThrows(IOException.class,
                () -> invokeParseDnsResponse(dnsResponse(0x1234, 0x8182, "1.2.3.4"), 0x1234));
        assertThrows(java.net.UnknownHostException.class,
                () -> invokeParseDnsResponse(dnsResponse(0x1234, 0x8183, "1.2.3.4"), 0x1234));
    }

    @Test
    public void dnsClientConvertsIdnAndRequiresHttpsServer() throws Exception {
        HttpsDnsClient client = new HttpsDnsClient("例子.测试");
        Field field = HttpsDnsClient.class.getDeclaredField("hostname");
        field.setAccessible(true);
        assertEquals(IDN.toASCII("例子.测试"), field.get(client));

        assertThrows(IllegalArgumentException.class,
                () -> new HttpsDnsClient("example.com", "http://dns.example/dns-query"));
    }

    @Test
    public void rsaCiphertextHeuristicAcceptsExactLength() throws Exception {
        assertTrue("344 字符应被识别为候选密文", invokeLooksLikeRsaCiphertext(repeat("A", 344)));
        assertFalse("343 字符不应被识别", invokeLooksLikeRsaCiphertext(repeat("A", 343)));
        assertFalse("345 字符不应被识别", invokeLooksLikeRsaCiphertext(repeat("A", 345)));
    }

    @Test
    public void hutoolUtilsJsonContractMatchesGsonUtils() {
        assertNull(HutoolUtils.fromJson("{bad", Map.class));
        assertNull(HutoolUtils.toMap("{bad"));
        // Hutool 的 formatJsonStr 本身是宽松的，不保证失败；这里只要求它不抛异常
        assertNotNull(HutoolUtils.formatJson("{bad"));
    }

    // ==================== 辅助方法 ====================

    private static String baseUrl(SimpleHttpServer server) {
        return "http://" + server.getBoundAddress().getAddress().getHostAddress()
                + ":" + server.getBoundPort();
    }

    private static String wrapBase64(String value, int lineLength) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i += lineLength) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(value, i, Math.min(value.length(), i + lineLength));
        }
        return sb.toString();
    }

    private static Path writeProbeConfig() throws IOException {
        Path config = Files.createTempFile("ygy-probe", ".json");
        String json = "{"
                + "\"server\":{\"ip\":\"config-probe.invalid\",\"tcp_port\":5101,"
                + "\"http_port\":5102,\"timeout\":4321},"
                + "\"network\":{\"connect_timeout\":1111,\"read_timeout\":2222,"
                + "\"buffer_size\":3333,\"max_redirects\":1},"
                + "\"cache\":{\"enabled\":false}"
                + "}";
        Files.write(config, json.getBytes(StandardCharsets.UTF_8));
        return config;
    }

    private static String runConfigProbe(Path config, Map<String, String> extraEnv) throws Exception {
        String javaHome = System.getProperty("java.home");
        String executable = System.getProperty("os.name").toLowerCase().contains("windows")
                ? "java.exe" : "java";
        File java = new File(new File(javaHome, "bin"), executable);
        String classpath = System.getProperty("surefire.test.class.path",
                System.getProperty("java.class.path"));

        ProcessBuilder builder = new ProcessBuilder(
                java.getAbsolutePath(),
                "-cp", classpath,
                "-Dyaoguangyun.config=" + config.toAbsolutePath(),
                "com.yaoguangyun.ConfigProbe");
        builder.redirectErrorStream(true);

        String[] variables = {
                "YAOGUANGYUN_SERVER_IP", "YAOGUANGYUN_TCP_PORT", "YAOGUANGYUN_HTTP_PORT",
                "YAOGUANGYUN_RSA_PUBLIC_KEY", "YAOGUANGYUN_APP_KEY", "YAOGUANGYUN_CONFIG"
        };
        for (String variable : variables) {
            builder.environment().remove(variable);
        }
        if (extraEnv != null) {
            builder.environment().putAll(extraEnv);
        }

        Process process = builder.start();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = process.getInputStream().read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        assertTrue("配置探针应在 30 秒内结束", process.waitFor(30, TimeUnit.SECONDS));
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    private static byte[] dnsResponse(int transactionId, int flags, String ip) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(output);
        data.writeShort(transactionId);
        data.writeShort(flags);
        data.writeShort(1); // QDCOUNT
        data.writeShort(1); // ANCOUNT
        data.writeShort(0); // NSCOUNT
        data.writeShort(0); // ARCOUNT

        for (String label : new String[] {"www", "example", "com"}) {
            data.writeByte(label.length());
            data.write(label.getBytes(StandardCharsets.US_ASCII));
        }
        data.writeByte(0);
        data.writeShort(1); // QTYPE A
        data.writeShort(1); // QCLASS IN

        data.writeShort(0xC00C); // 指向查询名的压缩指针
        data.writeShort(1);      // TYPE A
        data.writeShort(1);      // CLASS IN
        data.writeInt(60);       // TTL
        data.writeShort(4);      // RDLENGTH
        for (String octet : ip.split("\\.")) {
            data.writeByte(Integer.parseInt(octet));
        }
        data.flush();
        return output.toByteArray();
    }

    private static String invokeParseDnsResponse(byte[] response, int expectedTransactionId) throws Exception {
        java.lang.reflect.Constructor<HttpsDnsClient> constructor =
                HttpsDnsClient.class.getDeclaredConstructor(String.class);
        constructor.setAccessible(true);
        HttpsDnsClient client = constructor.newInstance("www.example.com");

        Method method = HttpsDnsClient.class.getDeclaredMethod(
                "parseDnsResponse", byte[].class, int.class);
        method.setAccessible(true);
        try {
            return (String) method.invoke(client, response, expectedTransactionId);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw e;
        }
    }

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
