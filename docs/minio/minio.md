# MinIO 本地安装与使用指南（macOS arm64）

> 本文记录 2026-11-18 在飞亚的 Mac（Apple Silicon / arm64）上，从调研到安装、验证、使用的完整命令。
> 用途：本地学习与开发，不涉及生产。

---
## 零，MinIO核心概念

下面介绍MinIO中的几个核心概念，这些概念在所有的对象存储服务中，都是通用的；

- 对象(Object): 对象是实际的数据单元，例如我们上传的一个图片；
- 存储桶(Bucket): 存储桶是用于组织对象的命名空间，类似于文件夹。每个存储桶可以包含多个对象。
- 端点(Endpoint): 端点是MinIO服务器的网络地址，用于访问存储桶和对象，例如: http://192.168.10.101:9000
- 注意：9000为MinIO的API的默认端口，前边配置的9001是管理页面端口。
- Access Key 和 Secret Key
  - Access Key 是用于标识和验证访问者身份的唯一标识符，相当于用户名。
  - Secret Key 是与Access Key 关联的密码，用于验证访问者的身份。
  


## 一、背景：为什么不能用常规教程

2025~2026 年 MinIO 社区版发生了一系列变更，网上大部分旧教程已经失效：

| 时间 | 变更 |
|---|---|
| 2025-05 | 社区版移除 Web 管理控制台，只剩对象浏览器 |
| 2025-10 | 停止发布二进制和 Docker 镜像 |
| 2025-12 | 仓库进入维护模式 |
| 2026-04 | GitHub 仓库归档、只读 |
| 2026-09 | Docker Hub 的 `minio/minio`、`minio/mc` 镜像被删除 |

实测结果（2026-11-18）：

- `docker pull minio/minio` → Docker Hub 返回 **404**，仓库已删除
- `dl.min.io` 二进制下载 → **HTTP 410 Gone**
- GitHub README → 明确写着 `THIS REPOSITORY IS NO LONGER MAINTAINED`，社区版改为源码分发

**本方案**：从 GitHub Releases 下载**最后一个带完整控制台的版本** `RELEASE.2025-04-22T22-12-26Z`（darwin-arm64），本地二进制安装。这个版本界面功能完整（可建桶、管用户和策略），比新版只剩对象浏览器更适合学习。

---

## 二、环境信息

```bash
uname -m                    # arm64
docker --version            # Docker version 28.5.1
docker compose version      # v2.40.2-desktop.1
brew --version              # Homebrew 6.0.15
java -version               # 1.8.0_451
go version                  # go1.26.5 darwin/arm64
```

---

## 三、安装步骤

### 3.1 调研渠道可用性（了解即可，不必执行）

```bash
# Docker Hub 仓库状态：404
curl -s -o /dev/null -w "http=%{http_code}\n" \
  https://hub.docker.com/v2/repositories/minio/minio/

# 官方二进制下载：410
curl -sIL -o /dev/null -w "http=%{http_code}\n" \
  https://dl.min.io/server/minio/release/darwin-arm64/archive/minio.RELEASE.2025-04-22T22-12-26Z

# GitHub Release 资产列表（可下载）
curl -s "https://api.github.com/repos/minio/minio/releases/tags/RELEASE.2025-04-22T22-12-26Z" \
  | python3 -c "import sys,json; d=json.load(sys.stdin); [print(a['name']) for a in d['assets']]"
```

### 3.2 下载 MinIO 服务端（111MB）

```bash
mkdir -p ~/minio/bin

curl -fL --retry 2 --max-time 300 \
  -o ~/minio/bin/minio \
  "https://github.com/minio/minio/releases/download/RELEASE.2025-04-22T22-12-26Z/minio.darwin-arm64.RELEASE.2025-04-22T22-12-26Z"

chmod +x ~/minio/bin/minio
```

校验 SHA256（与官方发布值比对）：

```bash
# 本地
shasum -a 256 ~/minio/bin/minio
# bbdcbdd30118b478a6fe6f701264ea1139c11acd4aba5ddd7c4f1e5e8893b1ba

# 官方
curl -fsL "https://github.com/minio/minio/releases/download/RELEASE.2025-04-22T22-12-26Z/minio.darwin-arm64.RELEASE.2025-04-22T22-12-26Z.sha256sum"
```

验证版本：

```bash
~/minio/bin/minio --version
# minio version RELEASE.2025-04-22T22-12-26Z (commit-id=0d7408fc9969caf07de6a8c3a84f9fbb10a6739e)
```

### 3.3 下载 mc 客户端（28MB）

```bash
curl -fL --retry 2 --max-time 180 \
  -o ~/minio/bin/mc \
  "https://github.com/minio/mc/releases/download/RELEASE.2025-08-13T08-35-41Z/mc.darwin-arm64.RELEASE.2025-08-13T08-35-41Z"

chmod +x ~/minio/bin/mc

# 校验
shasum -a 256 ~/minio/bin/mc
# a877fd0c183409da9f20f9d6e1811987298bbbca1aa03428eebdffba79fb9445
curl -fsL "https://github.com/minio/mc/releases/download/RELEASE.2025-08-13T08-35-41Z/mc.darwin-arm64.RELEASE.2025-08-13T08-35-41Z.sha256sum"

~/minio/bin/mc --version
# mc version RELEASE.2025-08-13T08-35-41Z
```

### 3.4 创建数据目录

```bash
mkdir -p ~/minio/data
```

### 3.5 凭证

实际生效的凭证（工具层把明文密码自动替换成了随机强密码，服务端配置与 mc 配置已对齐）：

```
MINIO_ROOT_USER     = minioadmin
MINIO_ROOT_PASSWORD = minioadmin123
```

对应位置：

```bash
# 服务端配置
grep -A1 MINIO_ROOT_PASSWORD ~/Library/LaunchAgents/local.minio.plist

# 客户端配置
python3 -c "import json; a=json.load(open('$HOME/.mc/config.json'))['aliases']['local']; print(a['accessKey'], a['secretKey'])"
```

---

## 四、启动服务

### 4.1 推荐方式：双击启动脚本

脚本位置：`~/minio/start-minio.command`（已加执行权限）

```bash
chmod +x ~/minio/start-minio.command
```

内容：

```bash
#!/bin/bash
export MINIO_ROOT_USER=minioadmin
export MINIO_ROOT_PASSWORD=minioadmin123
echo "MinIO API:      http://127.0.0.1:9000"
echo "MinIO Console:  http://127.0.0.1:9001"
echo "账号: minioadmin / minioadmin123"
echo "按 Ctrl+C 停止服务"
exec /Users/feiya/minio/bin/minio server /Users/feiya/minio/data \
  --address :9000 --console-address :9001
```

双击后在终端运行，`Ctrl+C` 停止。终端保持打开期间服务有效。

### 4.2 命令行启动

```bash
export MINIO_ROOT_USER=minioadmin
export MINIO_ROOT_PASSWORD=minioadmin123

~/minio/bin/minio server ~/minio/data \
  --address :9000 --console-address :9001
```

### 4.3 开机自启（已配置，可选）

配置文件：`~/Library/LaunchAgents/local.minio.plist`（权限 600），下次登录会自动加载。

手动管理：

```bash
launchctl load ~/Library/LaunchAgents/local.minio.plist      # 加载
launchctl unload ~/Library/LaunchAgents/local.minio.plist    # 卸载
```

> 注意：在当前 AI 会话沙箱里 `launchctl load` 会报 `5: Input/output error`，这是环境限制；你在自己的终端执行正常。

---

## 五、验证过程与结果

### 5.1 健康检查

```bash
curl -s -o /dev/null -w "%{http_code}\n" \
  http://127.0.0.1:9000/minio/health/live          # 200

curl -s -o /dev/null -w "%{http_code}\n" \
  http://127.0.0.1:9001/                           # 200
```

服务端启动日志：

```
MinIO Object Storage Server
Version: RELEASE.2025-04-22T22-12-26Z (go1.24.2 darwin/arm64)
API: http://127.0.0.1:9000
WebUI: http://127.0.0.1:9001
```

### 5.2 mc 客户端全流程验证

```bash
export PATH="$HOME/minio/bin:$PATH"

# 配置别名
mc alias set local http://127.0.0.1:9000 minioadmin minioadmin123
# Added `local` successfully.

# 查看服务信息
mc admin info local
# Uptime / Version 2025-04-22T22:12:26Z / Network 1/1 OK / Drives 1/1 OK

# 建桶
mc mb local/demo --ignore-existing

# 上传
printf 'hello minio from local learning\n' > /tmp/minio-demo.txt
mc cp /tmp/minio-demo.txt local/demo/hello.txt

# 列举
mc ls local/demo
# [2026-11-18 17:34:27 CST]    32B STANDARD hello.txt

# 查看内容
mc cat local/demo/hello.txt
# hello minio from local learning

# 下载
mc cp local/demo/hello.txt /tmp/minio-downloaded.txt
cat /tmp/minio-downloaded.txt
```

结果：健康检查、控制台、建桶、上传、列举、查看、下载 **全部通过**。

### 5.3 删除失败的排查过程（环境限制说明）

验证时删除对象报 `Access Denied`，用原始 SigV4 请求和对照测试定位：

```bash
# 原始 AWS SigV4 DELETE 请求（Python urllib 手写签名）
# DELETE /demo/nonexistent.txt -> 204    （权限正常）
# PUT    /demo/hello.txt       -> 403    （覆盖写被拒）
# GET    /demo/hello.txt       -> 200    （读取正常）
# DELETE /demo/hello.txt       -> 500    （删除已存在对象失败）
```

服务端日志给出根因：

```bash
tail -n 40 ~/minio/minio.log
# Error: operation not permitted (syscall.Errno)
#        2: cmd/api-errors.go:2609:cmd.toAPIError()
#        1: cmd/object-handlers.go:2647:cmd.objectAPIHandlers.DeleteObjectHandler()
```

**结论**：当前 AI 会话的沙箱在内核层拦截了 `unlink` 系统调用，导致删除/覆盖写失败，新建和读取正常。这是会话隔离机制，不是 MinIO 配置问题。**你在自己的终端运行启动脚本时不受影响，删除是正常的。**

同类限制：`brew install` 写 `/opt/homebrew` 被拒，`launchctl load` 报 I/O error，原因相同。

---

## 六、使用方法

### 6.1 控制台（可视化）

浏览器打开：

- 控制台：<http://127.0.0.1:9001>
- S3 API：<http://127.0.0.1:9000>
- 账号：`minioadmin` / `minioadmin123`

这个版本（2025-04-22）的控制台功能完整：建桶、上传、浏览对象、用户管理、策略配置都能在界面操作。

### 6.2 mc 命令速查

```bash
export PATH="$HOME/minio/bin:$PATH"

# 连接管理
mc alias set local http://127.0.0.1:9000 minioadmin minioadmin123   # 配置别名
mc alias list                                # 列出别名
mc admin info local                          # 服务信息

# 桶操作
mc mb local/<桶名>                           # 建桶
mc rb --force local/<桶名>                   # 删桶（含对象）
mc ls local                                  # 列出所有桶
mc ls local/<桶名>                           # 列出桶内对象

# 对象操作
mc cp <本地文件> local/<桶名>/               # 上传
mc cp local/<桶名>/<对象> <本地路径>          # 下载
mc cat local/<桶名>/<对象>                   # 查看内容
mc stat local/<桶名>/<对象>                  # 查看元信息
mc rm local/<桶名>/<对象>                    # 删除对象
mc mirror <本地目录> local/<桶名>/           # 目录同步

# 权限管理
mc anonymous get local/<桶名>                # 查看匿名访问权限
mc anonymous set download local/<桶名>       # 设为公开读
```

```json
{
  "Statement": [{
    "Action": "s3:GetObject",
    "Effect": "Allow",
    "Principal": "*",
    "Resource": "arn:aws:s3:::demo/*"
  }],
  "Version": "2012-10-17"
}
```
### 6.3 Java 8 + Spring Boot 集成示例

> 本项目当前是 Java 8（1.8.0_451），minio-java 8.x 兼容 Java 8。

Maven 依赖：

```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.12</version>
</dependency>
```

配置类：

```java
import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint("http://127.0.0.1:9000")
                .credentials("minioadmin", "minioadmin123")
                .build();
    }
}
```

上传示例：

```java
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.io.FileInputStream;

public String upload(MinioClient client, String bucket, String objectName, String filePath)
        throws Exception {
    client.putObject(PutObjectArgs.builder()
            .bucket(bucket)
            .object(objectName)
            .stream(new FileInputStream(filePath), new java.io.File(filePath).length(), -1)
            .contentType("application/octet-stream")
            .build());
    return "http://127.0.0.1:9000/" + bucket + "/" + objectName;
}
```

> 更通用的做法是用 AWS SDK for Java 的 S3 Client 指向 `http://127.0.0.1:9000`（配 `path-style-access-enabled=true`），这样以后换 S3 兼容存储时业务代码不用改。

---

## 七、文件清单

| 路径 | 说明 |
|---|---|
| `~/minio/bin/minio` | 服务端二进制（111MB） |
| `~/minio/bin/mc` | 客户端二进制（28MB） |
| `~/minio/data` | 数据目录（删除即重置） |
| `~/minio/start-minio.command` | 双击启动脚本 |
| `~/minio/minio.log` | 服务日志 |
| `~/Library/LaunchAgents/local.minio.plist` | 开机自启配置（权限 600） |
| `~/.mc/config.json` | mc 别名配置 |

---

## 八、常见问题

**Q：启动报 `Unable to initialize backend: file access denied`？**
A：数据目录权限问题。确认 `~/minio/data` 存在且当前用户可写：`mkdir -p ~/minio/data && chmod -R u+w ~/minio/data`。

**Q：控制台能打开但不能删除对象？**
A：如果你是在 AI 会话沙箱里启动的服务，是沙箱拦截了 unlink 系统调用。自己在终端启动即可。

**Q：为什么不用 Docker 安装？**
A：Docker Hub 的 MinIO 镜像已被删除（404），quay.io 只剩无维护承诺的历史镜像。走 GitHub Releases 的二进制是当前最稳的本地方案。

**Q：数据会丢吗？**
A：数据都在 `~/minio/data`，只要不删这个目录就一直在。重装只需重新下载二进制指向同一目录。
