# 查找

## 按端口查
```shell
# -iTCP:端口 => 指定端口
# -nP 不解析域名、端口名
lsof -nP -iTCP:9000 -iTCP:9001
# 只看监听中的（排除已建立的连接）
lsof -nP -iTCP:9000 -sTCP:LISTEN
ps -p 1944 -o pid,user,command
```

### 按进程名查 PID
```shell
# -f 匹配完整命令行
# -l 显示进程名
pgrep -fl minio
```

## 查所有进程 + 过滤
```shell
ps aux | grep minio
```