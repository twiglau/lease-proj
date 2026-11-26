# 逻辑删除功能
由于数据库中所有表均采用逻辑删除策略，所以查询数据时均需要增加过滤条件 is_deleted=0.
上述操作虽然不难实现，但是每个查询接口都要考虑到，也显得有些繁琐。为简化上述操作，可以使用Mybatis-Plus提供的
逻辑删除功能，它可以自动为查询操作增加 is_deleted=0过滤条件，并将删除操作转为更新语句。

## 在application.yml中增加如下内容：

```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: flag # 全局逻辑删除的实体字段名(配置后可以忽略不配置步骤...)
      logic-delete-value: 1 # 逻辑已删除值(默认为1)
      logic-not-delete-value: 0 # 逻辑未删除值（默认为 0）
```

## 在实体类中的删除标识字段上增加 @TableLogic 注解

```java
@Data
public class BaseEntity {
  @Schema(description = "主键")
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  
  @Schema(description = "创建时间")
  @JsonIgnore
  private Date createTime;
  
  @Schema(description = "更新时间")
  @JsonIgnore
  private Date updateTime;
  
  @Schema(description = "逻辑删除")
  @JsonIgnore
  @TableLogic
  @TableField("is_deleted")
  private Byte isDeleted;
}
```

## 注意：
逻辑删除功能只对 Mybatis-Plus 自动注入的sql起效，也就是说，对于手动在Mapper.xml文件配置的sql
不会生效，需要单独考虑。