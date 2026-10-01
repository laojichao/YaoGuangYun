# YaoGuangYun 网络请求迁移项目

## 项目概述

本项目从 `bbc.me` Android 应用中提取并迁移了实际的网络请求实现到 Java 项目中。

### 核心功能

1. **TCP 客户端通信**
   - 自定义 TCP 协议实现（`"Epic"` 魔数帧，非标准 Protobuf 线格式）
   - 支持 GZIP 压缩（响应自动解压）
   - 线程池管理（守护线程）
   - 超时与响应长度上限保护

2. **HTTP 请求处理**
   - 基于 HttpURLConnection 的实现
   - 支持重定向（最多 5 次）
   - 自定义请求头
   - 超时设置

3. **设备信息管理**
   - 完整的设备信息收集
   - 支持设备信息伪装
   - 包含 70+ 设备参数

4. **加密工具**
   - RSA 公钥加密/私钥解密（2048 位，单块上限 245 字节）
   - RSA 数字签名/验签（SHA256withRSA，验签失败返回 false）
   - Base64 编码/解码

5. **JSON数据处理**
   - Gson 集成和工具类
   - JSON 序列化和反序列化
   - 配置文件解析

6. **Hutool 工具包**
   - 字符串、日期、ID 生成工具
   - HTTP 客户端和 JSON 处理
   - 加密解密和编码工具

## 项目结构

```
src/main/java/com/yaoguangyun/
├── Main.java                    # 程序入口
├── network/                     # 网络模块
│   ├── TcpClient.java          # TCP 客户端
│   ├── HttpClient.java         # HTTP 客户端
│   ├── ServerConfig.java       # 服务器配置
│   └── JsonDataProcessor.java  # JSON 数据处理器
├── device/                      # 设备模块
│   ├── DeviceInfo.java         # 设备信息类
│   └── DeviceInfoManager.java  # 设备信息管理器
├── proto/                       # 协议模块
│   ├── BasicDeviceInfo.java    # 基本设备信息
│   ├── CardProtobufMessage.java # 卡片消息
│   └── ...其他协议类
├── util/                        # 工具模块
│   ├── RsaEncryptUtils.java    # RSA 加密工具
│   ├── GsonUtils.java          # Gson 工具类
│   ├── HutoolUtils.java        # Hutool 工具类
│   └── AppUtils.java           # 应用工具类
├── config/                      # 配置模块
│   └── JsonConfigParser.java   # JSON 配置解析器
├── example/                     # 示例模块
│   ├── NetworkExample.java     # 网络请求示例
│   ├── GsonExample.java        # Gson 使用示例
│   └── HutoolExample.java      # Hutool 使用示例
└── test/                        # 测试模块
    ├── NetworkTest.java        # 网络功能测试
    └── SimpleHttpServer.java   # 本地测试服务器
```

## 快速开始

### 使用 Maven 构建（推荐）

```bash
# 编译项目
mvn clean compile

# 运行单元测试（JUnit，离线可跑）
mvn test

# 运行完整示例
mvn exec:java -Dexec.mainClass="com.yaoguangyun.Main"

# 运行功能自检（本地检查失败时以非0退出码结束）
mvn exec:java -Dexec.mainClass="com.yaoguangyun.Main" -Dexec.args="test"

# 启动本地测试服务器（仅监听回环地址）
mvn exec:java -Dexec.mainClass="com.yaoguangyun.Main" -Dexec.args="server"
```

## 使用示例

### 1. TCP 请求

```java
TcpClient tcpClient = new TcpClient();
String message = "{\"action\":\"test\"}";

tcpClient.sendMessage(message.getBytes(), new TcpResponseCallback() {
    @Override
    public void onSuccess(String response) {
        System.out.println("响应: " + response);
    }
    
    @Override
    public void onError(Exception e) {
        System.err.println("错误: " + e.getMessage());
    }
});
```

### 2. HTTP 请求

```java
HttpClient httpClient = new HttpClient(5000);
httpClient.addHeader("User-Agent", "Mozilla/5.0");

InputStream response = httpClient.get("http://httpbin.org/get");
```

### 3. 设备信息管理

```java
DeviceInfoManager manager = DeviceInfoManager.getInstance();
manager.collectDeviceInfo();

DeviceInfo deviceInfo = manager.getDeviceInfo();
System.out.println("品牌: " + deviceInfo.getBrand());
System.out.println("型号: " + deviceInfo.getModel());
```

### 4. RSA 加密

```java
KeyPair keyPair = RsaEncryptUtils.generateKeyPair();
String publicKey = RsaEncryptUtils.getPublicKeyBase64(keyPair);
String privateKey = RsaEncryptUtils.getPrivateKeyBase64(keyPair);

String encrypted = RsaEncryptUtils.encrypt("Hello", publicKey);
String decrypted = RsaEncryptUtils.decrypt(encrypted, privateKey);
```

### 5. Hutool 工具

```java
// 字符串工具
boolean isEmpty = HutoolUtils.isEmpty("test");
String formatted = HutoolUtils.format("Hello, {}!", "World");

// 日期工具
String now = HutoolUtils.now();

// ID 生成
String uuid = HutoolUtils.randomUUID();

// 加密工具
String md5 = HutoolUtils.md5("Hello");

// HTTP 工具
String response = HutoolUtils.httpGet("http://httpbin.org/get");
```

## 依赖配置

### Maven 依赖

```xml
<dependencies>
    <!-- Gson -->
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.10.1</version>
    </dependency>

    <!-- Hutool -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-core</artifactId>
        <version>5.8.22</version>
    </dependency>
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-http</artifactId>
        <version>5.8.22</version>
    </dependency>
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-json</artifactId>
        <version>5.8.22</version>
    </dependency>
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-crypto</artifactId>
        <version>5.8.22</version>
    </dependency>
</dependencies>
```

## 文档说明

- **README.md** - 项目主文档（本文件）
- **CLAUDE.md** - 面向 AI 编码助手的项目说明（架构、构建、约定）
- **HUTOOL_USAGE.md** - Hutool 使用指南
- **AUDIT_REPORT.md** - 代码全面排查报告：问题清单、修复状态与对抗性复核结论

## 测试与自检

```bash
# 单元测试（JUnit 4，离线可跑，不依赖外部服务器）
mvn test

# 功能自检：本地检查失败时以非 0 退出码结束，可用于 CI 判定回归
java -jar target/YaoGuangYun-1.0.0.jar test
```

`mvn test` 当前有 36 个用例，覆盖 RSA 加解密与签名、TCP 响应解码（含压缩炸弹防护）、
HTTP 重定向语义、DNS 报文边界、Gson 严格校验、设备信息双视图一致性、配置解析容错等。

## 注意事项

1. **网络连接**: 某些功能需要实际的网络连接才能正常工作（单元测试不需要）
2. **服务器配置**: 项目使用 `hc.t60.top` 作为服务器地址，可通过 `config.json` 或环境变量调整
3. **编码问题**: 项目使用 UTF-8 编码
4. **依赖管理**: 使用 Maven 管理依赖（Gson 来自 Maven 仓库，仓库内不附带 jar）
5. **配置**: `config.json` 位于项目根目录，会实际驱动 `ServerConfig`；打包后随 jar 一起发布
6. **密钥**: `security.app_key` 建议改用环境变量 `YAOGUANGYUN_APP_KEY` 注入，不要提交到仓库

## 许可证

本项目仅供学习和研究使用。