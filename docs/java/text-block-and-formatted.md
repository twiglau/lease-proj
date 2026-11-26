# Java 文本块（Text Block）与 String.formatted()

> 记录时间：2026-11-26
> 项目环境：JDK 17（`java.version=17`）、Spring Boot 3.0.11
> 代码位置：`web/web-admin/.../service/impl/FileServiceImpl.java#initBucket()`

## 一、原始代码

```java
minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
        .bucket(minioProperties.getBucketName())
        .config("""
                {
                  "Statement" : [ {
                    "Action" : "s3:GetObject",
                    "Effect" : "Allow",
                    "Principal" : "*",
                    "Resource" : "arn:aws:s3:::%s/*"
                  } ],
                  "Version" : "2012-10-17"
                }
                """.formatted(minioProperties.getBucketName()))
        .build());
```

这段代码由两部分组成：

- `""" ... """`：Java 的**文本块**，用来书写多行字符串；
- `.formatted(...)`：Java 的**字符串格式化方法**，把 `%s` 替换成实际参数。

## 二、版本要求

| 特性 | 引入版本 | 说明 |
|---|---|---|
| 文本块（Text Blocks） | JDK 15 正式（JDK 13/14 为预览） | JEP 378 |
| `String.formatted(Object...)` | JDK 15 | 等价于 `String.format(this, args)` |
| `String.format` | JDK 5 | 老 API，一直可用 |

本项目 `java.version=17`，两个特性都能直接用。如果降到 JDK 8/11，这段代码**编译不通过**，必须改回字符串拼接或 `String.format`。

## 三、文本块语法

### 3.1 基本规则

- 以 `"""` 开始，**开头三引号后必须换行**，内容从下一行开始；
- 以 `"""` 结束，可以单独占一行，也可以紧跟在最后一行内容后面；
- 内容里的双引号 `"` **不需要转义**，换行也**不需要写 `\n`**；
- 编译器会自动去掉每行的公共前导缩进（incidental whitespace）。

因此可以直接把 JSON 原样贴进去，这是文本块最主要的用途。

### 3.2 缩进如何被去掉

```java
String s = """
        {
          "a": 1
        }
        """;
```

每行都有 8 个空格的公共缩进，编译器会把它们整体去掉，实际内容是：

```text
{
  "a": 1
}
```

相对缩进（`"a"` 前的 2 个空格）会保留，所以 JSON 的层次不会乱。

### 3.3 结尾换行

```java
// 结束三引号单独占一行：内容末尾带一个换行符
String a = """
        line1
        line2
        """;   // 结果："line1\nline2\n"

// 结束三引号紧贴最后一行：内容末尾没有换行符
String b = """
        line1
        line2"""; // 结果："line1\nline2"
```

MinIO 的桶策略是 JSON，多一个结尾换行不影响解析，但如果是签名、摘要计算等场景，这个差异会导致结果不一致。

### 3.4 文本块里的转义

文本块里仍然可以使用转义字符：

| 写法 | 含义 |
|---|---|
| `\n` | 换行（一般不需要，直接回车即可） |
| `\"` | 双引号（一般不需要，直接写 `"`） |
| `\\` | 反斜杠本身 |
| `\s` | 一个空格，常用于保留行尾空格 |
| 行尾 `\` | 取消该行换行，把下一行接上来 |

注意：行尾空格默认会被去掉，需要保留时用 `\s`。

## 四、`.formatted()` 与 `%s`

`String.formatted(Object... args)` 是实例方法，内部就是 `String.format(this, args)`：

```java
"Hello, %s".formatted("Tom");        // "Hello, Tom"
"%s - %s".formatted("a", "b");       // "a - b"
```

上面代码里的 `%s` 会被 `minioProperties.getBucketName()` 替换。假设桶名是 `lease`，最终传给 MinIO 的字符串是：

```json
{
  "Statement" : [ {
    "Action" : "s3:GetObject",
    "Effect" : "Allow",
    "Principal" : "*",
    "Resource" : "arn:aws:s3:::lease/*"
  } ],
  "Version" : "2012-10-17"
}
```

常用格式符：

| 格式符 | 含义 |
|---|---|
| `%s` | 字符串（任意对象，调用 `toString()`） |
| `%d` | 整数 |
| `%f` | 浮点数 |
| `%b` | 布尔值 |
| `%%` | 输出一个百分号 `%` |
| `%1$s` | 重复使用第 1 个参数 |

## 五、这段代码在业务上做什么

MinIO 的**桶策略（Bucket Policy）**，含义是：

- `Action: s3:GetObject`：允许读取对象；
- `Effect: Allow` + `Principal: "*"`：任何人（含匿名用户）都允许；
- `Resource: arn:aws:s3:::lease/*`：作用于该桶下的全部对象。

也就是把桶设成**公共读**，上传后的图片可以直接通过 URL 访问，不需要签名。

需要注意：这段逻辑位于 `if (!bucketExists)` 内，只有**桶不存在、刚创建时**才设置策略。如果桶已存在但策略缺失，这段代码不会补设，需要手动在 MinIO 控制台配置，或把 `setBucketPolicy` 移到判断之外。

## 六、常见坑

1. **字面量 `%` 必须写成 `%%`**
   文本块参与 `formatted` 时，`%` 是格式符的起始字符。JSON 里如果出现 `%20`、`100%` 这类内容，会抛 `UnknownFormatConversionException`。

2. **参数个数必须匹配**
   占位符比参数多，抛 `MissingFormatArgumentException`；参数比占位符多不会报错，但多余参数被忽略，属于隐性 bug。

3. **`%s` 不做 JSON 转义**
   它只是原样插入字符串。这里 bucket 名受 S3 命名规则限制（小写字母、数字、连字符），所以安全；如果换成用户可控内容拼接 JSON，需要考虑 JSON 注入。

4. **文本块是编译期常量**
   拼接的仍是一个普通 `String`，不是模板引擎，不支持循环、条件。复杂 JSON 建议用 Jackson 序列化对象，避免手写字符串。

5. **缩进以「最少缩进的行」为准**
   如果把某一行写到了行首（0 缩进），其他行的缩进就不会被去掉，最终字符串会带上多余空格。IDE 的格式化一般会处理，但手工编辑时要注意。

## 七、等价的传统写法

便于对比理解：

```java
// 字符串拼接
String policy = "{\n" +
        "  \"Statement\" : [ {\n" +
        "    \"Action\" : \"s3:GetObject\",\n" +
        "    \"Effect\" : \"Allow\",\n" +
        "    \"Principal\" : \"*\",\n" +
        "    \"Resource\" : \"arn:aws:s3:::" + bucketName + "/*\"\n" +
        "  } ],\n" +
        "  \"Version\" : \"2012-10-17\"\n" +
        "}";

// String.format + 普通字符串
String policy2 = String.format("{\n  \"Resource\" : \"arn:aws:s3:::%s/*\"\n}", bucketName);
```

三种写法结果一致，文本块胜在可读性和可维护性。
