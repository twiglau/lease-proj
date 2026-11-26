# MySQL 8 连接报错：Public Key Retrieval is not allowed

> 记录时间：2026-11-25
> 环境：macOS（Apple Silicon）、MySQL 8.4（Homebrew）、Spring Boot 3.0.11、mysql-connector-j 8.0.33、HikariCP 5.0.1
> 触发接口：`GET /admin/payment/list`（支付方式列表）

## 一、现象

访问支付方式列表接口时，应用日志抛出：

```text
java.sql.SQLNonTransientConnectionException: Public Key Retrieval is not allowed
    at com.mysql.cj.jdbc.exceptions.SQLError.createSQLException(SQLError.java:111)
    ...
    at com.zaxxer.hikari.pool.HikariPool.createPoolEntry(HikariPool.java:470)
    ...
    at org.twiglau.lease.web.admin.controller.apartment.PaymentTypeController.listPaymentType(PaymentTypeController.java:33)

Caused by: com.mysql.cj.exceptions.UnableToConnectException: Public Key Retrieval is not allowed
    at com.mysql.cj.protocol.a.authentication.CachingSha2PasswordPlugin.nextAuthenticationStep(CachingSha2PasswordPlugin.java:130)
    at com.mysql.cj.protocol.a.NativeAuthenticationProvider.proceedHandshakeWithPluggableAuthentication(NativeAuthenticationProvider.java:443)
    ...
```

关键信息有两处：

- 异常类型是 `SQLNonTransientConnectionException`，说明**连接根本没建立起来**，不是 SQL 写错。
- 底层来自 `CachingSha2PasswordPlugin`，直接指向 MySQL 8 的密码认证插件 `caching_sha2_password`。

## 二、原因

### 2.1 MySQL 8 的默认认证方式变了

MySQL 5.7 默认使用 `mysql_native_password`；从 MySQL 8.0 开始，默认插件改为 `caching_sha2_password`。两者的认证流程不同：

- `mysql_native_password`：客户端把密码做 SHA1 混淆后直接发给服务器。
- `caching_sha2_password`：密码不在网络上明文传输。首次认证（或服务器缓存失效）时，服务器要求客户端用**服务器的 RSA 公钥**加密密码后再发送。

### 2.2 驱动默认禁止向服务器索取公钥

MySQL Connector/J 有一个安全参数 `allowPublicKeyRetrieval`，默认值是 `false`。也就是说：

1. 连接使用 `useSSL=false`（本项目配置如此），通道是明文的；
2. 服务器走 `caching_sha2_password` 的完整认证流程，要求客户端取 RSA 公钥；
3. 驱动发现没有开启 SSL、又没有允许取公钥，于是直接拒绝，抛出 `Public Key Retrieval is not allowed`。

完整链路：

```text
JDBC 发起连接
  → TCP 握手成功
  → 服务器使用 caching_sha2_password 要求完整认证
  → 驱动需要 RSA 公钥来加密密码
  → useSSL=false 且 allowPublicKeyRetrieval=false
  → 驱动拒绝取公钥，抛 Public Key Retrieval is not allowed
```

### 2.3 为什么之前能连、现在才报错

`caching_sha2_password` 带有一层服务端缓存：

- 同一账号短时间内重复连接，服务器会走**快速认证（fast auth）**，不需要 RSA 公钥；
- 服务器重启、缓存淘汰、连接来自新的客户端或密码变更后，会退回**完整认证（full auth）**，这时才需要公钥。

所以这个错误经常表现为「昨天还好好的，今天突然连不上」，容易误判成代码问题。

## 三、解决

### 3.1 推荐：连接串增加 allowPublicKeyRetrieval=true

修改 `web/web-admin/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    type: com.zaxxer.hikari.HikariDataSource
    url: jdbc:mysql://127.0.0.1:3306/lease?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=GMT%2d8
    username: root
    password: <你的本地 root 密码>
```

参数说明：

| 参数 | 作用 |
|---|---|
| `useSSL=false` | 本地开发不走 SSL，减少证书配置 |
| `allowPublicKeyRetrieval=true` | 允许驱动向服务器索取 RSA 公钥，用于加密密码 |
| `serverTimezone=GMT%2d8` | 指定东八区，避免时间字段偏移 |

修改后重启应用即可。当时实测接口恢复：

```json
{"code":200,"message":"成功","data":[{"id":6,"name":"月付","payMonthCount":1,"additionalInfo":"押一付一"}, ...]}
```

### 3.2 其他可选方案（了解即可）

| 方案 | 做法 | 评价 |
|---|---|---|
| 开启 SSL | `useSSL=true&requireSSL=true` | 生产环境首选，通道加密后不需要取公钥；需要证书配置 |
| 改用户认证插件 | `ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '密码';` | 不推荐：MySQL 8.4 已默认移除 `mysql_native_password`，属于开历史倒车 |
| 升级驱动 | 换更高版本 Connector/J | 不解决本质问题，默认行为仍然是禁止取公钥 |

### 3.3 安全提醒

`allowPublicKeyRetrieval=true` 配合 `useSSL=false` 意味着连接是明文的，理论上存在中间人攻击风险（攻击者可以冒充服务器返回自己的公钥）。

- 本地学习、内网受信任环境：可以接受。
- 生产环境：应使用 SSL，或至少不要同时关闭 SSL 又允许取公钥。

## 四、验证与排查命令

### 4.1 确认账号使用的认证插件

```bash
mysql -h 127.0.0.1 -uroot -p -e "SELECT user, host, plugin FROM mysql.user WHERE user='root';"
```

输出中的 `plugin` 为 `caching_sha2_password` 即符合本文场景。MySQL 8.4 也可以查看全局策略：

```sql
SHOW VARIABLES LIKE 'authentication_policy';
```

### 4.2 确认数据库和表存在

```bash
mysql -h 127.0.0.1 -uroot -p -e "SHOW DATABASES LIKE 'lease';"
mysql -h 127.0.0.1 -uroot -p lease -e "SELECT COUNT(*) FROM payment_type;"
```

### 4.3 启动应用并验证接口

```bash
curl -s http://127.0.0.1:8080/admin/payment/list
```

## 五、同类连接错误的区分

排查 JDBC 连接问题时，先看异常类型，再定位方向：

| 报错 | 含义 | 处理方向 |
|---|---|---|
| `Public Key Retrieval is not allowed` | 认证阶段缺公钥 | 加 `allowPublicKeyRetrieval=true`，或开启 SSL |
| `Access denied for user ...` | 账号或密码错误 | 核对用户名、密码、host 匹配规则 |
| `Communications link failure` + `EOFException` | TCP 通了但 MySQL 握手被断开 | 检查 host/port 是否是有效 MySQL 服务 |
| `Unknown database 'lease'` | 库不存在 | 导入建库脚本 |
| `Table 'lease.xxx' doesn't exist` | 表不存在 | 导入建表、初始化数据脚本 |

> 关联问题：本次调试前还遇到过 `jdbc:mysql://120.0.0.1:3306/lease` 导致 `Communications link failure`。该地址能建立 TCP 连接，但在 MySQL 握手阶段被断开；改为 `127.0.0.1` 后解决。两个问题叠在一起时，容易误以为是同一个原因，实际要分开处理。
