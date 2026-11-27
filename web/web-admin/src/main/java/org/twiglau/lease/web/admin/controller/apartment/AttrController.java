package org.twiglau.lease.web.admin.controller.apartment;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.twiglau.lease.result.Result;
import org.twiglau.lease.model.entity.AttrKey;
import org.twiglau.lease.model.entity.AttrValue;
import org.twiglau.lease.web.admin.service.IAttrKeyService;
import org.twiglau.lease.web.admin.service.IAttrValueService;
import org.twiglau.lease.web.admin.vo.attr.AttrKeyVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 房间属性管理
 * </p>
 *
 * @author zjw
 * @since 2024-06-05
 */
@Tag(name = "房间属性管理")
@RestController
@RequestMapping("/admin/attr")
@RequiredArgsConstructor
public class AttrController {

    private final IAttrKeyService attrKeyService;

    private final IAttrValueService attrValueService;

    @Operation(summary = "新增或更新属性名称")
    @PostMapping("key/saveOrUpdate")
    public Result<Void> saveOrUpdateAttrKey(@RequestBody AttrKey attrKey) {
        attrKeyService.saveOrUpdate(attrKey);
        return Result.ok();
    }

    @Operation(summary = "新增或更新属性值")
    @PostMapping("value/saveOrUpdate")
    public Result<Void> saveOrUpdateAttrValue(@RequestBody AttrValue attrValue) {
        attrValueService.saveOrUpdate(attrValue);
        return Result.ok();
    }


    @Operation(summary = "查询全部属性名称和属性值列表")
    @GetMapping("list")
    public Result<List<AttrKeyVo>> listAttrInfo() {
        List<AttrKeyVo> attrKeyVoList = attrKeyService.listAttrInfo();
        return Result.ok(attrKeyVoList);
    }

    @Operation(summary = "根据id删除属性名称")
    @DeleteMapping("key/deleteById")
    public Result<Void> removeAttrKeyById(@RequestParam Long attrKeyId) {
        attrKeyService.removeById(attrKeyId);
        // 删除属性名称对应的属性值
        LambdaQueryWrapper<AttrValue> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(AttrValue::getAttrKeyId, attrKeyId);
        attrValueService.remove(lambdaQueryWrapper);
        return Result.ok();
    }

    @Operation(summary = "根据id删除属性值")
    @DeleteMapping("value/deleteById")
    public Result<Void> removeAttrValueById(@RequestParam Long id) {
        attrValueService.removeById(id);
        return Result.ok();
    }

}
