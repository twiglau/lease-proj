# MyBatis Plus
MyBatis Plus(简称MP) 是一个 MyBatis的增强工具，在MyBatis的基础上只做增强不做改变，为简化开发，提高效率而生。

## 特性

- 无侵入：只做增强，不做改变。引入它不会对现有工程产生影响
- 强大的CRUD操作：内置通用Mapper,通用Service,提供了大量的通用 CURD 方法。
- 条件构造器：提供了强大的条件构造器，可以构造各种复杂的查询条件，以应对各种复杂查询。
- 内置分页插件: 配置好插件之后，写分页等同于普通List查询，无需关注分页逻辑。


## 通用Mapper

通用Mapper提供了通用的CRUD方法，使用它可以省去大量编写简单重复的SQL语句的工作。

### 创建Mapper接口

创建UserMapper接口，并继承由Mybatis Plus提供的 BaseMapper<T> 接口，

```java
@Mapper
public interface UserMapper extends BaseMapper<User> {}
```

**注意**

若Mapper接口过多，可不用逐一配置`@Mapper`注解，而使用`@MapperScan`注解指定扫描路径进行统一管理。

```java
@SpringBootApplication
@MapperScan("com.twiglau.example.mapper")
public class HelloMpApplication {
    public static void main(String[] args) {
        SpringApplication.run(HelloMpApplication.class, args);
    }
}
```

## 通用Service

通用Service进一步封装了通用Mapper的CRUD方法，并提供了例如 saveOrUpdate, saveBatch等高级方法。

### 创建Service接口

```java
public interface UserService extends IService<User> {}
```

### 创建Service实现类

```java
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {}
```

## 条件构造器

条件构造器用于构造复杂的查询条件，例如获取 name='zhangsan' 的用户。MyBatis Plus 共提供了两类构造器。
分别是 `QueryWrapper` 和 `UpdateWrapper`. 其中 `QueryWrapper` 主要用于查询，删除操作，`UpdateWrapper`
主要用于更新操作。

```java
@SpringBootTest
public class WrapperTest {
    @Autowired
    private UserService userService;
    
    @Test
    public void testQueryWrapper() {
        // 查询name=Tom的所有用户
        QueryWrapper<User> queryWrapper1 = new QueryWrapper<>();
        queryWrapper1.eq("name", "Tom");
        
        // 查询所有用户信息并按照age字段降序排序
        QueryWrapper<User> queryWrapper3 = new QueryWrapper<>();
        queryWrapper3.orderByDesc("age");
    }
}
```


### 上述的QueryWrapper和UpdateWrapper均有一个Lambda版本，也就是LambdaQueryWrapper和LambdaUpdateWrapper,

Lambda版本的优势在于，可以省去字段名的硬编码。

```java
import java.util.List;

@SpringBootTest
public class LambdaWrapperTest {
    @Autowired
    private UserService userService;

    @Test
    public void testLambdaQueryWrapper() {
        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(User::getName, "Tom");

        List<User> list = userService.list(lambdaQueryWrapper);
        list.forEach(System.out::println);
    }
    
    @Test
    public void testLambdaUpdateWrapper() {
        LambdaUpdateWrapper<User> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(User::getName, "Tom");
        lambdaUpdateWrapper.set(User::getEmail, "tom@gmail.com");
        
        userService.update(lambdaUpdateWrapper);
    }
}
```


## 分页插件

分页查询时一个很常见的需求，故 Mybatis Plus 提供了一个分页插件，使用它可以十分方便的完成分页查询。

### 配置分页插件

- 创建com.twiglau.hellomp.config.MPConfiguration配置类。
```java
@Configuration
public class MPConfiguration {
    
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        
        return interceptor;
    }
}
```

### 分页插件使用说明

1. 构造分页对象：(records, total, size, current)

分页对象即作为分页查询的参数，也作为分页查询的返回结果；当作为查询参数时，
通常只需提供 current 和 size 属性。

```java
IPage<T> page = new Page<>(current, size);
```

注意： IPage为分页接口， Page为IPage接口的一个实现类。

2. 分页查询

Mybatis Plus 的 `BaseMapper`和`ServiceImpl`均提供了常用的分页查询的方法，

- BaseMapper的分页查询

`IPage<T> selectPage(IPage<T> page, Wrapper<T> queryWrapper);`

- ServiceImpl的分页查询：

```java
//无条件分页查询
IPage<T> page(IPage<T> page);
//条件分页查询
IPage<T> page(IPage<T> page, Wrapper<T> queryWrapper);
```

- 自定义Mapper

对于自定义SQL,也可以十分方便的完成分页查询；如下

Mapper接口：
`IPage<UserVo> selectPageVo(IPage<?> page, Integer state);`

Mapper.xml:
```xml
<select id="selectPageVo" resultType="xxx.xxx.xxx.UserVo">
    SELECT id,name FROM user WHERE state=#{state}
</select>
```

注意： Mapper.xml 中的SQL只需实现查询list的逻辑即可，无需关注分页的逻辑。



在UserMapper中声明分页查询方法如下 `IPage<User> selectUserPage(IPage<User> page);`

创建`resources/mapper/UserMapper.xml`文件，内容如下

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.twiglau.hellomp.mapper.UserMapper">
    <select id="selectUserPage" resultType="com.twiglau.hellomp.entity.User">
        select * from user
    </select>
</mapper>
```

注意：

Mybatis-Plus中Mapper.xml文件路径默认为：`classpath*:/mapper/**/*.xml`,可在application.yml中
配置以下参数进行修改：
```yaml
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
```






























