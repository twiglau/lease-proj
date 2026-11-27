# mysql

## 导出数据库

```shell
# 只导出表结构。
mysqldump -h127.0.0.1 -uroot -p \
--no-data --skip-comments --set-gtid-purged=OFF \
lease > /tmp/V1__init_schema.sql

# 导出表+数据
mysqldump -h127.0.0.1 -uroot -p \
  --single-transaction \
  --skip-lock-tables \
  --skip-add-drop-table \
  --set-gtid-purged=OFF \
  --skip-comments \
  --default-character-set=utf8mb4 \
  --no-tablespaces \
  lease > /tmp/V1__init_schema_and_data.sql
```

| 参数 | 作用 |
|------|-----|
|去掉 --no-data | 关键: 连数据一起导出 |
|--single-transaction | InnoDB下用一致性快照导出，不锁表 |
| --skip-lock-tables | 避免 LOCK TABLES,与上一条配合 |
|--skip-add-drop-table | 不生成 DROP TABLE IF EXISTS,避免迁移脚本误删表 |
| --set-gtid-purged=OFF | 去掉GTID相关语句，普通环境可执行 |
| --default-character-set=utf8mb4 | 中文不乱码 |
|--no-tablespace | 避免因缺少PROCESS权利失败 |


```shell
# 导出数据脚本
mysqldump -h127.0.0.1 -uroot -p \
  --no-create-info \
  --complete-insert \
  --skip-extended-insert \
  --skip-triggers \
  --set-gtid-purged=OFF \
  --skip-comments \
  --default-character-set=utf8mb4 \
  lease > /tmp/V2__seed_data.sql
 
# --no-create-info: 只要数据；
# --complete-insert: INSERT 带列名，表结构微调后仍能执行；
# --skip-extended-insert: 一行一条 INSERT, 出问题容易定位。
```