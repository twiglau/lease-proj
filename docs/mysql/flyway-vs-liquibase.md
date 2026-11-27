# Flyway 与 Liquibase：数据库版本管理工具

> 记录时间：2026-11-27
> 项目环境：JDK 17、Spring Boot 3.0.11、MySQL 8.x
> 适用场景：建表、加索引、改字段等 DDL 需要跟随代码版本一起管理

## 一、为什么需要迁移工具

手工执行 SQL 的问题：

- 换机器、换环境容易漏执行；
- 不知道某个库到底执行到哪一版；
- 脚本重复执行报错（MySQL 不支持 `CREATE INDEX IF NOT EXISTS`）；
- 多人协作时脚本顺序混乱。

Flyway 和 Liquibase 解决同一件事：**把数据库变更写成按版本编号的脚本，由工具记录执行历史，只执行没跑过的部分**。两者都会在库里建一张历史表：

| 工具 | 历史表 |
|---|---|
| Flyway | `flyway_schema_history` |
| Liquibase | `DATABASECHANGELOG`、`DATABASECHANGELOGLOCK` |

Spring Boot 对两者都有自动配置：加依赖、放脚本、启动即执行。

## 二、Flyway

### 2.1 特点

- 以**纯 SQL** 为主，学习成本低，适合熟悉 MySQL 的团队；
- 脚本按版本号顺序执行，执行过的记录校验和（checksum），脚本被改动会报错；
- 社区版**不支持自动回滚**（undo），回滚要靠自己写补偿脚本；
- 支持 Java 迁移、可重复执行脚本（`R__` 前缀）。

### 2.2 引入依赖

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<!-- MySQL 8 必须额外引入，Flyway 9 起数据库支持拆成了独立模块 -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

版本由 Spring Boot 父 pom 管理，不需要手写。

### 2.3 配置

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true   # 已有数据的库首次接入时使用
    baseline-version: 1
```

- `locations`：脚本目录，默认 `classpath:db/migration`；
- `baseline-on-migrate`：库里已有表但没有历史表时，先打基线再执行后续脚本，避免首次启动失败。

### 2.4 目录与命名

```text
web/web-admin/src/main/resources/db/migration/
├── V1__init_schema.sql
├── V2__add_apartment_query_indexes.sql
└── R__refresh_views.sql          # 可重复执行脚本（每次校验和变化都执行）
```

命名规则：

```text
V<版本号>__<描述>.sql
```

- `V` 前缀表示版本迁移，版本号可用 `1`、`1.1`、`20261127`；
- **两个下划线** `__`，单下划线会解析失败；
- 描述用英文和下划线，避免空格和中文；
- 已执行过的脚本**不能再改内容**，否则 checksum 校验失败，需要 `flyway repair`。

### 2.5 最小示例

`V1__init_schema.sql`：

```sql
CREATE TABLE demo_apartment
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(64) NOT NULL,
    is_deleted TINYINT     NOT NULL DEFAULT 0
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
```

`V2__add_apartment_query_indexes.sql`：

```sql
CREATE INDEX idx_room_apartment
    ON room_info (apartment_id, is_deleted, is_release);

CREATE INDEX idx_agreement_apartment
    ON lease_agreement (apartment_id, is_deleted, status);
```

启动应用后日志会出现：

```text
Migrating schema `lease` to version "1 - init schema"
Migrating schema `lease` to version "2 - add apartment query indexes"
Successfully applied 2 migrations to schema `lease`
```

历史表内容：

```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

### 2.6 常用操作

```bash
# 查看状态（需要 flyway-maven-plugin 或命令行工具）
mvn flyway:info

# 校验历史与脚本是否一致
mvn flyway:validate

# 修复历史表（脚本被误改、执行中断时）
mvn flyway:repair
```

生产环境一般只允许 `migrate`，不要随手 `clean`（会删库中对象）。

## 三、Liquibase

### 3.1 特点

- 用 **changeset** 描述变更，支持 XML、YAML、JSON、SQL 多种格式；
- 每个 changeset 有 `id + author` 唯一标识，并记录校验和；
- **原生支持 rollback**，可以给每个 changeset 写回滚逻辑；
- 与数据库无关的 DSL（`createTable`、`addColumn`、`createIndex`），跨库迁移友好；
- 配置和概念比 Flyway 多一些，学习成本略高。

### 3.2 引入依赖

```xml
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
</dependency>
```

版本同样由 Spring Boot 管理。

### 3.3 配置

```yaml
spring:
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.yaml
```

### 3.4 目录结构

```text
web/web-admin/src/main/resources/db/changelog/
├── db.changelog-master.yaml
├── changes/
│   ├── 001-init-schema.yaml
│   └── 002-add-indexes.yaml
└── sql/
    └── 002-add-indexes.sql       # 可选：纯 SQL 形式
```

`db.changelog-master.yaml` 只负责汇总：

```yaml
databaseChangeLog:
  - include:
      file: db/changelog/changes/001-init-schema.yaml
  - include:
      file: db/changelog/changes/002-add-indexes.yaml
```

### 3.5 最小示例（YAML DSL）

`001-init-schema.yaml`：

```yaml
databaseChangeLog:
  - changeSet:
      id: 001-init-schema
      author: feiya
      changes:
        - createTable:
            tableName: demo_apartment
            columns:
              - column:
                  name: id
                  type: BIGINT
                  autoIncrement: true
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: name
                  type: VARCHAR(64)
                  constraints:
                    nullable: false
              - column:
                  name: is_deleted
                  type: TINYINT
                  defaultValue: 0
                  constraints:
                    nullable: false
```

`002-add-indexes.yaml`：

```yaml
databaseChangeLog:
  - changeSet:
      id: 002-add-indexes
      author: feiya
      changes:
        - createIndex:
            tableName: room_info
            indexName: idx_room_apartment
            columns:
              - column:
                  name: apartment_id
              - column:
                  name: is_deleted
              - column:
                  name: is_release

  - changeSet:
      id: 003-add-agreement-index
      author: feiya
      changes:
        - createIndex:
            tableName: lease_agreement
            indexName: idx_agreement_apartment
            columns:
              - column:
                  name: apartment_id
              - column:
                  name: is_deleted
              - column:
                  name: status
```

### 3.6 最小示例（纯 SQL + 回滚）

不想学 DSL 时可以直接写 SQL，并配上 rollback：

```yaml
databaseChangeLog:
  - changeSet:
      id: 004-add-room-index
      author: feiya
      changes:
        - sql:
            sql: >
              CREATE INDEX idx_room_apartment
              ON room_info (apartment_id, is_deleted, is_release)
      rollback:
        - sql:
            sql: DROP INDEX idx_room_apartment ON room_info
```

### 3.7 常用操作

```bash
mvn liquibase:status    # 查看未执行的 changeset
mvn liquibase:update    # 执行
mvn liquibase:rollback -Dliquibase.rollbackCount=1   # 回滚最近 1 个
```

## 四、对比与选型

| 维度 | Flyway | Liquibase |
|---|---|---|
| 脚本形式 | 以原生 SQL 为主 | XML/YAML/JSON/SQL 均可 |
| 学习成本 | 低 | 中 |
| 回滚 | 社区版不支持 undo | 原生支持 rollback |
| 跨数据库 | SQL 需按方言编写 | DSL 抽象，跨库更友好 |
| 多人协作冲突 | 版本号冲突需人工协调 | `id + author` 粒度更细 |
| 校验机制 | checksum | checksum |
| Spring Boot 集成 | 自动配置 | 自动配置 |
| 适合团队 | 熟悉 SQL、单库为主 | 多数据库、需要回滚审计 |

**对本项目的建议**

- 只用 MySQL、团队熟悉 SQL → **Flyway**，把现有 `docs/mysql/indexes.sql` 拆成 `V2__add_apartment_query_indexes.sql` 即可；
- 需要每个变更都能自动回滚、或未来可能换数据库 → **Liquibase**；
- 学习阶段不必急于接入，先理解「版本号 + 历史表 + 只执行一次」的核心机制，再按需引入。

## 五、常见坑

1. **已执行的脚本被修改**：Flyway 报 `Validate failed: checksum mismatch`。不要删历史表了事，先确认改动是否必要，再执行 `flyway repair`。
2. **版本号冲突**：两个人同时提交 `V3__xxx.sql`，合并后会报重复版本，需要重新编号。
3. **MySQL 8 缺 `flyway-mysql`**：只引 `flyway-core` 会报 `Unsupported Database: MySQL 8.x`。
4. **`baseline-on-migrate` 滥用**：新库不需要，老库首次接入才用；配置不当会跳过本该执行的脚本。
5. **生产环境执行 `clean`**：Flyway 的 `clean` 会删除 schema 下所有对象，生产必须禁用：

   ```yaml
   spring:
     flyway:
       clean-disabled: true
   ```

6. **迁移脚本与 ORM 混用**：MyBatis-Plus 不会生成表结构，别指望实体改动自动同步数据库，DDL 统一走迁移脚本。
