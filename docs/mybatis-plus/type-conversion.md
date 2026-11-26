# 类型转换

如下所示，接口功能是根据 type (公寓/房间),查询标签列表。由于这个 type 字段在数据库，实体类，前后端交互
的过程中有多种不同的形式，因此在请求和响应的过程中，type 字段会涉及到多次类型转换。

- SpringMVC中的`WebDataBinder`组件负责将HTTP的请求参数绑定到Controller方法的参数，并实现参数类型的转换；
- Mybatis中的`TypeHandler`用于处理Java中的实体对象与数据库之间的数据类型转换。


```java
import javax.xml.transform.Result;
import java.util.List;

public Result<List<LabelInfo>> labelList(@RequestParam(required = false) ItemType type) {
    LambdaQueryWrapper<LabelInfo> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(type != null, LabelInfo::getType, type);

    List<LabelInfo> list = service.list(queryWrapper);

    return Result.ok(list);
}
```

## type字段的各种形式

- 数据库中
数据库中的type字段为tinyint类型

- 实体类
实体类中的type字段为 ItemType 枚举类型

- 前后端交互中
前后端交互所传递的数据中 type 字段为 数字(1/2).

## 流程分析

### 请求流程

```text
                                                         |                  SpringMVC                 |
                                                         |                                            |
           select * from label_info      MybatisPlus     |    Controller方法参数                       |            前端
 数据库 <-  where type=1              <-  TypeHandler  <- |  @RequestParam ItemType <-WebDataBinder <- | <- /admin/label/list?type=1
                                                         |    ItemType.APARTMENT                      |
```

- 说明
 > 1. SpringMVC中的WebDataBinder组件负责将HTTP的请求参数绑定到Controller方法的参数，并实现参数类型的转换。
 > 2. Mybatis中的TypeHandler用于处理Java中的实体对象与数据库之间的数据类型转换。

### 响应流程

```text
                                                          SpringMVC
                                         |   Controller方法返回值                                         |
                                         |   @ResponseBody LabelInfo                                    |     前端
         type | name     MybatisPlus     |   {                               -> HTTPMessageConverter -> |    {
数据库 ->  1   | 健身放 -  TypeHandler ->  |       "type":ItemType.APARTMENT,                             |        "type": "1",
                                         |       "name": "健身房"                                        |        "name": "健身房"
                                         |    }                                                         |     }
```

- 说明
  > 1. SpringMVC中的`HTTPMessageConverter`组件负责将Controller方法的返回值(Java对象)转换为HTTP响应体中的JSON字符串，
  > 2. 或者将请求体中的JSON字符串转换为Controller方法中的参数(Java对象)。


- 保存或更新标签信息

```text
                                                                    SpringMVC
                                                       |      Controller方法参数                                       |             前端
                                                       |     @RequestBody LabelInfo                                   |     /admin/label/saveOrUpdate
             insert into               MybatisPlus     |     {                                                        |     {
数据库 <- label_info(type,name)  <-    TypeHandler <- - |        "type":ItemType.APARTMENT,  <- HTTPMessageCoverter <- | <-     "type": "1",
         values(1, "健身房")                            |        "name": "健身房"                                       |        "name": "健身房"
                                                       |      }                                                       |      }
```

## 每个环节的类型转换如何实现

### TypeHandler枚举类型转换
Mybatis中枚举类型的默认转换规则是枚举对象实例(ItemType.APARTMENT) 和 实例名称("APARTMENT")相互映射。若像实现编码(code属性)
到枚举对象实例的相互映射，需要自定义TypeHandler。

不过MybatisPlus提供了一个「通用的处理枚举类型的TypeHandler」。其使用十分简单，只需在ItemType枚举类的code属性上增肌一个注解@EnumValue,
Mybatis-Plus便可以完成从ItemType对象到code属性之间的相互映射。具体配置如下。

```java
public enum ItemType {
    APARTMENT(1, "公寓"),
    ROOM(2, "房间");
    
    @EnumValue
    private Integer code;
    private String name;
    
    ItemType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
```

### HTTPMessageConverter枚举类型转换
HttpMessageConverter依赖于json序列化框架(默认使用Jackson)。其对枚举类型的默认处理规则也是枚举对象实例(ItemType.APARTMENT) 和
实例名称("APARTMENT")相互映射。不过其提供了一个注解`@JsonValue`,同样只需在ItemType枚举类的code属性上增加一个注解@JsonValue,
Jackson便可完成从ItemType对象到code属性之间的互相映射。具体配置如下：

```java
@Getter
public enum ItemType {
    APARTMENT(1, "公寓"),
    ROOM(2, "房间");
    
    @EnumValue
    @JsonValue
    private Integer code;
    private String name;
    
    ItemType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
```





### WebDataBinder枚举类型转换
WebDataBinder 依赖于 Converter 实现类型转换，若Controller方法声明的@RequestParam参数的类型不是String,WebDataBinder
就会自动进行数据类型转换。SpringMVC提供了常用类型的转换器，例如String到Integer,String到Date,String到Boolean等等，其中
也包括String到枚举类型，但是String到枚举类型的默认转换规则是根据实例名称("APARTMENT")转换为枚举对象实例(ItemType.APARTMENT).
若想实现code属性到枚举对象实例的转换，需要自定义Converter.

```java
public class StringToItemTypeConverter implements Coverter<String, ItemType> {
    @Override
    public ItemType covert(String code) {
        for(ItemType val : ItemType.values()) {
            if(val.getCode().equals(Integer.valueOf(code))) {
                return val;
            }
        }
        throw new IllegalArgumentException("code 非法");
    }
}
```


但是我们有很多的枚举类型需要考虑类型转换这个问题，按照上述思路，我们需要为每个枚举类型都定义一个Converter,并且每个
Converter的转换逻辑都完全相同。针对这种情况，考虑官方提供的第二种方案，使用`ConverterFactory`,这个接口可以将
同一个转换逻辑应用一个接口的所有实现类，因此我们定义一个BaseEnum接口，然后另所有的枚举类都实现该接口，然后就可以
自定义ConverterFactory,集中编写写各枚举类的转换逻辑了。

在model模块定义 org.twiglau.lease.model.enums.BaseEnum 接口

```java
public interface BaseEnum {
    Integer getCode();
    String getName();
}
```

令所有org.twiglau.lease.model.enums 包下的枚举类都实现 BaseEnum 接口

在web-admin模块自定义org.twiglau.lease.web.admin.custom.converter.StringToBaseEnumConverterFactory

```java
@Component
public class StringToBaseEnumConverterFactory implements ConverterFactory<String, BaseEnum> {
    @Override
    public <T extends BaseEnum> Converter<String, T> getConverter(Class<T> targetType) {
        return new Converter<String, T>() {
            @Override
            public T convert(String source) {
                for (T enumConstant : targetType.getEnumConstants()) {
                    if(enumConstant.getCode().equals(Integer.valueOf(source))) {
                        return enumConstant;
                    }
                }
                
                throw new IllegalArgumentException("非法的枚举值: " + source);
            }
        };
    }
}
```













































