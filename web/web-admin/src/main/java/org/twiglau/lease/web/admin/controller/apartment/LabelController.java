package org.twiglau.lease.web.admin.controller.apartment;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.twiglau.lease.common.result.Result;
import org.twiglau.lease.model.entity.LabelInfo;
import org.twiglau.lease.model.enums.ItemType;
import org.twiglau.lease.web.admin.service.ILabelInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 标签管理
 * </p>
 *
 * @author zjw
 * @since 2024-06-05
 * @AllArgsConstructor 作用：
 * 1. 是 Lombok 注解，作用是在编译期自动生成一个包含所有字段的构造方法；
 * 2. 这个类只有一个字段: private final ILabelInfoService labelService;
 * 3. Lombok 生成的代码等价于:
 *    public LabelController(ILabelInfoService labelService) {
 *        this.labelService = labelService;
 *    }
 * 4. 解决两件事：
 * - 依赖注入：Spring 创建Controller时，会调用这个构造方法把 ILabelInfoService 传进来，效果和写@Autowired
 *   一样。Spring 4.3 之后，类只有一个构造方法时自动用它注入，不需要额外注解。
 * - 字段可以声明为final: 保证依赖在对象创建后不可变，避免空指针，也方便单元测试时手动 new LabelController(mockService).
 * --------------------------------------------------------
 * 几个容易混淆的点：
 * - 它不是Spring注解，是Lombok的；删掉它之后，final字段没有初始化，编译会直接报错。
 * ------------------
 * - @AllArgsConstructor 会给所有字段生成参数。如果以后加了非 final 且不需要注入的字段，它也会被塞进
 *   构造方法。这种场景更适合换成@RequiredArgsConstructor, 只给 final 和 @NonNull 字段生成参数。
 * ------------------
 * - 本类只有一个字段，所以 @AllArgsConstructor 和 @RequiredArgsConstructor 生成的代码完全一样。项目
 *   里其他Controller也是同样的用法。
 */
@Tag(name = "标签管理")
@RestController
@RequestMapping("/admin/label")
@RequiredArgsConstructor
public class LabelController {

    private final ILabelInfoService labelService;

    @Operation(summary = "（根据类型）查询标签列表")
    @GetMapping("list")
    public Result<List<LabelInfo>> labelList(
            @Parameter(
                    description = "标签类型: 1-公寓，2-房间；不传则查询全部",
                    example = "1",
                    required = false
            )
            @RequestParam(required = false)
            ItemType type)
    {
        LambdaQueryWrapper<LabelInfo> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        // 如不为空，添加过滤条件
        lambdaQueryWrapper.eq(type != null , LabelInfo::getType, type);
        return Result.ok(labelService.list(lambdaQueryWrapper));
    }

    /**
     * @RequestBody 要求请求体的媒体类型能被某个 HttpMessageConverter处理，
     * 前端设置 Content-Type: application/json 就是告诉 Spring 用 Jackson 那个转换器。
     *
     * 流程是： Content-Type -> 选择 HttpMessageConverter -> 读body -> 反序列化成LabelInfo.
     * 没有这个头，或者头不对，转换器选不中，就会失败。
     * @param labelInfo
     * @return
     */

    @Operation(summary = "新增或修改标签信息")
    @PostMapping("saveOrUpdate")
    public Result<Void> saveOrUpdateLabel(@RequestBody LabelInfo labelInfo) {
        labelService.saveOrUpdate(labelInfo);
        return Result.ok();
    }

    @Operation(summary = "根据id删除标签信息")
    @DeleteMapping("deleteById")
    public Result<Void> deleteLabelById(@RequestParam Long id) {
        labelService.removeById(id);
        return Result.ok();
    }
}
