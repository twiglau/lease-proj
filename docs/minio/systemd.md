# MinIO 集成 Systemd

## Systemd 概述

Systemd 是 Linux 系统的初始化和服务管理器，负责管理系统中的各种服务和进程，
包括启动、停止和重启服务。此外，它还能检测服务运行状态，在服务异常退出时自动拉起，
以保证服务的稳定性。系统自带的 firewalld、自己安装的 mysqld 和 redis 均由 Systemd 管理。
此处将 MinIO 服务也交给 Systemd 管理。

> 环境说明：以下配置适用于 systemd ≥ 247（Ubuntu 20.04+ / CentOS 8+ / Rocky 8+）。
> CentOS 7 的 systemd 219 不支持 `ProtectProc`，需删除该行。

---

## 一、创建运行用户和数据目录

> 官方推荐以非 root 用户运行 MinIO，避免安全风险。这一步原文档缺失，必须补上。

```bash
# 创建系统用户组和用户（-r 表示系统用户，-M 表示不创建家目录）
sudo groupadd -r minio-user
sudo useradd -M -r -g minio-user minio-user

# 创建数据存储目录（-p 表示目录已存在时不报错）
sudo mkdir -p /data

# 把数据目录属主改为 minio-user，否则服务无权写入
sudo chown -R minio-user:minio-user /data
```

---

## 二、安装二进制文件

```bash
# 把 minio 二进制拷到 /usr/local/bin
# 注意：service 文件里的 AssertFileIsExecutable 会检查这个路径，路径不对服务无法启动
sudo cp minio /usr/local/bin/minio
sudo chmod +x /usr/local/bin/minio

# 验证
/usr/local/bin/minio --version
```

---

## 三、编写 MinIO 服务配置文件

Systemd 管理的服务需要一个配置文件描述，这些文件位于 `/etc/systemd/system/` 或
`/usr/lib/systemd/system/` 目录。创建 MinIO 服务配置文件：

```bash
sudo vim /etc/systemd/system/minio.service
```

内容如下（`#` 后为注释，说明每个字段的作用）：

```ini
[Unit]
# 服务描述，systemctl status 时显示
Description=MinIO
# 文档链接，仅作说明，不影响运行
Documentation=https://min.io/docs/minio/linux/index.html
# 依赖网络就绪后再启动
Wants=network-online.target
After=network-online.target
# 启动前断言二进制文件存在且可执行，不满足则拒绝启动
AssertFileIsExecutable=/usr/local/bin/minio

[Service]
# 服务的工作目录
WorkingDirectory=/usr/local

# 以非 root 用户运行（安全最佳实践）
User=minio-user
Group=minio-user
# 隐藏其他进程信息，增强隔离（systemd < 247 需删除此行）
ProtectProc=invisible

# 从 /etc/default/minio 读取环境变量；开头的 "-" 表示文件不存在也不报错
EnvironmentFile=-/etc/default/minio

# 启动前检查：若 MINIO_VOLUMES 未设置则报错退出，避免服务用错目录
# 注意：外层用单引号，内部的 \" 是对 bash 的转义，写成 |" 会导致语法错误
ExecStartPre=/bin/bash -c 'if [ -z "${MINIO_VOLUMES}" ]; then echo "Variable MINIO_VOLUMES not set in /etc/default/minio"; exit 1; fi'

# 核心启动命令：$MINIO_OPTS 和 $MINIO_VOLUMES 均来自 EnvironmentFile
ExecStart=/usr/local/bin/minio server $MINIO_OPTS $MINIO_VOLUMES

# MinIO RELEASE.2023-05-04 之后支持 Type=notify，可让 systemd 准确感知服务就绪状态
Type=notify
NotifyAccess=all

# 服务异常退出后总是自动重启
Restart=always

# 最大文件描述符数（对象存储会打开大量文件，必须调大）
LimitNOFILE=65536
# 不限制线程数
TasksMax=infinity
# 停止时无限等待，直到进程真正退出
TimeoutStopSec=infinity
# 停止时不发送 SIGKILL 强杀，让 MinIO 优雅关闭
SendSIGKILL=no

[Install]
# 开机自启所属的目标（多用户命令行模式）
WantedBy=multi-user.target
```

重点关注：

- **EnvironmentFile**：存放 MinIO 所需的各项参数，便于集中管理
- **ExecStart**：启动命令，`$MINIO_OPTS`、`$MINIO_VOLUMES` 均引用自 EnvironmentFile
  - `MINIO_OPTS`：启动选项，如控制台端口，可省略
  - `MINIO_VOLUMES`：数据存储路径
- **ExecStartPre**：启动前校验，防止漏配变量导致服务异常
- **Restart=always**：服务异常退出时自动拉起

---

## 四、编写 EnvironmentFile 文件

```bash
sudo vim /etc/default/minio
```

内容如下：

```ini
# root 账号用户名（原文档 MINIO_ROOT_URSER 拼写错误，已修正）
MINIO_ROOT_USER=minioadmin

# root 账号密码：至少 8 位！请替换为你自己的强密码
# 原文档这里用的是示例值，Linux 上务必改成自己的
MINIO_ROOT_PASSWORD=CHANGE_ME_至少8位强密码

# 数据存储路径，必须与前面 chown 授权的目录一致
MINIO_VOLUMES=/data

# 启动选项：9001 为 Web 管理控制台端口（9000 是 S3 API 端口，默认开启）
MINIO_OPTS="--console-address :9001"
```

设置文件权限，避免其他用户读到明文密码：

```bash
sudo chmod 600 /etc/default/minio
```

---

## 五、启动 MinIO

```bash
# 关键：新建或修改 service 文件后必须重载配置，否则 systemctl 找不到 minio 服务
sudo systemctl daemon-reload

# 启动服务
sudo systemctl start minio

# 查看运行状态
sudo systemctl status minio

# 设置开机自启（--now 表示同时立即启动，可替代上面的 start）
sudo systemctl enable --now minio
```

排错时看日志：

```bash
journalctl -u minio -f
```

---

## 六、防火墙放行端口（如启用了 firewalld）

> 不放行端口的话，浏览器访问会被拦截。9000 是 S3 API，9001 是控制台。

```bash
sudo firewall-cmd --permanent --add-port=9000/tcp
sudo firewall-cmd --permanent --add-port=9001/tcp
sudo firewall-cmd --reload

# 查看已放行端口
sudo firewall-cmd --list-ports
```

---

## 七、验证访问

浏览器打开控制台（把 IP 换成你的服务器实际地址）：

```
http://192.168.10.101:9001
```

用 `/etc/default/minio` 里配置的 `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` 登录。
命令行健康检查：

```bash
curl -i http://127.0.0.1:9000/minio/health/live
# 返回 HTTP/1.1 200 OK 表示服务正常
```

---

## 八、常用运维命令

```bash
sudo systemctl status minio      # 查看状态
sudo systemctl restart minio     # 重启
sudo systemctl stop minio        # 停止
sudo systemctl disable minio     # 取消开机自启
sudo systemctl daemon-reload     # 修改 service 文件后重载
journalctl -u minio -n 50        # 查看最近 50 行日志
```

---

## 九、原文档错误对照

| 位置 | 原内容 | 修正 |
|---|---|---|
| service 路径 | `etc/systemd/system/` | `/etc/systemd/system/`（补开头 `/`） |
| Documentation | `ocumentation=...min.10/docs/min10/1znux...` | `Documentation=https://min.io/docs/minio/linux/index.html` |
| ExecStartPre | `if [ -z \|"${MINIO_VOLUMES}\" ]...echo \| "Variable...` | 单引号包裹，内部 `\"` 转义（管道符 `\|"` 是语法错误） |
| 环境变量 | `MINIO_ROOT_URSER` | `MINIO_ROOT_USER`（拼写修正） |
| 启动流程 | 缺 `daemon-reload` | 补上 `sudo systemctl daemon-reload` |
| 二进制部署 | 未说明 | 补上 `cp minio /usr/local/bin/minio` |
| 运行用户 | 未说明 | 补上 `minio-user` 用户创建和数据目录授权 |
| 文件权限 | 未说明 | 补上 `chmod 600 /etc/default/minio` |
| 防火墙 | 未说明 | 补上 firewalld 放行 9000/9001 |
