# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

YaoGuangYun is a Java library extracted from the `bbc.me` Android app, reimplementing its network communication layer. It provides TCP and HTTP clients for communicating with the `hc.t60.top` server, along with device info management, RSA encryption, and JSON data processing. This is a standalone Java project (not Android) — device info is simulated with hardcoded Samsung Galaxy S8 data.

## Build and Run Commands

```bash
# Compile
mvn clean compile

# Run the full demo (NetworkExample)
mvn exec:java -Dexec.mainClass="com.yaoguangyun.Main"

# Run functional tests
mvn exec:java -Dexec.mainClass="com.yaoguangyun.Main" -Dexec.args="test"

# Start local test HTTP server on port 8080
mvn exec:java -Dexec.mainClass="com.yaoguangyun.Main" -Dexec.args="server"

# Package
mvn package
```

Java target: 1.8. Profiles available for Java 11 and 17 (`-P java11`, `-P java17`).

## Architecture

All source is under `src/main/java/com/yaoguangyun/`. Entry point is `Main.java` which dispatches based on CLI args.

### Module Layout

- **network/** — Core networking layer
  - `TcpClient` — Custom TCP protocol using a 16-thread pool. Protocol format: `[magic:"Epic"][msgType:4B][compressedLen:4B][gzip data]`. Uses RSA to attempt decrypting responses.
  - `HttpClient` — `HttpURLConnection`-based with manual redirect handling (max 5), custom headers, and content-length aware stream wrapper.
  - `ServerConfig` — Static config holder for server address (`hc.t60.top`), ports (TCP:5000, HTTP:5600), RSA public key, and app key.
  - `JsonDataProcessor` — Builds request JSON (`CardProtobufMessage` with device info), parses server responses, and provides JSON merge/extract utilities via Gson.
  - `HttpsDnsClient` — DNS-over-HTTPS resolver.
  - `TcpMessageType` — Enum defining TCP message types (verify, heartbeat, etc.).

- **proto/** — Protocol/message model classes (not actual Protobuf-generated)
  - `ProtobufMessage` — Top-level message wrapper with `dataType` (Soft/Card) discriminating between `DnsMessage` and `CardProtobufMessage`.
  - `CardProtobufMessage` — Card message with `BasicDeviceInfo`, operation type, and a `Map<String, String>` data payload.
  - `BasicDeviceInfo` — Device identity fields (locale, androidId, version, statusMachine, time).
  - `CardType`, `CardOperationType` — Enums for message categories and operations.
  - `RequestDataMap` — Request data container.

- **device/** — Device info simulation
  - `DeviceInfo` — POJO with 70+ Android device fields (brand, model, network, GPS, screen, SIM, etc.).
  - `DeviceInfoManager` — Singleton that populates `DeviceInfo` with Samsung Galaxy S8 mock data. Provides both the object and a `Map<String, String>` view.

- **util/** — Utilities
  - `RsaEncryptUtils` — RSA encrypt/decrypt, sign/verify, Base64 encode/decode using JDK crypto.
  - `GsonUtils` — Gson-based JSON validation, pretty-print, Map conversion, field extraction.
  - `HutoolUtils` — Wrapper around Hutool for string, date, ID, hash, HTTP, and JSON operations.
  - `AppUtils` — General app utilities.

- **config/** — `JsonConfigParser` parses `config.json` at project root for server, device, network, security, cache, and logging settings.

- **example/** — Demo classes (`NetworkExample`, `GsonExample`, `HutoolExample`) showing usage of each module.

- **test/** — `NetworkTest` for integration testing against the server; `SimpleHttpServer` for local testing.

### Key Dependencies

- **Gson 2.10.1** — JSON serialization
- **Hutool 5.8.22** — Utility toolkit (core, http, json, crypto, socket)
- **JUnit 4.13.2** — Testing (test scope)

Maven repos include Aliyun mirror as primary, Maven Central as fallback.

## Important Notes

- The project uses hardcoded server address `hc.t60.top` in `ServerConfig`. Network features require this server to be reachable.
- TCP protocol uses a custom framing format with "Epic" magic bytes — not standard Protobuf wire format despite the class naming.
- Device info in `DeviceInfoManager.collectDeviceInfo()` is entirely simulated. There is no real Android Context.
- The RSA key in `ServerConfig` is the public key extracted from the original app.
