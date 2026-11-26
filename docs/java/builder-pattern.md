# Builder 模式：MinIO SDK 里的 `XXXArgs.builder()`

> 记录时间：2026-11-26
> 项目环境：JDK 17、Spring Boot 3.0.11、minio-java 8.5.17
> 代码位置：`web/web-admin/.../service/impl/FileServiceImpl.java`

## 一、原始代码

```java
minioClient.putObject(PutObjectArgs.builder()
        .bucket(minioProperties.getBucketName())
        .object(fileName)
        .stream(file.getInputStream(), file.getSize(), -1)
        .contentType(file.getContentType())
        .build());

minioClient.makeBucket(MakeBucketArgs.builder()
        .bucket(minioProperties.getBucketName())
        .build());
```

`PutObjectArgs.builder()` 和 `MakeBucketArgs.builder()` 是 **建造者模式（Builder Pattern）**。`builder()` 返回的不是最终参数对象，而是一个「参数装配器」。

## 二、builder() 创建了什么

以 `PutObjectArgs` 为例，SDK 内部结构大致如下（简化版）：

```java
public class PutObjectArgs extends ObjectWriteArgs {

    // 1. 静态工厂方法：返回一个空的 Builder
    public static Builder builder() {
        return new Builder();
    }

    // 2. 内部静态类：负责收集参数
    public static final class Builder extends ObjectWriteArgs.Builder<Builder, PutObjectArgs> {

        public Builder stream(InputStream stream, long objectSize, long partSize) {
            this.stream = stream;
            this.objectSize = objectSize;
            this.partSize = partSize;
            return this;              // 返回 this，才能链式调用
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        // 3. 校验并生成不可变的 PutObjectArgs
        public PutObjectArgs build() {
            validate();               // 必填项、bucket 命名、partSize 等校验
            return new PutObjectArgs(this);
        }
    }
}
```

调用过程拆解：

| 步骤 | 得到的对象 | 状态 |
|---|---|---|
| `PutObjectArgs.builder()` | `Builder` 实例 | 所有字段为 `null` |
| `.bucket("lease")` | 同一个 `Builder` | `bucket` 被赋值 |
| `.object("a.txt")` | 同一个 `Builder` | `object` 被赋值 |
| `.stream(in, size, -1)` | 同一个 `Builder` | 流和大小被赋值 |
| `.build()` | `PutObjectArgs` | 校验通过后的只读参数对象 |

真正传给 `minioClient.putObject(...)` 的，是最后 `build()` 出来的 `PutObjectArgs`，不是 `Builder`。

## 三、为什么要这么设计

传统构造方法写法：

```java
new PutObjectArgs("lease", "a.txt", stream, size, -1, null, null, null);
```

问题很明显：参数顺序记不住、可读性差、可选参数多时无法维护。Builder 的收益：

1. **参数有名字**
   `.bucket(...)`、`.contentType(...)` 自解释，不用背参数位置。

2. **可选参数按需设置**
   `PutObjectArgs` 有十几个可选字段，构造方法不可能为每种组合写一个重载（telescoping constructor 问题）。

3. **延迟校验**
   必填项、bucket 命名规则、分片大小等统一在 `build()`（或 setter）里校验，构造出来的对象一定合法。
   例如 `bucket name '${spring.apllication.name}' does not follow Amazon S3 standards` 就是 `MakeBucketArgs.Builder.bucket()` 设置时抛出的校验异常。

4. **不可变对象**
   `build()` 生成的是只读快照，可以安全传递、复用，不会被后续修改影响。

## 四、PutObjectArgs 与 MakeBucketArgs 的关系

- `PutObjectArgs`：上传对象参数，字段包括 bucket、object、stream、contentType、headers 等。
- `MakeBucketArgs`：创建桶参数，只有 bucket 是必要字段，所以链式调用很短：

```java
MakeBucketArgs.builder()
        .bucket("lease")
        .build();
```

- 两者都继承 `BucketArgs`，`bucket()` 和桶名校验定义在父类的 Builder 中，因此行为一致。
- 同类还有 `BucketExistsArgs`、`SetBucketPolicyArgs`、`GetObjectArgs` 等，用法完全一样。

## 五、手写一个最小示例

便于理解模式本身：

```java
public class Order {
    private final long id;
    private final int amount;
    private final String remark;

    private Order(Builder builder) {
        this.id = builder.id;
        this.amount = builder.amount;
        this.remark = builder.remark;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private long id;
        private int amount;
        private String remark;

        public Builder id(long id) {
            this.id = id;
            return this;
        }

        public Builder amount(int amount) {
            this.amount = amount;
            return this;
        }

        public Builder remark(String remark) {
            this.remark = remark;
            return this;
        }

        public Order build() {
            if (amount <= 0) {
                throw new IllegalArgumentException("amount 必须大于 0");
            }
            return new Order(this);
        }
    }
}
```

使用：

```java
Order order = Order.builder()
        .id(1L)
        .amount(100)
        .remark("测试")
        .build();
```

## 六、与 Lombok @Builder 的关系

Lombok 的 `@Builder` 会自动生成上面这套样板代码：

```java
@Builder
public class Order {
    private final long id;
    private final int amount;
    private final String remark;
}
```

编译后等价于手写 `builder()`、`Builder` 内部类、链式 setter 和 `build()`。

区别：

- Lombok 生成的方法名就是字段名（`.id(...)`），不额外做业务校验，除非自己写 `@Builder` + 自定义构造/`@PrePersist`；
- MinIO 是手写 Builder，可以在 `bucket()`、`build()` 里插入命名规则等校验。

## 七、使用要点

1. **Builder 不是线程安全的**，一次装配完立即 `build()`，不要跨线程共享。
2. **必须调用 `build()`**，`builder()` 只是拿到装配器；把 Builder 直接传给方法编译都过不了。
3. **`build()` 之后不要再改 Builder**，最终对象持有的是值的拷贝，Builder 后续修改不会影响已构建对象。
4. **校验可能发生在 setter，也可能发生在 build()**，所以异常栈要看清是哪个阶段抛的。
5. 这不是 MinIO 独有写法：`StringBuilder`、`Stream.builder()`、Spring 的 `UriComponentsBuilder`、Guava 的 `ImmutableList.builder()` 都是同一模式。
