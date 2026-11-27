# JPA / Hibernate：实体声明索引与建表

> 记录时间：2026-11-27
> 项目环境：JDK 17、Spring Boot 3.0.11（jakarta 命名空间）
> 说明：本项目使用 MyBatis-Plus，本文用于对比与学习；两者不要在同一模块混用

## 一、JPA 与 Hibernate 的关系

- **JPA（Jakarta Persistence API）**：规范，只定义注解和接口（`@Entity`、`@Table`、`@Id`、`EntityManager`）。
- **Hibernate**：JPA 的主流实现，负责把实体映射成 SQL，并提供建表、索引生成等扩展能力。
- Spring Boot 的 `spring-boot-starter-data-jpa` 默认整合的是 Hibernate。

所以写代码时 import 的是 `jakarta.persistence.*`，运行时干活的是 Hibernate。

## 二、依赖（仅在使用 JPA 的模块添加）

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

## 三、在实体上声明索引

JPA 2.1 起支持在 `@Table` 里声明索引：

```java
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(
        name = "room_info",
        indexes = {
                // 普通复合索引
                @Index(name = "idx_room_apartment",
                        columnList = "apartment_id, is_deleted, is_release"),
                // 唯一索引
                @Index(name = "uk_room_no", columnList = "apartment_id, room_number", unique = true)
        }
)
public class RoomInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "apartment_id", nullable = false)
    private Long apartmentId;

    @Column(name = "room_number", nullable = false, length = 32)
    private String roomNumber;

    @Column(name = "is_deleted", nullable = false)
    private Integer isDeleted;

    @Column(name = "is_release", nullable = false)
    private Integer isRelease;
}
```

要点：

- `@Index` 只能写在 **`@Table` 的 `indexes` 属性**里，不能直接标在字段上；
- `columnList` 写**数据库列名**（与 `@Column(name=...)` 一致），多个列用逗号分隔；
- `unique = true` 生成唯一索引，等价于 `@Table(uniqueConstraints = ...)` 的简化写法；
- 索引名在**同一张表内**要唯一，建议统一 `idx_表_列` / `uk_表_列`。

单列唯一也可以用字段级注解：

```java
@Column(name = "phone", unique = true, nullable = false)
private String phone;
```

复合唯一约束的另一种写法：

```java
@Table(
        name = "room_info",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_room_no",
                columnNames = {"apartment_id", "room_number"})
)
```

## 四、让 Hibernate 生成 DDL

### 4.1 ddl-auto 的取值

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

| 值 | 行为 |
|---|---|
| `none` | 不做任何结构操作（生产推荐） |
| `validate` | 启动时校验实体与表是否匹配，不匹配报错 |
| `update` | 尽量补齐缺失的表、列、索引，但不删除、不改名 |
| `create` | 每次启动删表重建（数据丢失） |
| `create-drop` | 启动建表，关闭时删表（测试用） |

### 4.2 执行时机与默认值

「自动建表」不是编译期行为，而是**运行期的启动阶段**：Spring Boot 创建 `EntityManagerFactory` → Hibernate 初始化 `SessionFactory` → 按 `ddl-auto` 生成并执行 DDL。所以：

- 配置了 `update` / `create`，应用一启动就会操作数据库结构；
- 配置了 `none`，启动时完全不碰结构，实体注解里的 `@Index` 只是元数据，**不会产生任何 DDL**。

Spring Boot 的默认值容易误会：

| 数据库 | 未配置 `ddl-auto` 时的默认值 |
|---|---|
| H2 等嵌入式数据库 | `create-drop`，所以「一运行就有表」 |
| MySQL 等外部数据库 | `none`，**不会自动建表** |

也就是说，开发环境连 MySQL 时，必须显式写 `ddl-auto: update` 才会自动补表、补索引；默认情况下第一次启动仍然会报表不存在。

### 4.3 导出建表 SQL（推荐做法）

不直接让 Hibernate 改库，而是把 DDL 导出成脚本，交给 Flyway/Liquibase 或 DBA 执行：

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: none
    properties:
      jakarta.persistence.schema-generation.scripts.action: create
      jakarta.persistence.schema-generation.scripts.create-target: target/schema.sql
```

启动后会在 `target/schema.sql` 看到类似：

```sql
create table room_info (
    id bigint not null auto_increment,
    apartment_id bigint not null,
    room_number varchar(32) not null,
    is_deleted integer not null,
    is_release integer not null,
    primary key (id)
) engine = InnoDB;

create index idx_room_apartment
    on room_info (apartment_id, is_deleted, is_release);

alter table room_info
    add constraint uk_room_no unique (apartment_id, room_number);
```

这份脚本可以作为迁移脚本的初稿，再按项目规范调整。

### 4.4 开发环境与生产环境的差异

**开发环境**

```yaml
# application-dev.yml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

方便快速迭代：加字段、加表、加索引起动即生效。但要有心理准备：`update` 只加不删、不改类型、不改名，改多了容易留下历史包袱，开发库可以随时重建。

**生产环境**

```yaml
# application-prod.yml
spring:
  jpa:
    hibernate:
      ddl-auto: validate   # 或 none
  flyway:
    enabled: true
    clean-disabled: true
```

- DDL 由 Flyway / Liquibase 执行，每次变更都有版本号和执行记录；
- Hibernate 最多用 `validate` 在启动时校验实体与表是否一致；
- 应用使用的数据库账号**不授予 DDL 权限**，建表、改表交给迁移流程或 DBA；
- Spring Boot 保证 Flyway/Liquibase 在 Hibernate 初始化之前执行，因此「脚本建表 → Hibernate 校验」可以共存。

**两套配置的取舍**

| 环境 | ddl-auto | DDL 来源 |
|---|---|---|
| 本地开发 | `update` 或 `none` + 导出脚本 | Hibernate 或手工脚本 |
| 测试环境 | `none` / `validate` | Flyway / Liquibase |
| 生产环境 | `none` / `validate` | Flyway / Liquibase / DBA |

结论：**自动建表是开发期的便利，不是生产的部署方式**。生产靠迁移脚本，实体注解只作为结构声明和文档。

## 五、命名策略：驼峰如何变成下划线

Hibernate 默认使用 Spring Boot 的 `CamelCaseToUnderscoresNamingStrategy`：

```text
apartmentId  → apartment_id
isDeleted    → is_deleted
```

因此 `@Index(columnList = "apartment_id, is_deleted")` 能对上列名。如果自定义了命名策略或显式写了 `@Column(name=...)`，以最终物理列名为准。

## 六、为什么不建议生产使用 `ddl-auto: update`

1. **不确定**：只加不删，字段改名会新建列而不是重命名，旧列残留；
2. **无版本记录**：不知道哪个环境执行到哪一步；
3. **索引不可靠**：表已存在时，某些索引变更不一定生效；
4. **多实例竞争**：多个节点同时启动可能并发执行 DDL；
5. **不可回滚**：出问题没有对应的补偿脚本。

正确姿势：`ddl-auto: none` + Flyway/Liquibase 管理 DDL，实体注解只作为结构声明和文档。

## 七、与 MyBatis-Plus 的对比

| 能力 | JPA / Hibernate | MyBatis-Plus |
|---|---|---|
| 实体注解 | `@Entity`、`@Table(indexes=...)` | `@TableName`、`@TableId`、`@TableField` |
| 索引声明 | 支持 `@Index`、`@UniqueConstraint` | **不支持**，只能写 DDL |
| 自动建表 | 支持 `ddl-auto` / 导出脚本 | 不支持 |
| SQL 控制 | 自动生成，复杂 SQL 用 JPQL/原生 SQL | 手写 SQL/XML，完全可控 |
| 本项目选择 | 未使用 | 使用中 |

这也是之前结论的来源：**MyBatis-Plus 的实体加不了索引，索引必须落到数据库 DDL 或迁移脚本**；JPA 能加，但也只影响 DDL 生成，运行时不会自动同步。

## 八、验证

```sql
-- 查看索引是否按预期生成
SHOW INDEX FROM room_info;

-- 查看建表语句中的索引与唯一约束
SHOW CREATE TABLE room_info;

-- 校验执行计划
EXPLAIN SELECT *
FROM room_info
WHERE apartment_id = 1 AND is_deleted = 0 AND is_release = 1;
```

## 九、常见坑

1. **`columnList` 写成 Java 属性名**：如果命名策略把 `apartmentId` 映射成 `apartment_id`，写 `apartmentId` 生成的 DDL 列名可能对不上；
2. **`@Index` 标在字段上**：JPA 不支持，编译不报错但不会生效；Hibernate 私有的 `@org.hibernate.annotations.Index` 已废弃；
3. **索引名重复**：MySQL 的索引名在表内唯一，多张表可同名，但排查时容易混淆，建议带表名前缀；
4. **`ddl-auto: update` 不会删除旧索引**：改索引名会留下两个索引；
5. **JPA 与 MyBatis-Plus 混用**：两套映射、事务、懒加载机制并存，学习项目里没有收益，不要为了「能加索引」引入 JPA。

## 十、结论

- 想用注解声明索引 → 只能选 JPA/Hibernate；
- 本项目用 MyBatis-Plus → 索引写在 `docs/mysql/indexes.sql` 或迁移脚本里；
- 无论哪种 ORM，生产环境的 DDL 都应该由 Flyway/Liquibase 这类工具管理，`ddl-auto` 只用于本地开发和生成初稿。
