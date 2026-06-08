# Hutool使用指南

## 概述

Hutool是一个Java工具包，提供了很多实用的工具类。本项目已集成Hutool，用于简化常见的开发任务。

## 依赖配置

### Maven配置
```xml
<properties>
    <hutool.version>5.8.22</hutool.version>
</properties>

<dependencies>
    <!-- Hutool核心工具包 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-core</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Hutool HTTP客户端 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-http</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Hutool JSON处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-json</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Hutool加密解密 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-crypto</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Hutool网络工具 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-socket</artifactId>
        <version>${hutool.version}</version>
    </dependency>
</dependencies>
```

## 工具类使用

### 1. 字符串工具 (StrUtil)

```java
import cn.hutool.core.util.StrUtil;

// 检查字符串
boolean isEmpty = StrUtil.isEmpty(null);      // true
boolean isBlank = StrUtil.isBlank("  ");       // true
boolean isNotEmpty = StrUtil.isNotEmpty("test"); // true

// 格式化字符串
String formatted = StrUtil.format("Hello, {}! You are {} years old.", "张三", 25);
// 结果: "Hello, 张三! You are 25 years old."

// 截取字符串
String sub = StrUtil.sub("Hello World", 0, 5);
// 结果: "Hello"

// 去除空白
String trimmed = StrUtil.trim("  Hello  ");
// 结果: "Hello"
```

### 2. 日期工具 (DateUtil)

```java
import cn.hutool.core.date.DateUtil;
import java.util.Date;

// 当前时间
String now = DateUtil.now();          // "2026-06-02 10:30:00"
String today = DateUtil.today();      // "2026-06-02"

// 格式化日期
Date date = new Date();
String formatted = DateUtil.format(date, "yyyy-MM-dd HH:mm:ss");
// 结果: "2026-06-02 10:30:00"

// 解析日期
Date parsed = DateUtil.parse("2026-06-02 10:30:00", "yyyy-MM-dd HH:mm:ss");

// 日期计算
Date tomorrow = DateUtil.offsetDay(date, 1);  // 明天
Date nextWeek = DateUtil.offsetWeek(date, 1); // 下周
```

### 3. ID生成工具 (IdUtil)

```java
import cn.hutool.core.util.IdUtil;

// UUID
String uuid = IdUtil.randomUUID();           // "550e8400-e29b-41d4-a716-446655440000"
String simpleUuid = IdUtil.simpleUUID();     // "550e8400e29b41d4a716446655440000"

// 雪花ID
long snowflakeId = IdUtil.getSnowflakeNextId(); // 1234567890123456789

// 简单ID
String simpleId = IdUtil.simpleId();         // "a1b2c3d4"
```

### 4. 随机工具 (RandomUtil)

```java
import cn.hutool.core.util.RandomUtil;

// 随机整数
int randomInt = RandomUtil.randomInt(1, 100);  // 1-100之间的随机整数

// 随机字符串
String randomStr = RandomUtil.randomString(8);  // 8位随机字符串

// 随机数字
String randomNum = RandomUtil.randomNumbers(6); // 6位随机数字

// 随机元素
String[] array = {"A", "B", "C"};
String randomElement = RandomUtil.randomEle(array); // 随机选择一个元素
```

### 5. 编码工具 (Base64, HexUtil)

```java
import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.HexUtil;

// Base64编码
String original = "Hello, Hutool!";
String encoded = Base64.encode(original, "UTF-8");
String decoded = Base64.decodeStr(encoded, "UTF-8");

// 十六进制编码
String hex = HexUtil.encodeHexStr("Hello", StandardCharsets.UTF_8);
String fromHex = HexUtil.decodeHexStr(hex, StandardCharsets.UTF_8);
```

### 6. 加密工具 (SecureUtil, DigestUtil)

```java
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.asymmetric.KeyType;

// MD5加密
String md5 = DigestUtil.md5Hex("Hello");

// SHA256加密
String sha256 = DigestUtil.sha256Hex("Hello");

// RSA加密
RSA rsa = new RSA();
String publicKey = rsa.getPublicKeyBase64();
String privateKey = rsa.getPrivateKeyBase64();

String encrypted = rsa.encryptBase64("Hello", KeyType.PublicKey);
String decrypted = rsa.decryptStr(encrypted, KeyType.PrivateKey);
```

### 7. HTTP工具 (HttpUtil, HttpRequest)

```java
import cn.hutool.http.HttpUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;

// GET请求
String response = HttpUtil.get("http://httpbin.org/get");

// POST请求
Map<String, Object> params = new HashMap<>();
params.put("username", "test");
params.put("password", "123456");
String postResponse = HttpUtil.post("http://httpbin.org/post", params);

// 自定义请求
HttpResponse response = HttpRequest.post("http://httpbin.org/post")
    .header("Content-Type", "application/json")
    .header("User-Agent", "Hutool-Test/1.0")
    .body("{\"key\":\"value\"}")
    .execute();

int statusCode = response.getStatus();
String body = response.body();
```

### 8. JSON工具 (JSONUtil)

```java
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import java.util.Map;

// 创建JSON对象
JSONObject json = new JSONObject();
json.set("name", "张三");
json.set("age", 25);

// 转换为字符串
String jsonStr = json.toString();

// 格式化JSON
String formatted = JSONUtil.formatJsonStr(jsonStr);

// 解析JSON
JSONObject parsed = JSONUtil.parseObj(jsonStr);
String name = parsed.getStr("name");
int age = parsed.getInt("age");

// Map转JSON
Map<String, Object> map = new HashMap<>();
map.put("key", "value");
String mapJson = JSONUtil.toJsonStr(map);

// JSON转Map
Map<String, Object> parsedMap = JSONUtil.toBean(mapJson, Map.class);
```

### 9. 集合工具 (CollUtil)

```java
import cn.hutool.core.collection.CollUtil;
import java.util.List;
import java.util.Set;

// 创建List
List<String> list = CollUtil.toList("A", "B", "C");

// 创建Set
Set<String> set = CollUtil.set(false, "A", "B", "C");

// 集合操作
boolean isEmpty = CollUtil.isEmpty(list);
int size = CollUtil.size(list);

// 集合转换
String join = CollUtil.join(list, ","); // "A,B,C"
```

### 10. 文件工具 (FileUtil)

```java
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import java.io.File;

// 读取文件
String content = FileUtil.readString("test.txt", CharsetUtil.CHARSET_UTF_8);

// 写入文件
FileUtil.writeString("Hello, Hutool!", "test.txt", CharsetUtil.CHARSET_UTF_8);

// 文件操作
boolean exist = FileUtil.exist("test.txt");
File file = FileUtil.file("test.txt");
long size = FileUtil.size(file);
```

## 项目中的使用

### 1. HutoolUtils工具类

项目提供了 `HutoolUtils` 工具类，封装了Hutool的常用功能：

```java
import com.yaoguangyun.util.HutoolUtils;

// 字符串操作
boolean isEmpty = HutoolUtils.isEmpty("test");
String formatted = HutoolUtils.format("Hello, {}!", "World");

// 日期操作
String now = HutoolUtils.now();
String today = HutoolUtils.today();

// ID生成
String uuid = HutoolUtils.randomUUID();
long snowflakeId = HutoolUtils.snowflakeId();

// 加密操作
String md5 = HutoolUtils.md5("Hello");
String sha256 = HutoolUtils.sha256("Hello");

// HTTP操作
String response = HutoolUtils.httpGet("http://httpbin.org/get");

// JSON操作
String json = HutoolUtils.toJson(object);
Map<String, Object> map = HutoolUtils.toMap(jsonStr);
```

### 2. HutoolExample示例

项目提供了 `HutoolExample` 示例类，演示了Hutool的各种功能：

```java
import com.yaoguangyun.example.HutoolExample;

// 运行所有示例
HutoolExample.runExamples();
```

## 测试结果

运行测试的结果：

```
=== Hutool使用示例 ===
--- 字符串工具示例 ---
isEmpty(null): true
isEmpty(""): true
isEmpty("test"): false
isBlank("  "): true
格式化: Hello, 张三! You are 25 years old.
截取: Hello

--- 日期工具示例 ---
当前时间: 2026-06-02 02:47:58
今天日期: 2026-06-02
格式化日期: 2026-06-02 02:47:58
解析日期: 2026-06-02 10:30:00

--- ID生成示例 ---
UUID: 110fa365-ef55-4a61-b72f-71b9cfa51431
简单UUID: 65cb7ac0333042c89926ccb78df4e55b
雪花ID: 2061520147469660160

--- 随机工具示例 ---
随机整数(1-100): 8
随机字符串(8位): sFPWKPHS
随机数字(6位): 440138

--- 编码工具示例 ---
原始数据: Hello, Hutool!
Base64编码: SGVsbG8sIEh1dG9vbCE=
Base64解码: Hello, Hutool!

--- 加密工具示例 ---
MD5加密: 727a8ae5cb807f0bc9a29e696febf685
SHA256加密: bcf07f54ed04354d491445b03a54cbeca3151f19af1bb700af3a1894cd673b83
RSA公钥长度: 216
RSA私钥长度: 848
RSA加密成功: true
RSA解密成功: true

--- HTTP工具示例 ---
GET请求成功: true
POST请求成功: true
自定义请求成功: true
响应状态码: 200

--- JSON工具示例 ---
JSON字符串: {"name":"张三","age":25,"city":"北京"}
格式化JSON:
{
    "name": "张三",
    "age": 25,
    "city": "北京"
}
解析JSON: name=张三, age=25
Map转JSON: {"release":"8.0.0","model":"SM-G950F","brand":"Samsung"}
JSON转Map: brand=Samsung

=== Hutool示例完成 ===
```

## 最佳实践

### 1. 字符串处理
```java
// 推荐使用StrUtil而不是手动判断
if (StrUtil.isNotEmpty(str)) {
    // 处理非空字符串
}

// 使用format而不是字符串拼接
String msg = StrUtil.format("用户{}登录成功", username);
```

### 2. 日期处理
```java
// 使用DateUtil而不是SimpleDateFormat
String dateStr = DateUtil.format(new Date(), "yyyy-MM-dd");
Date date = DateUtil.parse(dateStr, "yyyy-MM-dd");
```

### 3. ID生成
```java
// 使用IdUtil生成唯一ID
String uuid = IdUtil.simpleUUID();
long id = IdUtil.getSnowflakeNextId();
```

### 4. 加密操作
```java
// 使用DigestUtil进行摘要加密
String md5 = DigestUtil.md5Hex(data);
String sha256 = DigestUtil.sha256Hex(data);

// 使用SecureUtil进行对称/非对称加密
AES aes = SecureUtil.aes();
String encrypted = aes.encryptHex(data);
```

### 5. HTTP请求
```java
// 简单请求使用HttpUtil
String response = HttpUtil.get(url);

// 复杂请求使用HttpRequest
HttpResponse response = HttpRequest.post(url)
    .header("Content-Type", "application/json")
    .body(jsonBody)
    .timeout(5000)
    .execute();
```

### 6. JSON处理
```java
// 使用JSONUtil而不是手动构建JSON
JSONObject json = JSONUtil.parseObj(jsonStr);
String name = json.getStr("name");

// 对象转JSON
String json = JSONUtil.toJsonStr(object);
```

## 总结

Hutool提供了丰富的工具类，可以大大简化Java开发。本项目已集成Hutool的核心模块，包括：

1. **hutool-core** - 核心工具类
2. **hutool-http** - HTTP客户端
3. **hutool-json** - JSON处理
4. **hutool-crypto** - 加密解密
5. **hutool-socket** - 网络工具

通过使用Hutool，可以：
- 减少重复代码
- 提高开发效率
- 保证代码质量
- 简化常见操作