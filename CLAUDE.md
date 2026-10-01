# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

YaoGuangYun is a Java library extracted from the `bbc.me` Android app, reimplementing its network communication layer. It provides TCP and HTTP clients for communicating with the `hc.t60.top` server, along with device info management, RSA encryption, and JSON data processing. This is a standalone Java project (not Android) — device info is simulated with hardcoded Samsung Galaxy S8 data.

## Build and Run Commands

```bash
# Compile
mvn clean compile

# Run unit tests (JUnit 4, offline & deterministic)
mvn test

# Run the full demo (NetworkExample)
mvn exec:java -Dexec.mainClass="com.yaoguangyun.Main"

# Run the self-check (exits non-zero if any local check fails)
mvn exec:java -Dexec.mainClass="com.yaoguangyun.Main" -Dexec.args="test"

# Start local test HTTP server on port 8080 (loopback only)
mvn exec:java -Dexec.mainClass="com.yaoguangyun.Main" -Dexec.args="server"

# Package (produces target/YaoGuangYun-1.0.0.jar + target/lib/)
mvn package
```

**Java target: 8.** `maven.compiler.release` is set explicitly, so the build fails at compile time if
any Java 9+ API slips in (this is deliberate — see `maven.compiler.release` in `pom.xml`).
Profiles switch the target: `-P java11`, `-P java17` (they override `maven.compiler.release`).

> Building requires a JDK 9+ (for `--release`); the build machine here uses JDK 17.

### Configuration

项目配置在根目录 `config.json`，包含以下节:
- `server` — 服务器地址、端口、超时
- `device` — 设备信息模拟参数
- `network` — 连接/读取超时、缓冲区大小、最大重定向次数
- `security` — RSA 公钥、应用密钥
- `cache` / `logging` — 缓存和日志设置

`config.json` **会实际驱动运行时**：`ServerConfig` 在类初始化时加载它
（解析路径顺序：系统属性 `yaoguangyun.config` → 环境变量 `YAOGUANGYUN_CONFIG` → 工作目录 `config.json`
→ jar 内 classpath 资源），缺失时回退到内置默认值。

密钥类配置可用环境变量覆盖，便于把凭据排除在源码之外：
`YAOGUANGYUN_SERVER_IP`、`YAOGUANGYUN_TCP_PORT`、`YAOGUANGYUN_HTTP_PORT`、
`YAOGUANGYUN_RSA_PUBLIC_KEY`、`YAOGUANGYUN_APP_KEY`。

## Architecture

All source is under `src/main/java/com/yaoguangyun/`. Entry point is `Main.java` which dispatches based on CLI args.

### Module Layout

- **network/** — Core networking layer
  - `TcpClient` — Custom TCP protocol using a 16-thread daemon pool. Protocol format: `[magic:"Epic"][msgType:4B][compressedLen:4B][gzip data]`. The response body is GZIP-decompressed when it carries a gzip header (plain bodies fall back to raw bytes), and decompression is bounded — a compressed payload that would expand past 64 MB is rejected rather than allowed to exhaust the heap. RSA decryption is attempted only when the payload length exactly matches the configured key's ciphertext length. Call `TcpClient.shutdown()` to release the pool; `sendMessage` reports a shut-down pool through `onError` instead of throwing.
  - `HttpClient` — `HttpURLConnection`-based with manual redirect handling, custom headers, and a content-length aware stream wrapper. 307/308 redirects preserve method and body, 301/302/303 downgrade to GET; `network.max_redirects` counts redirects followed (so `0` means "don't follow redirects" and still allows the initial request). Every non-stream exit path disconnects the connection.
  - `ServerConfig` — Loads server address, ports, timeouts, buffer size, RSA public key, app key and cache flag from `config.json`, with environment-variable overrides and hardcoded fallbacks.
  - `JsonDataProcessor` — Builds request JSON (`CardProtobufMessage` with device info), parses server responses, and provides JSON merge/extract utilities via Gson.
  - `HttpsDnsClient` — DNS-over-HTTPS resolver (opt-in; the TCP client uses the JVM's default resolver).
  - `TcpMessageType` — Enum defining TCP message types (verify, heartbeat, etc.). **The single definition lives here** — an earlier duplicate in `proto/` had conflicting numeric values and was removed.

- **proto/** — Protocol/message model classes (not actual Protobuf-generated)
  - `CardProtobufMessage` — Card message with `BasicDeviceInfo`, operation type, and a `Map<String, String>` data payload. This is what `JsonDataProcessor` actually puts on the wire.
  - `BasicDeviceInfo` — Device identity fields (locale, androidId, version, statusMachine, time).
  - `CardType`, `CardOperationType` — Enums for message categories and operations. Both carry a Gson `TypeAdapter` that writes the enum name and reads either the name or the numeric value.
  - `ProtobufMessage` — Optional top-level wrapper with `dataType` (Soft/Card). **Not currently used by `JsonDataProcessor`**, which sends the unwrapped `CardProtobufMessage`; available for callers that need the wrapper.
  - `DnsMessage`, `RequestDataMap` — Supporting model classes, not referenced by the default request path.

- **device/** — Device info simulation
  - `DeviceInfo` — POJO with 69 Android device fields (brand, model, network, GPS, screen, SIM, etc.), all mapped to snake_case JSON names via `@SerializedName`.
  - `DeviceInfoManager` — Singleton that populates `DeviceInfo` with Samsung Galaxy S8 mock data. The `DeviceInfo` object is the single source of truth; `getDeviceInfoMap()` derives its keys from `@SerializedName`, so the map view and the JSON view never disagree.

- **util/** — Utilities
  - `RsaEncryptUtils` — RSA encrypt/decrypt plus real signatures (`sign`/`verify`, SHA256withRSA). Single-block encryption is capped at 245 bytes for a 2048-bit key and throws `IllegalArgumentException` beyond that.
  - `GsonUtils` — Gson-based JSON handling. `isValidJson` parses in **strict** mode (Gson's default parser is lenient and would accept `{key: value}`).
  - `HutoolUtils` — Wrapper around Hutool for string, date, ID, hash, HTTP, and JSON operations. HTTP helpers set an explicit 5 s timeout (Hutool's default is "no timeout").
  - `AppUtils` — General app utilities (hex/Base64/charset conversion, `SecureRandom` string generation).

- **config/** — `JsonConfigParser` parses JSON config files (dotted keys, e.g. `server.ip`); falls back to the classpath when the path is not a file.

- **example/** — Demo classes (`NetworkExample`, `GsonExample`, `HutoolExample`) showing usage of each module.

- **test/** — `NetworkTest` (self-check with an exit code) and `SimpleHttpServer` (loopback-only local test server). These live in the **main** source set because `Main` exposes them as CLI commands.

### Tests

Real JUnit 4 tests live in `src/test/java/com/yaoguangyun/YaoGuangYunTest.java` and run via `mvn test`.
They are offline and deterministic (they spin up `SimpleHttpServer` on an ephemeral port rather than
hitting `hc.t60.top`). `mvn exec:java -Dexec.args="test"` runs `NetworkTest`, which counts failures and
returns them as the process exit code.

### Key Dependencies

- **Gson 2.10.1** — JSON serialization (resolved from Maven Central; there is no vendored jar)
- **Hutool 5.8.22** — Utility toolkit (core, http, json, crypto). `HUTOOL_USAGE.md` has the usage guide.
- **JUnit 4.13.2** — Unit tests (test scope)

Maven repos include Aliyun mirror as primary, Maven Central as fallback. A first build needs network access.

## Important Notes

- The default server address is `hc.t60.top` (from `config.json` / `ServerConfig`). Network features require it to be reachable; the unit tests do not.
- TCP protocol uses a custom framing format with "Epic" magic bytes — not standard Protobuf wire format despite the class naming.
- Device info in `DeviceInfoManager.collectDeviceInfo()` is entirely simulated. There is no real Android Context.
- The RSA key in `ServerConfig`/`config.json` is the public key extracted from the original app.
- The committed `app_key` is a legacy credential from the original app; prefer injecting it via `YAOGUANGYUN_APP_KEY` and rotate it if it is still live.
