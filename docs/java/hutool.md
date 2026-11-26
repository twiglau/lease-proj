# Hutool 工具库使用说明

> 记录时间：2026-11-26
> 项目环境：JDK 17、Spring Boot 3.0.11、hutool（`hutool-all`）
> 依赖位置：`web/web-admin/pom.xml`

## 一、依赖与定位

```xml
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
</dependency>
```

Hutool 是国产的 Java 工具库，目标是把 JDK 中常见的样板代码收成一行调用。

`hutool-all` 是**全量聚合包**，内部包含多个模块：

| 模块 | 能力 |
|---|---|
| `hutool-core` | 字符串、集合、Bean、日期、类型转换、ID、随机数 |
| `hutool-crypto` | MD5、SHA、AES、RSA、Base64 |
| `hutool-json` | JSON 序列化/反序列化 |
| `hutool-http` | 简易 HTTP 客户端 |
| `hutool-poi` | Excel / Word 读写 |
| `hutool-captcha` | 图形/行为验证码 |
| `hutool-extra` | 邮件、二维码、模板引擎等 |

只用到部分能力时，可以改为按模块引入（如 `hutool-core`、`hutool-crypto`），减少依赖体积。

## 二、本项目中的实际用法

### 2.1 BeanUtil：对象拷贝

用于实体与 VO 之间的属性复制：

```java
AdminVo vo = BeanUtil.copyProperties(entity, AdminVo.class);
```

出现在：

- `SystemUserServiceImpl`
- `RoomInfoServiceImpl`
- `LeaseAgreementServiceImpl`

注意参数顺序：Hutool 是 `(源对象, 目标Class)`，与 Spring 的 `BeanUtils.copyProperties(源对象, 目标对象)` **相反**，混用时极易写错。

### 2.2 DateUtil：日期处理

```java
String day = DateUtil.format(new Date(), "yyyMMdd");
```

用于生成上传文件的日期目录。常用方法：

```java
Date now = DateUtil.date();
String s = DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss");
Date d = DateUtil.parse("2026-11-26 10:30:00");
long days = DateUtil.betweenDay(start, end, true);
Date begin = DateUtil.beginOfDay(new Date());
```

### 2.3 IdUtil：生成 ID

```java
String fileName = DateUtil.format(new Date(), "yyyMMdd")
        + "/"
        + IdUtil.simpleUUID()
        + "-"
        + file.getOriginalFilename();
```

常用方法：

```java
String uuid = IdUtil.simpleUUID();                 // 32 位无横线 UUID
String uuid2 = IdUtil.randomUUID();                // 带横线 UUID
String id = IdUtil.getSnowflakeNextIdStr();        // 雪花 ID
String objectId = IdUtil.objectId();               // MongoDB 风格 ID
```

### 2.4 SecureUtil：摘要与加密

```java
String md5 = SecureUtil.md5(password);
```

用于登录密码校验。常用方法：

```java
String md5 = SecureUtil.md5("123456");
String sha256 = SecureUtil.sha256("123456");
String base64 = SecureUtil.base64Encode("abc");
String aes = SecureUtil.aes(key).encryptHex("明文");
```

## 三、其他高频场景

### 3.1 字符串

```java
StrUtil.isBlank(name);                     // null、""、"  " 都算空
StrUtil.isNotBlank(name);
StrUtil.format("用户 {} 不存在", userId);   // 占位符替换
StrUtil.subBetween(str, "[", "]");
StrUtil.cleanBlank(str);
```

### 3.2 集合

```java
CollUtil.isEmpty(list);
CollUtil.getFirst(list);
CollUtil.newArrayList("a", "b");
CollUtil.join(list, ",");
```

### 3.3 类型转换

```java
int n = Convert.toInt(str, 0);              // 失败返回默认值
long id = Convert.toLong("123");
String s = Convert.toStr(null, "");
List<Integer> list = Convert.toList(Integer.class, array);
```

相比 `Integer.parseInt`，`Convert` 不抛异常、可给默认值，适合处理外部输入。

### 3.4 对象与 Map

```java
ObjectUtil.isEmpty(obj);
ObjectUtil.defaultIfNull(value, "");
MapUtil.of("k1", "v1", "k2", "v2");
MapUtil.getStr(map, "name", "");
```

### 3.5 随机数（验证码、临时密码）

```java
String code = RandomUtil.randomString(6);
int num = RandomUtil.randomInt(1000, 9999);
String nums = RandomUtil.randomNumbers(4);
```

### 3.6 文件与网络

```java
String content = FileUtil.readUtf8String(file);
FileUtil.writeUtf8String("hello", "a.txt");
String body = HttpUtil.get("https://example.com");
```

## 四、使用注意事项

1. **`hutool-all` 体积较大**
   会带上 POI、HTTP 客户端、二维码等模块。对启动速度和包体积敏感时，改为按模块依赖。
   验证码场景其实只需要 `hutool-captcha`，但本项目为了简化用的是全量包。

2. **`SecureUtil.md5` 不适合存密码**
   MD5 无盐、可被彩虹表反查。教程用它是为了简化；生产环境应使用 BCrypt（`BCryptPasswordEncoder`）或 Argon2，并加盐。

3. **与 Spring 的 `BeanUtils` 参数顺序相反**

   ```java
   BeanUtil.copyProperties(entity, Vo.class);     // Hutool
   BeanUtils.copyProperties(entity, vo);          // Spring
   ```

   两者同名不同参，代码里同时存在时务必看清 import。

4. **不要用它替代框架能力**
   例如 JSON 序列化，项目统一用 Jackson（Spring MVC 默认），不要再混用 `JSONUtil`，否则日期格式、枚举处理会不一致。

5. **注意版本**
   父 pom 管理 `hutool.version`，不同大版本 API 有差异（如 5.x 与 6.x 包名变化），升级前先看变更说明。

## 五、与其他工具库的简单对比

| 需求 | Hutool | 其他常见选择 |
|---|---|---|
| 字符串/集合判空 | `StrUtil`、`CollUtil` | Apache Commons Lang `StringUtils`、Guava |
| Bean 拷贝 | `BeanUtil` | Spring `BeanUtils`、MapStruct（编译期生成，性能更好） |
| 日期 | `DateUtil` | JDK 8 `java.time`、Joda-Time |
| 摘要加密 | `SecureUtil` | Spring Security `PasswordEncoder`、BouncyCastle |
| Excel | `ExcelUtil` | EasyExcel、Apache POI |

学习项目用 Hutool 能明显减少样板代码；正式项目里，Bean 拷贝建议用 MapStruct，密码用 BCrypt，JSON 与框架保持一致。
