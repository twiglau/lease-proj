# 保存或更新
在PaymentTypeController中增加如下内容
```java
@Operation(summary = "保存或更新支付方式")
@PostMapping("saveOrUpdate")
public Result saveOrUpdatePaymentType(@RequestBody PaymentType paymentType) {
    service.saveOrUpdate(paymentType);
    return Result.ok();
}
```

## 保存或更新数据时，前端通常不会传入 isDeleted, createTime, updateTime 这三个字段，因此需手动赋值。但是数据库中几乎每张表都有上述字段，
所以手动去赋值就显得有些繁琐。

- `is_deleted`字段：可将数据库中该字段的默认值设置为0.
- `create_time`和`update_time`:可使用mybatis-plus的自动填充功能，所谓自动填充，就是通过统一配置，在插入或更新数据时，
  自动为某些字段赋值。
   - 为相关字段配置触发填充的时机：
   - `create_time` 需要在插入数据时填充；
   - `update_time` 需要在更新数据时填充;
   - 观察 @TableField 注解中的fill属性；

```java
@Data
public class BaseEntity {
    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @Schema(description = "创建时间")
    @JsonIgnore
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;
    
    @Schema(description = "更新时间")
    @JsonIgnore
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private Date updateTime;
    
    @Schema(description = "逻辑删除")
    @JsonIgnore
    @TableLogic
    @TableField("is_deleted")
    private Byte isDeleted;
}
```


## 配置自动填充的内容，具体配置如下
在common模块下创建 org.twiglau.lease.common.mybatisplus.MybatisMetaObjectHandler类，内容如下

```java
@Component
public class MybatisMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
```