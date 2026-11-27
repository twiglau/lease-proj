# 读懂 EXPLAIN：条件顺序、最左前缀与覆盖索引

> 记录时间：2026-11-27
> 环境：MySQL 8.x、库 `lease`、表 `room_info`
> 相关索引脚本：`docs/mysql/indexes.sql`

## 一、被测查询与索引

```sql
SELECT apartment_id, COUNT(*)
FROM room_info
WHERE is_deleted = 0
  AND is_release = 1
GROUP BY apartment_id;
```

```sql
CREATE INDEX idx_room_apartment
    ON room_info (apartment_id, is_deleted, is_release);
```

先确认索引确实存在：

```sql
SHOW INDEX FROM room_info;
```

```text
+-----------+------------+--------------------+--------------+--------------+
| Table     | Non_unique | Key_name           | Seq_in_index | Column_name  |
+-----------+------------+--------------------+--------------+--------------+
| room_info |          0 | PRIMARY            |            1 | id           |
| room_info |          1 | idx_room_apartment |            1 | apartment_id |
| room_info |          1 | idx_room_apartment |            2 | is_deleted   |
| room_info |          1 | idx_room_apartment |            3 | is_release   |
+-----------+------------+--------------------+--------------+--------------+
```

## 二、实际 EXPLAIN 结果

```text
+----+-------------+-----------+-------+--------------------+--------------------+---------+------+------+----------+--------------------------+
| id | select_type | table     | type  | possible_keys      | key                | key_len | ref  | rows | filtered | Extra                    |
+----+-------------+-----------+-------+--------------------+--------------------+---------+------+------+----------+--------------------------+
|  1 | SIMPLE      | room_info | index | idx_room_apartment | idx_room_apartment | 13      | NULL |   14 |     7.14 | Using where; Using index |
+----+-------------+-----------+-------+--------------------+--------------------+---------+------+------+----------+--------------------------+
```

## 三、逐列解读

| 列 | 值 | 含义 |
|---|---|---|
| `id` | 1 | 查询序号，单表查询只有一行 |
| `select_type` | SIMPLE | 简单查询，无子查询/UNION |
| `table` | room_info | 访问的表 |
| `type` | index | **全索引扫描**（不是全表扫描，也不是精准范围查找） |
| `possible_keys` | idx_room_apartment | 理论上可选的索引 |
| `key` | idx_room_apartment | 实际选中的索引 |
| `key_len` | 13 | 实际使用的索引字节数，说明用了哪几列（见下节） |
| `ref` | NULL | 没有用常量做等值定位，因为走的是索引全扫 |
| `rows` | 14 | 预计扫描行数，正好等于当前表总行数 |
| `filtered` | 7.14 | 扫描后再过滤的比例（1/14），估算值 |
| `Extra` | Using where; Using index | 索引内过滤 + 覆盖索引，无需回表 |

## 四、key_len = 13 是怎么来的

`key_len` 能看出索引实际用了哪几列：

| 索引列 | 类型 | 可空 | 计算 |
|---|---|---|---|
| `apartment_id` | BIGINT | 是 | 8 + 1 = 9 |
| `is_deleted` | TINYINT | 是 | 1 + 1 = 2 |
| `is_release` | TINYINT | 是 | 1 + 1 = 2 |
| 合计 | | | **13** |

- 可空列额外占 1 字节存 NULL 标记；
- 三列合计 13，说明**三列全部参与**，没有被截断。

如果列是 `NOT NULL`，同样的索引 `key_len` 会是 `8 + 1 + 1 = 10`。

## 五、为什么条件顺序和索引列顺序不一致也能用

这是最容易误解的点：**SQL 里 WHERE 条件的书写顺序，与索引列顺序没有对应关系**。

这条查询对三列的来源是：

| 索引列 | 使用场景 |
|---|---|
| `apartment_id` | `GROUP BY apartment_id` |
| `is_deleted` | `WHERE is_deleted = 0` |
| `is_release` | `WHERE is_release = 1` |

优化器不是按文本顺序匹配索引，而是收集「哪些列有条件」，再决定索引能否使用。三列都有条件，所以整条索引可用。

### 5.1 最左前缀约束的是索引列，不是 SQL 文本

```sql
-- 索引 (apartment_id, is_deleted, is_release)

WHERE apartment_id = 1 AND is_release = 1;
-- 可用：最左列 apartment_id 有等值条件

WHERE is_deleted = 0 AND is_release = 1;
-- 通常不可用：缺少最左列 apartment_id

WHERE apartment_id = 1 AND is_release = 1 AND is_deleted = 0;
-- 可用：三个等值条件，顺序随意
```

### 5.2 多个等值条件可以任意排列

优化器在等值条件下会把列当作集合处理：

```sql
WHERE is_deleted = 0 AND is_release = 1 AND apartment_id = 1;
-- 与
WHERE apartment_id = 1 AND is_deleted = 0 AND is_release = 1;
-- 等价
```

### 5.3 范围条件才会打断后续列

```sql
-- 索引 (a, b, c)
WHERE a > 10 AND b = 2;
-- a 用于范围定位，b 无法继续用于定位（可能用于索引内过滤）
```

所以「等值条件顺序自由」和「最左前缀」并不矛盾：前者说的是 SQL 文本，后者说的是索引列之间的依赖。

## 六、GROUP BY 为什么也能利用索引

B+ 树索引本身是有序的。`GROUP BY apartment_id` 正好匹配索引最左列，MySQL 可以按索引顺序扫描完成分组，避免创建临时表和额外排序。

`Extra` 中没有出现：

- `Using temporary`
- `Using filesort`

说明分组和排序都借助了索引顺序。

### 6.1 type = index 是什么

| type | 含义 |
|---|---|
| `const` / `eq_ref` | 主键或唯一索引定位单行 |
| `ref` | 非唯一索引等值定位 |
| `range` | 索引范围扫描 |
| `index` | **扫描整棵索引树** |
| `ALL` | 全表扫描 |

`index` 比 `ALL` 好（只读索引，不读表数据），但仍是全量扫描。当前表只有 14 行，优化器认为扫整个索引比走范围更划算，所以选择 `index`。

### 6.2 Using index 是什么

`Using index` 表示**覆盖索引**：查询需要的列（`apartment_id`、`is_deleted`、`is_release`）都在索引里，不需要回表读聚簇索引。这是这条 SQL 性能好的关键。

如果查询额外加了 `SELECT name`，`name` 不在索引中，就会退化为需要回表，`Extra` 里的 `Using index` 会消失。

## 七、数据量变大后会怎样

当前执行计划是小表下的最优解。数据量上来后，可能出现：

1. `type` 变成 `range`，如果查询加上 `apartment_id` 的等值或范围条件；
2. `Extra` 出现 `Using index for group-by`（松散索引扫描），分组效率更高；
3. 仍然保持 `Using index`，继续走覆盖索引。

可以用 `EXPLAIN ANALYZE` 看实际耗时：

```sql
EXPLAIN ANALYZE
SELECT apartment_id, COUNT(*)
FROM room_info
WHERE is_deleted = 0
  AND is_release = 1
GROUP BY apartment_id;
```

关注实际行数、循环次数和耗时，与 `rows` 估算对比，判断统计信息是否准确（必要时 `ANALYZE TABLE room_info;`）。

## 八、常见误判

1. **看到 `type=index` 就以为走了索引定位**：它只是扫描整棵索引，不是精确查找，`rows` 接近表总行数时要注意。
2. **把 WHERE 顺序当成索引顺序**：真正影响的是「索引列是否有条件」，以及最左列是否参与。
3. **忽略隐式类型转换**：`varchar` 列与数字比较、或函数包住索引列（`DATE(create_time) = ...`）会导致索引失效，`key` 变成 NULL。
4. **只看 `key` 不看 `Extra`**：`Using filesort`、`Using temporary` 往往才是性能瓶颈。
5. **用 `filtered` 当真实比例**：它是优化器估算值，`EXPLAIN ANALYZE` 才能看到实际值。

## 九、结论

- 条件书写顺序不影响索引使用，三列分别由 WHERE 和 GROUP BY 提供，索引完整生效；
- `key_len=13` 证明 `apartment_id`、`is_deleted`、`is_release` 三列全部参与；
- `Using index` 说明是覆盖索引，无回表；
- 当前 `type=index` 是全索引扫描，在小表上是合理选择，不需要为了「看起来更精准」改写 SQL。
