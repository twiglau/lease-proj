# 公寓分页查询 SQL 简化对比（apartmentInfoMapper.xml）

> 记录时间：2026-11-27
> 环境：MySQL 8.x、MyBatis-Plus 3.5.4、Spring Boot 3.0.11
> 相关文件：`web/web-admin/src/main/resources/mapper/ApartmentInfoMapper.xml`
> 接口：`GET /admin/apartment/pageItem`

## 一、当前写法

```sql
SELECT ai.id,
       ai.name,
       ai.introduction,
       ai.district_id,
       ai.district_name,
       ai.city_id,
       ai.city_name,
       ai.province_id,
       ai.province_name,
       ai.address_detail,
       ai.latitude,
       ai.longitude,
       ai.phone,
       ai.is_release,
       IFNULL(tc.count_tc, 0)                          AS total_room_count,
       IFNULL(tc.count_tc, 0) - IFNULL(lc.count_lc, 0) AS free_room_count
FROM (SELECT id,
             name,
             introduction,
             district_id,
             district_name,
             city_id,
             city_name,
             province_id,
             province_name,
             address_detail,
             latitude,
             longitude,
             phone,
             is_release
      FROM apartment_info
      WHERE is_deleted = 0
        -- 动态条件：province_id / city_id / district_id
     ) ai
         LEFT JOIN
     (SELECT apartment_id, COUNT(*) AS count_tc
      FROM room_info
      WHERE is_deleted = 0
        AND is_release = 1
      GROUP BY apartment_id) tc
     ON ai.id = tc.apartment_id
         LEFT JOIN
     (SELECT apartment_id, COUNT(*) AS count_lc
      FROM lease_agreement
      WHERE is_deleted = 0
        AND status IN (2, 5)
      GROUP BY apartment_id) lc
     ON ai.id = lc.apartment_id;
```

结构可以理解为：

```text
(公寓主表 + 过滤) ai
  LEFT JOIN (房间数聚合) tc
  LEFT JOIN (合同数聚合) lc
```

## 二、简化后的写法

```sql
SELECT ai.id,
       ai.name,
       ai.introduction,
       ai.district_id,
       ai.district_name,
       ai.city_id,
       ai.city_name,
       ai.province_id,
       ai.province_name,
       ai.address_detail,
       ai.latitude,
       ai.longitude,
       ai.phone,
       ai.is_release,
       IFNULL(tc.count_tc, 0)                          AS total_room_count,
       IFNULL(tc.count_tc, 0) - IFNULL(lc.count_lc, 0) AS free_room_count
FROM apartment_info ai
         LEFT JOIN (SELECT apartment_id, COUNT(*) AS count_tc
                    FROM room_info
                    WHERE is_deleted = 0
                      AND is_release = 1
                    GROUP BY apartment_id) tc ON ai.id = tc.apartment_id
         LEFT JOIN (SELECT apartment_id, COUNT(*) AS count_lc
                    FROM lease_agreement
                    WHERE is_deleted = 0
                      AND status IN (2, 5)
                    GROUP BY apartment_id) lc ON ai.id = lc.apartment_id
WHERE ai.is_deleted = 0
  <if test="queryVo.provinceId != null">
      AND ai.province_id = #{queryVo.provinceId}
  </if>
  <if test="queryVo.cityId != null">
      AND ai.city_id = #{queryVo.cityId}
  </if>
  <if test="queryVo.districtId != null">
      AND ai.district_id = #{queryVo.districtId}
  </if>
```

## 三、改动点对比

| 项目 | 当前写法 | 简化后 | 影响 |
|---|---|---|---|
| 主表派生表 `ai` | 把 `apartment_info` 整表包一层子查询 | 直接 `FROM apartment_info ai` | 少一层嵌套，WHERE 下推到主表，可读性和优化空间更好 |
| 过滤条件位置 | 在派生表内部 | 在外层 WHERE | 语义相同，执行计划通常等价或更优 |
| 房间数聚合 `tc` | 派生表 + GROUP BY | 保持不变 | 逻辑不变 |
| 合同数聚合 `lc` | 派生表 + GROUP BY | 保持不变 | 逻辑不变 |
| OGNL 判断 | `queryVo.getProvinceId() != null` | `queryVo.provinceId != null` | 等价，后者更简洁 |
| 返回字段 | 不变 | 不变 | 接口契约不变 |

## 四、为什么不直接三表 JOIN

看起来「三张表 JOIN 一下再 GROUP BY」更短，例如：

```sql
SELECT ai.*,
       COUNT(r.id) AS total_room_count,
       COUNT(r.id) - COUNT(l.id) AS free_room_count
FROM apartment_info ai
LEFT JOIN room_info r ON r.apartment_id = ai.id ...
LEFT JOIN lease_agreement l ON l.apartment_id = ai.id ...
GROUP BY ai.id;
```

问题在于 `room_info` 和 `lease_agreement` 都是「一个公寓多条记录」，两路一对多 JOIN 会产生**笛卡尔积**：

```text
公寓 A：3 个房间 × 2 份合同 = 6 行
```

此时 `COUNT(r.id)` 结果变成 6，必须改成 `COUNT(DISTINCT r.id)` 才能算对；即便改对，中间结果集也会按乘积膨胀，数据量上来后性能明显差于「先分组、再 JOIN」。

现有「两个子查询各自 GROUP BY 后再 LEFT JOIN」的思路是正确的，不需要推翻。

## 五、可选方案：CTE（WITH）

MySQL 8 支持公用表表达式，可读性更好：

```sql
WITH room_count AS (SELECT apartment_id, COUNT(*) AS count_tc
                    FROM room_info
                    WHERE is_deleted = 0
                      AND is_release = 1
                    GROUP BY apartment_id),
     lease_count AS (SELECT apartment_id, COUNT(*) AS count_lc
                     FROM lease_agreement
                     WHERE is_deleted = 0
                       AND status IN (2, 5)
                     GROUP BY apartment_id)
SELECT ai.id,
       ai.name,
       ...
       IFNULL(rc.count_tc, 0) AS total_room_count,
       IFNULL(rc.count_tc, 0) - IFNULL(lc.count_lc, 0) AS free_room_count
FROM apartment_info ai
         LEFT JOIN room_count rc ON ai.id = rc.apartment_id
         LEFT JOIN lease_count lc ON ai.id = lc.apartment_id
WHERE ai.is_deleted = 0
  -- 动态条件
```

**注意**：MyBatis-Plus 3.5.4 的分页插件依赖 JSqlParser 生成 count SQL，部分版本对 CTE 的解析支持不完整，可能出现分页 `total` 不准。使用 CTE 前务必验证：

```sql
-- 分页 total 是否与实际数据量一致
SELECT COUNT(*) FROM apartment_info WHERE is_deleted = 0;
```

如果无法确认，保持派生表写法最稳。

## 六、可选方案：标量子查询

```sql
SELECT ai.*,
       (SELECT COUNT(*)
        FROM room_info r
        WHERE r.apartment_id = ai.id
          AND r.is_deleted = 0
          AND r.is_release = 1) AS total_room_count,
       (SELECT COUNT(*)
        FROM room_info r
        WHERE r.apartment_id = ai.id
          AND r.is_deleted = 0
          AND r.is_release = 1)
           -
       (SELECT COUNT(*)
        FROM lease_agreement l
        WHERE l.apartment_id = ai.id
          AND l.is_deleted = 0
          AND l.status IN (2, 5)) AS free_room_count
FROM apartment_info ai
WHERE ai.is_deleted = 0;
```

优点：

- 分页时只为当前页的 N 行执行子查询，不会像派生表那样先聚合全表；
- SQL 更贴近「每个公寓算一次」的业务语义。

缺点：

- `total_room_count` 的子查询重复了一次，可读性差；
- 数据量大、命中行数多时，逐行子查询可能比一次性聚合慢。

适合「分页页大小固定、表数据量大、索引齐全」的场景，建议用 `EXPLAIN ANALYZE` 实测后再选。

## 七、索引前提

### 7.1 为什么必须先确认索引

这条查询的两个聚合子查询都是「按 `apartment_id` 分组统计」：

```sql
SELECT apartment_id, COUNT(*) FROM room_info ... GROUP BY apartment_id
```

如果 `apartment_id` 上没有索引，MySQL 只能全表扫描 + 临时表分组；公寓数量、房间数量一大，接口就会明显变慢。`EXPLAIN` 里通常表现为 `type=ALL`、`Extra=Using temporary; Using filesort`。

当前数据库的实际情况（2026-11-27 查询 `information_schema.STATISTICS`）：

| 表 | 已有索引 |
|---|---|
| `apartment_info` | 仅 `PRIMARY(id)` |
| `room_info` | 仅 `PRIMARY(id)` |
| `lease_agreement` | 仅 `PRIMARY(id)` |

也就是说，**业务字段上没有任何二级索引**，需要手动补充。

### 7.2 第一步：判断索引是否已存在

方式一，查看某张表的全部索引：

```sql
SHOW INDEX FROM room_info;
```

方式二，只查业务索引（推荐，输出更清楚）：

```sql
SELECT INDEX_NAME,
       GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS columns
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'room_info'
GROUP BY INDEX_NAME;
```

方式三，直接看建表语句：

```sql
SHOW CREATE TABLE room_info;
```

判断标准是**索引名**和**列顺序**，两者都一致才算已存在。`SHOW INDEX` 里一个复合索引会显示多行（每个列一行），`Seq_in_index` 表示列的顺序。

### 7.3 第二步：不存在再创建

**MySQL 不支持 `CREATE INDEX IF NOT EXISTS`**（PostgreSQL 支持），所以不能直接写一条幂等语句。常用三种做法：

**做法 A：手工判断后创建（最简单）**

```sql
-- 1. 先查
SHOW INDEX FROM room_info WHERE Key_name = 'idx_room_apartment';

-- 2. 没有结果再执行
CREATE INDEX idx_room_apartment
    ON room_info (apartment_id, is_deleted, is_release);
```

**做法 B：存储过程封装（脚本重复执行时省事）**

```sql
DELIMITER $$

CREATE PROCEDURE add_index_if_not_exists(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_columns VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (SELECT 1
                   FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = p_table
                     AND INDEX_NAME = p_index) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD INDEX `', p_index, '` (', p_columns, ')');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL add_index_if_not_exists('room_info', 'idx_room_apartment',
                             'apartment_id, is_deleted, is_release');
CALL add_index_if_not_exists('lease_agreement', 'idx_agreement_apartment',
                             'apartment_id, is_deleted, status');
```

用完可以删掉过程，避免留在业务库里：

```sql
DROP PROCEDURE IF EXISTS add_index_if_not_exists;
```

**做法 C：迁移工具（正式项目推荐）**

用 Flyway / Liquibase 管理 DDL，每个版本一个脚本，工具自带「已执行记录表」，天然幂等，不需要手工判断。例如 Flyway 的 `V2__add_apartment_query_indexes.sql`：

```sql
CREATE INDEX idx_room_apartment
    ON room_info (apartment_id, is_deleted, is_release);
CREATE INDEX idx_agreement_apartment
    ON lease_agreement (apartment_id, is_deleted, status);
```

已执行过的版本不会重复执行。学习项目暂时用做法 A/B 即可。

### 7.4 本项目建议创建的索引

```sql
-- 房间统计：按公寓分组，同时过滤未删除、已发布
CREATE INDEX idx_room_apartment
    ON room_info (apartment_id, is_deleted, is_release);

-- 合同统计：按公寓分组，同时过滤未删除、有效状态
CREATE INDEX idx_agreement_apartment
    ON lease_agreement (apartment_id, is_deleted, status);

-- 公寓列表的地域过滤（可选，视查询条件而定）
CREATE INDEX idx_apartment_region
    ON apartment_info (province_id, city_id, district_id, is_deleted);
```

列顺序的依据：

1. **等值条件在前，范围条件在后**。这条查询里 `apartment_id`、`is_deleted`、`is_release` 都是等值匹配。
2. **`apartment_id` 放最左**，因为 `GROUP BY apartment_id` 和 JOIN 都以它为条件，能同时服务过滤和分组。
3. InnoDB 的二级索引会自动带上主键 `id`，不需要显式写进索引列。

关于 `idx_apartment_region` 的一个现实问题：三个地域条件是**可选**的，用户可能只传 `cityId`。复合索引遵循最左前缀，只传 `cityId` 时用不上 `(province_id, city_id, ...)`。如果「只按城市查」很常见，可以：

- 为 `city_id`、`district_id` 单独建索引；或
- 表数据量不大时接受全表扫描，不建这个索引。

不要为了「看起来完整」盲目加索引，写入和维护成本也会增加。

### 7.5 实体层怎么体现索引

本项目实体是 MyBatis-Plus 的 `@TableName`，**它没有索引注解**，索引只存在于数据库 DDL 中。所以：

- 不要试图在实体上加「索引注解」，MP 不会解析；
- 索引脚本统一放在 `docs/mysql/` 或迁移目录，跟建表脚本一起维护；
- 实体字段与索引列的对应关系靠命名和文档保证。

如果项目用的是 JPA，可以声明在实体上：

```java
@Entity
@Table(name = "room_info",
       indexes = {
           @Index(name = "idx_room_apartment",
                  columnList = "apartment_id, is_deleted, is_release")
       })
public class RoomInfo { ... }
```

注意 JPA 的 `@Index` 也只在 `hibernate.hbm2ddl.auto` 生成表结构时生效，生产环境仍应以迁移脚本为准。

### 7.6 创建后验证

```sql
-- 1. 确认索引存在、列顺序正确
SHOW INDEX FROM room_info WHERE Key_name = 'idx_room_apartment';

-- 2. 确认执行计划走了索引
EXPLAIN
SELECT apartment_id, COUNT(*)
FROM room_info
WHERE is_deleted = 0 AND is_release = 1
GROUP BY apartment_id;
```

关注：

| 字段 | 期望值 |
|---|---|
| `type` | `ref` 或 `range`，不应是 `ALL` |
| `key` | `idx_room_apartment` |
| `rows` | 显著小于表总行数 |
| `Extra` | 尽量不出现 `Using temporary`，理想情况出现 `Using index`（覆盖索引） |

MySQL 8 还可看实际耗时：

```sql
EXPLAIN ANALYZE SELECT ...;
```

### 7.7 创建索引的注意事项

1. **大表加索引用 Online DDL**。MySQL 8 默认 `ALGORITHM=INPLACE, LOCK=NONE`，一般不阻塞读写，但仍有 IO 开销，建议低峰执行：

   ```sql
   ALTER TABLE room_info
       ADD INDEX idx_room_apartment (apartment_id, is_deleted, is_release),
       ALGORITHM = INPLACE, LOCK = NONE;
   ```

2. **先查重复索引**。已有 `(apartment_id)` 时，再建 `(apartment_id, is_deleted)` 会让前者冗余，因为最左前缀已覆盖，建议直接替换。
3. **命名规范统一**：普通索引 `idx_表_列`，唯一索引 `uk_表_列`，便于排查。
4. **索引不是越多越好**：每个索引都会增加写入成本、占用空间，还会影响优化器选择。只给高频查询条件建索引。
5. **DDL 与代码一起版本化**：把索引脚本提交到仓库，避免换机器、换环境后漏建。

## 八、如何验证简化前后一致

1. **数据对比**：同一组筛选条件分别执行新旧 SQL，对比 `total_room_count`、`free_room_count`。
2. **执行计划**：

   ```sql
   EXPLAIN ANALYZE SELECT ...;
   ```

   关注是否出现 `Using temporary`、`Using filesort`，以及子查询是否走索引。
3. **接口验证**：

   ```bash
   curl 'http://127.0.0.1:8080/admin/apartment/pageItem?current=1&size=10&provinceId=110000'
   ```

   检查 `records` 中的 `totalRoomCount`、`freeRoomCount` 是否与数据库手工统计一致。
4. **分页 total**：确认 MyBatis-Plus 返回的 `total` 与实际符合条件的公寓数一致，尤其是采用 CTE 写法时。

## 九、补充：CTE 与 OGNL 详解

### 9.1 CTE（Common Table Expression，公用表表达式）

CTE 是给子查询起个名字，让它在当前 SQL 中像临时表一样被引用。**MySQL 8.0 起支持**，5.7 不支持；PostgreSQL、Oracle、SQL Server 也都有该特性。

最小示例：

```sql
WITH dept_count AS (
    SELECT dept_id, COUNT(*) AS cnt
    FROM employee
    GROUP BY dept_id
)
SELECT d.id, d.name, IFNULL(dc.cnt, 0) AS employee_count
FROM department d
LEFT JOIN dept_count dc ON d.id = dc.dept_id;
```

等价于把 `dept_count` 原样塞进 `LEFT JOIN (...)`，只是可读性更好。

特点：

- 只对紧跟其后的那一条语句有效，不是视图，不落盘；
- 可以定义多个，用逗号分隔，后面的 CTE 可以引用前面的；
- 支持递归 `WITH RECURSIVE`，常用于树形结构或生成序列；
- 性能上 MySQL 优化器通常会内联展开，**不保证更快**，主要收益是可读性。

递归示例（生成 1~5）：

```sql
WITH RECURSIVE nums AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM nums WHERE n < 5
)
SELECT * FROM nums;   -- 1,2,3,4,5
```

对应公寓查询：

```sql
WITH room_count AS (
    SELECT apartment_id, COUNT(*) AS count_tc
    FROM room_info
    WHERE is_deleted = 0 AND is_release = 1
    GROUP BY apartment_id
),
lease_count AS (
    SELECT apartment_id, COUNT(*) AS count_lc
    FROM lease_agreement
    WHERE is_deleted = 0 AND status IN (2, 5)
    GROUP BY apartment_id
)
SELECT ai.id, ai.name,
       IFNULL(rc.count_tc, 0) AS total_room_count,
       IFNULL(rc.count_tc, 0) - IFNULL(lc.count_lc, 0) AS free_room_count
FROM apartment_info ai
LEFT JOIN room_count rc ON ai.id = rc.apartment_id
LEFT JOIN lease_count lc ON ai.id = lc.apartment_id
WHERE ai.is_deleted = 0;
```

与派生表版本的唯一区别是「把子查询提前命名」，逻辑完全一致。用于 MyBatis-Plus 分页前，仍需验证 count SQL 的 total 是否准确（见第五节）。

### 9.2 OGNL（Object-Graph Navigation Language）

MyBatis 动态 SQL 的 `test` 属性使用 **OGNL** 表达式（Apache OGNL），不是 Spring EL/SpEL。它按 JavaBean 属性路径取值，再做布尔判断。

最小示例：

```xml
<select id="list" resultType="ApartmentInfo">
    SELECT * FROM apartment_info
    <where>
        is_deleted = 0
        <if test="provinceId != null">
            AND province_id = #{provinceId}
        </if>
        <if test="name != null and name != ''">
            AND name LIKE CONCAT('%', #{name}, '%')
        </if>
        <if test="ids != null and ids.size() > 0">
            AND id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
        </if>
        <if test="status == @org.twiglau.lease.model.enums.ItemType@APARTMENT">
            AND item_type = 1
        </if>
    </where>
</select>
```

常用语法：

| 场景 | 写法 |
|---|---|
| 取属性 | `queryVo.provinceId`（自动调用 `getProvinceId()`） |
| 调方法 | `name.length()`、`ids.size()` |
| 逻辑运算 | `and`、`or`、`not`（也可 `&&`、`\|\|`、`!`） |
| 比较 | `==`、`!=`、`>`、`<`、`>=`、`<=` |
| 静态访问 | `@类全名@常量或方法`，如 `@java.lang.Math@max(1,2)` |
| 三元表达式 | `flag == 1 ? 'a' : 'b'` |

常见坑：

1. **数字类型不要与空字符串比较**。`Long id` 写 `id != ''` 会抛 `NumberFormatException`，只判 `null` 即可：

   ```xml
   <if test="id != null">
   ```

   字符串才需要 `!= null and != ''`。

2. **OGNL 中单引号单字符是 `char`**，与 String 比较会不相等。比较字符串用双引号，外层属性用单引号：`test='status == "1"'`。
3. **属性不存在会抛异常**，不是返回 `null`，`test` 里不能引用 VO 中不存在的字段。
4. **参数对象本身为 null 时**，`queryVo.provinceId` 会抛 OGNL 异常。Spring MVC 绑定的 POJO(Plain Old Java Object) 参数一般非 null，但 MyBatis 直接传 `null` 的接口要注意。
5. **`getProvinceId()` 与 `provinceId` 等价**，OGNL 会调用 getter；写属性名更简洁，因此推荐 `queryVo.provinceId != null`。

### 9.3 两者的关系

- **CTE 属于 SQL 层**，解决子查询命名的可读性问题；
- **OGNL 属于 MyBatis 层**，解决「根据参数动态拼接 SQL」的问题；
- 两者互不相关，只是刚好同时出现在这条公寓查询中。

## 十、结论

- **推荐**：去掉最外层 `ai` 派生表，过滤条件下推，保留两个聚合子查询；这是改动最小、风险最低、可读性提升明显的方案。
- **谨慎**：CTE 写法更漂亮，但要验证分页 count SQL；标量子查询适合大数据量下的分页场景，需实测。
- **不要**：直接三表 JOIN + `GROUP BY`，一对多会放大行数，必须 `COUNT(DISTINCT)`，性能和可维护性都更差。
