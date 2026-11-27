# Spring MVC 查询参数绑定：从 ApartmentQueryVo 看参数怎么传、由谁处理

> 记录时间：2026-11-27
> 项目环境：JDK 17、Spring Boot 3.0.11（Spring Framework 6.0.12，jakarta 命名空间）、springdoc/Knife4j 4.x
> 相关代码：`ApartmentController#pageItem`、`ApartmentQueryVo`

## 一、场景

```java
@Operation(summary = "根据条件分页查询公寓列表")
@GetMapping("pageItem")
public Result<IPage<ApartmentItemVo>> pageItem(@RequestParam long current,
                                               @RequestParam long size,
                                               ApartmentQueryVo queryVo) {
    IPage<ApartmentItemVo> page = new Page<>(current, size);
    IPage<ApartmentItemVo> list = apartmentInfoService.pageApartmentItemVo(page, queryVo);
    return Result.ok(list);
}
```

```java
@Data
@Schema(description = "公寓查询实体")
public class ApartmentQueryVo {
    private Long provinceId;
    private Long cityId;
    private Long districtId;
}
```

`ApartmentQueryVo` 没有 `@RequestBody`，请求是 GET，所以它走的是 Spring MVC 的 **ModelAttribute 数据绑定**：把 URL 查询参数按属性名逐个 set 进对象。

## 二、前端怎么传

### 2.1 基本形式

所有字段拼在 query string 上，参数名和 VO 属性名完全一致：

```text
GET /admin/apartment/pageItem?current=1&size=10&provinceId=110000&cityId=110100&districtId=110101
```

| URL 参数 | 绑定目标 |
|---|---|
| `current` / `size` | `@RequestParam` 参数 |
| `provinceId` | `ApartmentQueryVo.provinceId` |
| `cityId` | `ApartmentQueryVo.cityId` |
| `districtId` | `ApartmentQueryVo.districtId` |

### 2.2 前端调用示例

```js
// axios
axios.get('/admin/apartment/pageItem', {
  params: { current: 1, size: 10, provinceId: 110000, cityId: 110100 }
});
// 实际请求：/admin/apartment/pageItem?current=1&size=10&provinceId=110000&cityId=110100
```

```bash
# curl
curl 'http://127.0.0.1:8080/admin/apartment/pageItem?current=1&size=10&provinceId=110000'
```

### 2.3 绑定规则与注意事项

1. **参数名必须与字段名一致，大小写敏感**。`provinceid`、`province_id` 都绑不上，值为 `null`。
2. **筛选条件可以不传**，`Long` 没传即为 `null`，SQL 中用 `<if test="...">` 动态拼接。
3. **`current`、`size` 默认必传**。`@RequestParam` 默认 `required = true`，缺失返回 400。可加默认值：

   ```java
   @RequestParam(defaultValue = "1") long current,
   @RequestParam(defaultValue = "10") long size,
   ```

4. **不要传字符串 `"null"`**。`provinceId=` 空值一般能转成 `null`，但 `provinceId=null` 会抛类型转换异常，返回 400。
5. **GET 接口不能靠 JSON body 传参**。若想传 JSON，应改为 POST + `@RequestBody`。
6. **中文字段无需手动编码**，浏览器和 axios 会自动 URL 编码。

### 2.4 让 Swagger 展开字段（springdoc）

springdoc 生成 OpenAPI 文档时，对「无注解的 POJO 参数」默认按**一个对象参数**处理。表现是 Knife4j/Swagger UI 里只出现一个 `queryVo` 输入项，属性折叠在它的 schema 里，调试时需要手填对象结构，很不方便。

展开成独立 query 参数有两种方式：

**方式一：单个参数用 `@ParameterObject`**

```java
import org.springdoc.core.annotations.ParameterObject;

@GetMapping("pageItem")
public Result<IPage<ApartmentItemVo>> pageItem(
        @RequestParam(defaultValue = "1") long current,
        @RequestParam(defaultValue = "10") long size,
        @ParameterObject ApartmentQueryVo queryVo) {
    ...
}
```

**方式二：全局配置 `springdoc.default-flat-param-object`**

```yaml
springdoc:
  default-flat-param-object: true
```

开启后，所有 object 类型的 query/form 参数都会被展开，等价于给每个参数都加了 `@ParameterObject`，不需要逐个标注。

三种情况对比：

| 配置 | 作用范围 | Swagger UI 效果 |
|---|---|---|
| 默认（`false`） | 全局 | 显示一个 `queryVo` 对象参数，字段在 schema 中折叠 |
| `false` + `@ParameterObject` | 单个方法参数 | 该对象的字段展开为 `provinceId`、`cityId` 等独立参数 |
| `true` | 全局 | 所有对象参数默认展开 |

开启后，`pageItem` 在文档中的参数会变成：

```text
current
size
provinceId
cityId
districtId
```

需要注意：

1. **它只影响文档生成和 Swagger UI 展示，不影响运行时绑定**。无论是否展开，前端实际传参始终是扁平的 query 参数（`?provinceId=110000`），Spring MVC 的 ModelAttribute 绑定逻辑不变。
2. **不会改变接口行为**。不加这个配置接口照样能调通，只是文档不好用。
3. **展开只作用一层**。如果 VO 里有嵌套对象，嵌套部分仍按 object 展示，不会递归展开成 `filter.provinceId` 这种形式。
4. **全局开启后没有局部关闭的注解**。个别对象不希望展开时，只能调整参数类型或拆成基本类型参数。
5. **多个对象参数同时展开可能出现同名参数冲突**，此时用 `@ParameterObject` 并显式指定参数名前缀，或避免同时使用多个 POJO 参数。
6. **版本与包名**：本项目 Knife4j 4.4.0 内置 springdoc-openapi 2.x，注解包名是 `org.springdoc.core.annotations.ParameterObject`；springdoc 1.x 的包名是 `org.springdoc.api.annotations.ParameterObject`。升级或降级时注意 import 来源。

如果项目里 POJO 查询参数很多，建议全局开启 `default-flat-param-object: true`；只有个别接口需要时，用 `@ParameterObject` 局部标注更可控。

也可以用 `@ModelAttribute` 显式声明绑定方式，但它只影响 Spring MVC 绑定语义，不改变 Swagger 的展开效果。

## 三、这属于哪块框架的能力

属于 **Spring MVC（spring-webmvc）的「方法参数解析 + 数据绑定」**，与 MyBatis-Plus 无关；Spring Boot 只负责自动配置（`WebMvcAutoConfiguration` 注册默认参数解析器、消息转换器）。

### 3.1 调用链

```text
DispatcherServlet
  └─ RequestMappingHandlerAdapter
      └─ HandlerMethodArgumentResolver
          ├─ RequestParamMethodArgumentResolver       // @RequestParam
          └─ ServletModelAttributeMethodProcessor     // 无注解的复合对象
              └─ WebDataBinder
                  ├─ BeanWrapper（spring-beans）       // 按属性名调 setter
                  └─ ConversionService（spring-core）  // String → Long
```

### 3.2 关键类与所属模块

| 能力 | 模块 | 类 |
|---|---|---|
| 请求分发 | spring-webmvc | `DispatcherServlet` |
| 参数解析 | spring-webmvc | `HandlerMethodArgumentResolver` 及实现 |
| POJO 参数绑定 | spring-webmvc | `ServletModelAttributeMethodProcessor` |
| 属性赋值 | spring-beans | `BeanWrapperImpl` |
| 类型转换 | spring-core | `ConversionService`、`StringToNumberConverterFactory` |
| 数据校验 | spring-context | `DataBinder`、`BindingResult` |

### 3.3 绑定能生效的三个条件

1. 参数是**非简单类型**，且没有 `@RequestBody`、`@RequestParam` 等注解 → 按 ModelAttribute 处理；
2. 类有 **setter**（`@Data` 生成）；
3. 参数名与属性名匹配，且 `ConversionService` 能完成类型转换。

### 3.4 注解归属容易混淆

| 注解 | 所属 | 作用 |
|---|---|---|
| `@RequestParam`、`@ModelAttribute`、`@RequestBody` | Spring MVC | 参与参数绑定 |
| `@ParameterObject` | springdoc | 只影响接口文档展示 |
| `@Data` | Lombok | 编译期生成 getter/setter，绑定生效的前提 |

## 四、常见故障排查

| 现象 | 原因 | 排查 |
|---|---|---|
| VO 字段全为 `null` | 参数名与属性名不一致 | 对比 URL 与字段名 |
| 400 Bad Request | `current`/`size` 缺失，或数值参数传了非数字 | 查看 `MethodArgumentTypeMismatchException` 日志 |
| 400 且提示绑定失败 | 字段类型不匹配，如 `provinceId=abc` | 检查前端传值 |
| Swagger 里只有一个对象参数 | 未加 `@ParameterObject` | 加注解或手写 curl 测试 |
| GET 传 JSON body 无效 | GET 无 body 语义，且无 `@RequestBody` | 改用 query 参数或 POST |

绑定失败抛出的 `MethodArgumentTypeMismatchException`、`BindException` 也由 Spring MVC 产生，通常交给 `@RestControllerAdvice` 统一转换为 `Result` 响应。
