# YaoGuangYun 代码全面排查报告（汇总）

> 排查方式：4 个独立子代理并行审计（网络层 / 协议-设备-配置层 / 工具-示例-测试层 / 构建-打包-跨切面），
> 全部结论均以**实际编译、运行、探针程序**验证，而非静态猜测。
> 环境：Maven 3.9.14、JDK 17.0.18、Windows、默认编码 GBK。
> 基线：`mvn clean compile` / `package` / `test` 均 BUILD SUCCESS（`test` 实际执行 0 个用例）。

本文件为**排查汇总**。修复情况见文末「修复状态」一节。

---

## 一、结论速览

| 级别 | 数量 | 代表问题 |
|------|------|----------|
| Critical | 1 | `androidId` 从未赋值 → 所有请求的设备标识为空 |
| High | 8 | 声明的 Java 8 兼容被打破；`-P java11/java17` 形同虚设；签名 API 是假的；`isValidJson` 不校验；HTTP 无超时；TCP 响应不解压；配置解析崩溃；无任何自动化测试 |
| Medium | 24 | 连接泄漏、POST 重定向丢失请求体、Gson 反序列化 NPE、枚举名/数值契约冲突、设备信息双视图漂移、本地服务器绑定全网卡等 |
| Low | 27 | 死代码、文档与实现不符、未用依赖、`.gitignore` 缺项等 |

**总体判断**：项目能编译、能打包、能跑，但「能跑」掩盖了大量静默错误——**没有任何一个模式会以非 0 退出码失败**，
`mvn test` 执行 0 个用例，因此所有缺陷都不会被自动发现。

---

## 二、Critical

### C-1 `androidId` 从未被赋值，主请求的设备标识永远为空
- **位置**：`device/DeviceInfoManager.java:63-140`（消费方 `network/JsonDataProcessor.java:46,80`）
- **证据**：`collectDeviceInfo()` 设置了 69 个字段中的 59 个，**唯独没有** `setAndroidId(...)`；
  另有 `serial`、`cellLocation`、`dataActivity`、`extraInfo`、5 个 `scanResults*` 共 10 个字段为 null。
- **后果**：`buildVerifyRequest()` 产出 `"basic":{"locale":"zh","android_id":"",...}`（`BasicDeviceInfo.setAndroidId` 把 null 归一为 `""`）。
  服务器用来识别/绑定设备的唯一字段永远是空字符串。心跳请求同样。
- **验证**：运行真实 `buildVerifyRequest` 打印 JSON 确认。

---

## 三、High

### H-1 声明的 Java 8 兼容性被打破
- **位置**：`test/SimpleHttpServer.java:90`
- **证据**：`exchange.getRequestBody().readAllBytes()` 是 Java 9+ API。
  `javac --release 8` 编译全部 24 个源文件时**仅此一行**报错：
  `error: cannot find symbol  symbol: method readAllBytes()  location: class InputStream`。
- **后果**：当前只因构建 JDK 是 17 才通过；在真实 Java 8 运行时报 `NoSuchMethodError`，用 JDK 8 的消费者无法编译。

### H-2 `-P java11` / `-P java17` 完全失效
- **位置**：`pom.xml:109-118`（插件硬编码）vs `pom.xml:253-267`（profile）
- **证据**：`-P java17` 下 `maven.compiler.target` 求值为 `17`，但产物 `Main.class` 的
  `major version: 52`（Java 8），编译日志恒为 `target 1.8`。
  插件 `<configuration><source>1.8</source><target>1.8</target></configuration>` 覆盖了 profile 设置的属性。
- **后果**：`CLAUDE.md` 宣称的「Profiles available for Java 11 and 17」是假的。

### H-3 「签名/验证」不是签名，且没有返回 false 的 verify
- **位置**：`util/RsaEncryptUtils.java:144-200`（javadoc 声明见 `:27`）
- **证据**：`TRANSFORMATION = "RSA/ECB/PKCS1Padding"`，`encryptWithPrivateKey` 用的是
  `Cipher.ENCRYPT_MODE` + 私钥 —— 这是 PKCS#1 v1.5 **加密**，不是签名：没有先做哈希。
  探针：篡改后 `decryptWithPublicKey` **抛异常**而不是返回 false，调用方无法用布尔值判断真伪。
- **订正（复核时确认）**：子代理报告中「同一消息两次签名完全相同 → 可伪造」的说法**不成立**。
  PKCS#1 v1.5 签名本身就是确定性的（无随机盐），这是正常行为；伪造签名仍需私钥。
  真正的缺陷是：该 API 没有摘要、语义上不是签名、且没有布尔返回的验签。
- **后果**：`NetworkExample.java:176-189`、`NetworkTest.java:72-80` 都在打印「签名验证测试成功」，向使用者示范了错误的安全模式。

### H-4 `isValidJson` 不做校验
- **位置**：`util/GsonUtils.java:422-432`
- **证据**：`JsonParser.parseString` 是**宽松模式**。探针：
  `isValidJson("{key: value}") -> true`、`isValidJson("abc") -> true`、`isValidJson("123") -> true`；
  `formatJson("{key: value}")` 会把它「修好」成 `{"key":"value"}`。
- **可见症状**：示例自身输出 `无效JSON验证: true`——标签与结果自相矛盾。

### H-5 Hutool HTTP 封装没有超时，会永久阻塞
- **位置**：`util/HutoolUtils.java:293-315`
- **证据**：`HttpUtil.get(url)` 未设置 timeout；Hutool 全局默认 `HttpGlobalConfig.getTimeout() -> -1`。
  探针：对「接受连接但永不响应」的黑洞服务器，`HutoolUtils.httpGet` **25 秒后仍阻塞，被迫 kill**。
  `HutoolExample.java:204/212/216` 在主线程同样裸调。

### H-6 TCP 响应从不做 GZIP 解压
- **位置**：`network/TcpClient.java:129-138`（编码端 `:112-114`）
- **证据**：类 javadoc 声明帧格式为 `[magic][msgType:4B][compressedLen:4B][gzip data]`，请求体确实 GZIP 压缩，
  但响应读出后直接 `new String(bytes, UTF_8)`，**全仓库不存在 `GZIPInputStream`/`Inflater`**（grep 确认）。
- **后果**：若服务器按帧格式回 gzip，响应必然是乱码；随后 RSA 解密失败被 catch 吞掉，**乱码被当作成功响应返回**。

### H-7 `JsonConfigParser.load()` 遇到非法 JSON 直接抛异常
- **位置**：`config/JsonConfigParser.java:60-80`
- **证据**：只 catch `IOException`。截断的 config.json 抛 `JsonSyntaxException`；
  `[1,2,3]` 这类合法但非对象的文档抛 `IllegalStateException`（均已复现）。
  调用方 `GsonExample.java:306` 是 `boolean loaded = config.load();` 无 try/catch。
- **后果**：用户配置文件里一个字符的笔误 → 程序崩溃，而不是打印「加载失败」。
  另：空文件时 `configData` 不重置却仍 `return true`（陈旧配置被当作加载成功）。

### H-8 无任何自动化测试，且所有模式恒以 0 退出
- **位置**：`test/NetworkTest.java:23-42` + `Main.java:35-79`
- **证据**：
  - `mvn test` → surefire 输出中**没有** `Tests run:` 行（执行 0 个用例）；`src/test/java` 目录根本不存在。
  - `Main` 忽略 `NetworkTest` 结果、从不 `System.exit`（全仓库 grep 无 `System.exit`）。
  - 实测：`-Dexec.args=test` 出现两处硬失败（`HTTP error: 405`、`TCP请求失败: null`），
    仍然打印「测试完成」，**退出码 0**。

---

## 四、Medium

### 网络层
- **M-1 重定向分支的连接泄漏**（`HttpClient.java:168-178`）：`connection.disconnect()` 位于 `Location` 校验**之后**，
  3xx 缺 `Location` 或 URL 非法时抛异常 → 连接不释放；`getResponseCode()` 抛异常、`post()` 写体抛异常同理。
- **M-2 POST 被重定向后降级为 GET 并丢失请求体**（`HttpClient.java:84-97` + `106-115`）：
  重定向一律递归进 `executeRequest`（无 `setDoOutput`/无 body）。307/308 本应保持方法与请求体。
- **M-3 `HttpClient.headers` 非线程安全**（`HttpClient.java:34,61-64`）：`HashMap` 被 `createConnection` 迭代、
  又被 `addHeader` 修改；链式 API 诱导跨线程复用。
- **M-4 `parseDnsResponse` 无边界检查**（`HttpsDnsClient.java:158-200`）：截断/伪造响应抛
  `BufferUnderflowException`（未受检，逃出 `throws IOException` 契约）。**已用探针复现**。
- **M-5 `buildVerifyRequest`/`buildHeartbeatRequest` 对 null 设备信息 NPE**（`JsonDataProcessor.java:42-49,76-83`）：
  而 `addDeviceInfoToCard` 明确做了 null 保护，作者意图是容忍 null。

### 协议 / 设备 / 配置层
- **M-6 `op` 的序列化契约冲突**（`proto/CardProtobufMessage.java:33-34,103-113`）：
  Gson 按枚举 **name** 写出 `"op":"Verify"`，但类上提供 `getOpValue()/setOpValue(int)` 与 `OP_FIELD_NUMBER`；
  **读取时数值输入静默变 null**（`fromJson("{\"op\":0}").getOp() == null`），未知名字也变 null。
- **M-7 Gson 绕过 setter 造成 null，进而 NPE**（`CardProtobufMessage` / `BasicDeviceInfo` / `DnsMessage`）：
  构造函数与 setter 都做了 null 归一，但 Gson 直接反射写字段。
  探针：`fromJson("{\"op\":null,\"data\":null}")` 后 `hashCode()`/`getDataCount()`/`getOpValue()` 全部 NPE。
- **M-8 `DeviceInfo` 的 JSON 键命名混乱**：42 个字段 snake_case、27 个字段 camelCase（未加 `@SerializedName`）。
  而 `JsonDataProcessor.addDeviceInfoToCard` 用的是 snake_case。
- **M-9 `DeviceInfoManager` 双视图键名不一致且会永久漂移**：
  map 用 camelCase（`androidId`/`sdkInt`），同一对象的 JSON 用 snake_case（`android_id`/`sdk_int`）；
  `setDeviceInfo()` 只同步 3/69 个字段（`// ... 其他字段的设置`），且接受任意不存在的键。
- **M-10 `DeviceInfoManager` 共享可变状态无同步**（`HashMap` + `DeviceInfo` 被单例共享，
  `updateDeviceInfoMap()` 先 `clear()` 再 69 次 `put`，读取方无锁）→ 可能读到半成品 map 或 `ConcurrentModificationException`。
  （注：`getInstance()` 本身是 `synchronized`，**不存在** DCL 缺 volatile 的问题——子代理已证伪该假设。）
- **M-11 枚举哨兵值可被序列化上线**（`CardType`/`CardOperationType`）：`setOpValue(9)` 存入 `UNRECOGNIZED`，
  `getOpValue()` 抛 `IllegalArgumentException`，而序列化会发出 `"op":"UNRECOGNIZED"`。

### 工具 / 示例 / 测试 / 服务器
- **M-12 `getString`/`getBoolean` 对非原始值抛异常，`getInt/getLong/getDouble` 却吞掉返回 0**（`GsonUtils.java:197-206,256-265`）：
  5 个同类方法两套契约；`getBoolean` 缺 try/catch。探针：`getString(o,"nested")` 抛 `UnsupportedOperationException: JsonObject`。
- **M-13 `hexToBytes` 静默产生错误字节**（`AppUtils.java:51-64`）：javadoc 承诺 `@throws IllegalArgumentException`，
  但 `Character.digit` 返回 -1 被折叠进字节。探针：`hexToBytes("zz")` → 无异常，返回 `ef`；`hexToBytes("7g")` → `6f`（'o'）。
- **M-14 `HutoolUtils.generateRSAKeyPair()` 静默生成 1024 位密钥**（`:260-262`），与项目 2048 位策略不一致
  （探针：1024 vs 2048；示例输出 `216/848` vs `392/1624`）。
- **M-15 `NetworkExample` 单次 `read()` 只读 1024 字节**（`:100-113`）：更大响应被截断（可能切断 UTF-8 字符）
  却紧接着打印「✓ HTTP请求成功」。
- **M-16 `NetworkExample` 第二个 URL 在服务器上不存在**（`:86-89`）：`http://hc.t60.top:5600/api/test` → **HTTP 404**，
  即项目自己的主入口每次都对自家后端报红。
- **M-17 `NetworkTest` 用 GET 请求 POST-only 的 `httpbin.org/post`**（`:101-105`）→ 永久 405 红。
- **M-18 本地测试服务器绑定通配地址**（`SimpleHttpServer.java:31`）：`new InetSocketAddress(port)` 绑定 `0.0.0.0` + `[::]`（netstat 实测），
  把含 `/api/echo`（回显请求体）的未鉴权端点暴露到局域网。
- **M-19 未设置 executor → 单个卡住的客户端阻塞全部请求**（`SimpleHttpServer.java:30-42`）：
  实测慢客户端占住时，第二个请求 4016 ms 后 `SocketTimeoutException`。
- **M-20 未知路径回落到 `/` 返回 200**（`SimpleHttpServer.java:34`）：`GET /api/nope` → 200，
  测试服务器永远无法返回 404，URL 拼错会「静默成功」。
- **M-21 `config.json` 完全不驱动运行时**：全仓库只有 `GsonExample.java:303` 构造 `JsonConfigParser`，
  `ServerConfig` 硬编码一切；26 个键中 16 个无任何读取方；`security.*` 与 `ServerConfig` 的密钥**逐字节重复**。
- **M-22 `Main` 从不传递失败**（同 H-8）：任何模式退出码恒为 0。
- **M-23 `hutool-socket` 声明但从未 import**（`pom.xml:62-67`）：`dependency:analyze` 报警，且被拉进 `target/lib` 与 manifest `Class-Path`。
- **M-24 已提交的 `lib/gson-2.10.1.jar` 不在任何 classpath 上**：Maven 从 `~/.m2` 解析；
  且与 `CLAUDE.md:78`「local jar in `lib/`」的说法矛盾。

---

## 五、Low（摘要）

- **死代码/重复**：`proto/TcpMessageType`（**数值与 network 版全部错位**：VERIFY 0 vs 1、HEARTBEAT 1 vs 2、DATA 2 vs 3，且无任何引用）；
  `proto/ProtobufMessage`、`proto/DnsMessage`、`proto/RequestDataMap`、`util/HutoolUtils` 均无外部引用，
  但 `CLAUDE.md` 仍把 `ProtobufMessage` 描述为「Top-level message wrapper」（实际发送的是未包装的 `CardProtobufMessage`）。
- **`DeviceInfo` 缺 `equals`/`hashCode`/`toString`**，而同层 POJO 全都有。
- **`TcpClient`**：`resolveDns` 是死代码且吞异常；字段 `HTTP_PORT` 赋值后从未使用；线程池 `public static final` 从不 shutdown；
  `onSuccess` 抛异常会被 catch 成 `onError`（双重回调）。
- **`JsonConfigParser`**：`toJson()` 与 `toPrettyJson()` 是同一个方法（都是美化输出），javadoc 与行为不符。
- **`RequestDataMap`**：`contains(null)`/`concat(null)`/`empty(-1)` 抛 NPE/NegativeArraySizeException（当前不可达）。
- **`AppUtils.generateRandomString` 用 `Math.random()`**（可预测，不适合令牌）。
- **`HutoolUtils`**：未用 import `MapUtil`/`Header`；`Method.valueOf` 大小写敏感（`"get"` 直接抛异常）；
  `toMap` 返回 Hutool 嵌套类型而 `GsonUtils.toMap` 返回 Gson 类型，错误契约相反。
- **`Main.java:41`** `args[0].toLowerCase()` 未指定 Locale（土耳其语 I 问题）。
- **`NetworkTest`**：用固定 `Thread.sleep(2000)` 等待异步回调（竞态）；单次 `read()` 截断。
- **`NetworkExample`**：`e.printStackTrace()`；TCP 失败打印 `null`（异常无 message，实测 `TCP请求失败: null`）。
- **文档**：`Main.java:19-20` 提到不存在的 `run.bat`/`run.sh`；`README.md:12` 宣称「支持 Protobuf 消息格式」（实为自定义 `"Epic"` 魔数帧）。
- **仓库卫生**：`.codeartsdoer/` 既未跟踪也未 ignore。
- **打包**：`copy-dependencies` 把 junit/hamcrest（test 作用域）拷进 `target/lib`；绑定在 `jar:jar` **之后**；
  `src/main/java/.../test/` 被编进正式构件。
- **安全**：344 字符的 `app_key` 凭据硬编码在 `ServerConfig.java:25` 与 `config.json:26` 两处。

---

## 六、子代理已明确证伪 / 排除的假设

为避免后续重复排查，以下「疑似问题」经证据排除：

| 假设 | 结论 |
|------|------|
| `test/` 包依赖 JUnit 导致编译失败 | **假**。全仓库 0 处 `junit` 引用；`mvn package` SUCCESS。真实问题是 `mvn test` 跑 0 个用例。 |
| `DeviceInfoManager` 单例是 DCL 缺 `volatile` | **假**。`getInstance()` 是 `synchronized` 方法，发布安全。真问题是内部 `HashMap` 无同步。 |
| TCP 帧的字节序/字段宽度/偏移有错 | **干净**。编解码两端都是大端 4 字节，魔数恰好 4 字节，`readFully` + 长度上限校验正确。 |
| `ContentLengthInputStream` 会多读/少读 | **干净**。`Math.min(len, remaining)` 正确，`-1`/chunked 分支正确。 |
| `DeviceInfo` 69 个访问器有复制粘贴错误 | **干净**。69 字段/69 getter/69 setter/42 `@SerializedName`，无重复、无错位。 |
| 模拟设备的屏幕参数自相矛盾 | **干净**。`density 3.0 × 160 = density_dpi 480`，`1440/2960` 与 `width/height` 一致。 |
| `SimpleHttpServer` 的 context 路径重叠有 bug | **干净**。最长前缀匹配，`/api/test` 正确优先于 `/`。 |
| 重定向计数有 off-by-one 或死循环 | **干净**。计数 0 起、5 时抛错，最多跟随 5 次。 |
| `Main server` 启动约 4 秒后退出是缺陷 | **非缺陷**。stdin 处于 EOF，符合「按 Enter 停止」契约。 |
| `NetworkTest` 会永久挂起 | **不会**。`TcpClient` 有 5 秒连接/读超时且线程池是守护线程。 |

---

## 七、修复状态

修复按「先基础设施与安全，后正确性，再文档」的顺序分批实施，每批后编译 + 跑测试验证。

| 编号 | 问题 | 状态 | 关键验证 |
|------|------|------|----------|
| H-1 | Java 9 API 破坏 Java 8 兼容 | ✅ | `--release 8` 编译通过；`readAllBytes()` 已替换为手写循环 |
| H-2 | `-P java11/java17` 失效 | ✅ | 字节码 major version 实测 52 / 55 / 61 |
| H-4 | `isValidJson` 宽松解析 | ✅ | 探针：`{key: value}`、`abc`、`'a'`、`{} garbage` 全部返回 false |
| H-5 | Hutool HTTP 无超时 | ✅ | `HttpUtil.createGet(...).timeout(5000)` |
| H-7 | `JsonConfigParser.load()` 抛异常 | ✅ | 单元测试：截断 JSON / `[1,2,3]` 均返回 false 不抛异常 |
| C-1 | `androidId` 未赋值 | ✅ | 探针 + 单测：`android_id` = `9774d56d682e549c`，请求载荷不再为空 |
| H-6 | TCP 响应不解压 | ✅ | 新增 `GZIPInputStream` 路径（带 gzip 魔数判断 + 明文回退） |
| H-3 | 假签名 API | ✅ | 新增 `sign`/`verify`（SHA256withRSA）；单测覆盖篡改、异钥、非法签名 |
| H-8 / M-22 | 无测试、退出码恒 0 | ✅ | `mvn test` **36** 个用例全绿；`test` 模式 18/18 通过、未知命令退出码 1 |
| M-1 ~ M-5 | 网络层正确性/资源 | ✅ | 重定向各失败路径 disconnect；307/308 保留方法与请求体；DNS 解析加边界检查 |
| M-6 ~ M-11 | 协议/设备/配置正确性 | ✅ | 枚举适配器兼容名/数值；Gson null 不再 NPE；map 与 JSON 键名统一为 snake_case |
| M-12 ~ M-14 | 工具类正确性 | ✅ | 五个取值方法统一契约；`hexToBytes("zz")` 抛错；Hutool RSA 实测 2048 位 |
| M-15 ~ M-20 | 示例/测试/服务器 | ✅ | 完整读流；本地服务器回环绑定 + 线程池 + 未知路径 404 |
| M-21 | config.json 不驱动运行时 | ✅ | `ServerConfig` 启动时加载 config.json，支持环境变量覆盖；打包进 jar |
| M-23 / M-24 | 依赖与残留 jar | ✅ | `dependency:analyze` 无 unused；`lib/gson-2.10.1.jar` 已删除 |
| BUILD-6 | `exec:java` 线程泄漏告警 | ✅ | `NetworkTest` 结束时 `TcpClient.shutdown()`，告警消失 |
| Low 全部 | 死代码/文档/卫生 | ✅ | 删除重复的 `proto/TcpMessageType`；`.gitignore` 补项；README/CLAUDE.md/HUTOOL_USAGE.md 与实现对齐 |

### 验证方式

- `mvn clean test` → **Tests run: 36, Failures: 0, Errors: 0**
- `mvn exec:java -Dexec.args=test` → **18/18 通过，失败 0 项**，退出码 0；未知命令退出码 1
- `mvn package` → jar 内含 `config.json`，从任意工作目录运行均可加载配置
- 子代理结论中可复现的部分均用 `%TEMP%` 下的独立探针程序逐条验证（未在项目内留下临时文件）
- 修复完成后再由独立子代理做对抗性复核，见第八节

### 有意保留 / 需要业务确认的取舍

1. **设备信息 JSON 键名统一为 snake_case**（27 个原为 camelCase 的字段）。
   项目内占多数、且请求载荷构造器 `addDeviceInfoToCard` 都使用 snake_case，
   但**如果对端确实按 camelCase 解析这 27 个字段，需要改回**——仓库内没有协议样本可判定。
   零风险替代方案：`@SerializedName(value = "local_host", alternate = {"localHost"})`，两种键名都能读。
2. **`buildHeartbeatRequest` 仍使用 `CardOperationType.Verify`**：协议枚举里没有「心跳」取值，
   凭空发明一个数值风险更大，因此保留行为并修正 javadoc 说明。
3. **`proto/TcpMessageType` 直接删除**：它零引用，且数值与 `network` 版全部错位（VERIFY 0 vs 1）。
   若它才是真实协议定义，应反过来删除 `network` 版并修正偏移。
4. **`app_key` 仍留在源码/配置中**：已支持 `YAOGUANGYUN_APP_KEY` 注入，但未强制移除历史值；
   若该凭据仍然有效，应由业务侧轮换。
5. **`com.yaoguangyun.test` 仍位于主源码集**：`Main` 把它作为 CLI 子命令暴露，移走会破坏命令行功能。
6. **TCP 协议本身仍未与真实服务器跑通**：`hc.t60.top:5000` 可达，但一次真实请求返回
   `EOFException`。响应是否 gzip、是否 RSA、枚举用名还是数值，都无法从仓库内判定——
   上述相关改动都按「有则处理、无则回退」的方式实现，不会因猜测而破坏现有行为。

---

## 八、复核轮（对抗性验证）

修复完成后，另派一个独立子代理**以「找出修复本身的错误」为目标**做对抗性复核，
用 `%TEMP%` 下的探针程序逐条复现。结论：**构建/打包/测试类声明全部属实**
（26→36 个用例、`--release 8` 确实生效、jar 确实含 config.json 且可从任意目录运行、
`dependency:analyze` 无告警），但**新引入 1 个严重缺陷并留下若干契约漏洞**，已全部修复：

| 编号 | 复核发现 | 级别 | 处理 |
|------|----------|------|------|
| V-1 | **新加的 gzip 解压没有上限**：200KB 的压缩炸弹可膨胀到 200MB，`-Xmx128m` 下直接 `OutOfMemoryError` | High | 增加 `MAX_DECOMPRESSED_SIZE`，边解压边计数并中止；取消 `×4` 预分配 |
| V-2 | `verify()` 对「合法 Base64 但长度不对」的签名抛 `RuntimeException`，违背自身 javadoc 的「返回 false」 | Med-High | 捕获 `SignatureException` → 返回 false |
| V-3 | 枚举适配器的 `nextInt()` 对 `1.5`/超大数抛 `NumberFormatException`，绕过 `GsonUtils` 的 catch 逃到调用方 | Medium | 统一转成 `JsonSyntaxException` |
| V-4 | `setDeviceInfo` 重建对象 → 调用方已缓存的 `DeviceInfo` 引用变成过期副本 | Medium | 改为按 `@SerializedName` 反射**原地写字段**，保持对象身份 |
| V-5 | 配置值未校验：`max_redirects: 0` 会让客户端**完全不可用**；`buffer_size: 0` 抛异常；`read_timeout: 0` 恢复无限阻塞 | Medium | 重定向上限判断移入重定向分支；`ServerConfig` 校验正数/非负 |
| V-6 | `shutdown()` 后 `sendMessage` 把 `RejectedExecutionException` 抛给调用方 | Low-Med | 捕获并转交 `onError` |
| V-7 | 「解密失败不再静默」只是多打一行日志，仍以成功返回 | Low | 修正 javadoc，如实描述回退行为 |
| V-8 | 密文启发式用「最小长度」，任何够长的 Base64 明文都会被误判 | Low | 改为**精确等于**公钥模长的 Base64 长度（2048 位 → 344） |
| V-9 | 显式指定的配置路径不存在时静默放弃整条查找链 | Low | 改为告警并继续沿默认链查找 |
| V-10 | `DeviceInfo.equals/hashCode` 走 Gson 序列化，约 39µs/次且对可变对象不安全 | Low | 记录为已知权衡（无仓库内消费方作为 map key） |
| V-11 | DNS 报文头部读取在边界保护块之外（当前不可达） | Low | 一并纳入 `requireRemaining` 保护 |
| V-12 | 「清理未用 import / 改用 SecureRandom」只做了一半 | Low | 清掉 `JsonDataProcessor`、`RequestDataMap` 的死 import；`generateRandomNumber` 同样改用 `SecureRandom` |

复核同时**证伪**了两处我原先记录的判断，已在文中订正：

- H-3 中「同一消息两次签名相同即可伪造」**不成立**：PKCS#1 v1.5 签名本就是确定性的，
  伪造仍需私钥。真正的缺陷是没有摘要、语义不是签名、且没有布尔返回的验签。
- `HttpClient` 的「catch 里 disconnect」**不会**误断开已交付给调用方的流：
  成功路径通过 `return` 离开 try，只有异常路径才进入 catch。

复核未能验证的部分（已如实记录）：对端真实线格式（gzip/RSA/枚举名或数值）、
`HttpsDnsClient.resolve()` 的端到端 TLS、以及真实 TCP 服务器能否跑通。

### 复核后的最终状态

```
mvn clean test   -> Tests run: 36, Failures: 0, Errors: 0
mvn package      -> BUILD SUCCESS；jar 内含 config.json，target/lib 仅含运行时依赖
java -jar ... test（任意工作目录）-> 18/18 通过，退出码 0
java -jar ... zzz                -> 退出码 1
-P java11 / -P java17 / default  -> 字节码 major 55 / 61 / 52（release 守卫生效）
mvn dependency:analyze           -> 无 unused / undeclared
```


