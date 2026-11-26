package org.twiglau.lease.web.admin.controller.apartment;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Parameter;
import org.twiglau.lease.common.result.Result;
import org.twiglau.lease.model.entity.FacilityInfo;
import org.twiglau.lease.model.enums.ItemType;
import org.twiglau.lease.web.admin.service.IFacilityInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 配套管理
 * </p>
 *
 * @author zjw
 * @since 2024-06-05
 */
@Tag(name = "配套管理")
@RestController
@RequestMapping("/admin/facility")
@AllArgsConstructor
public class FacilityController {

    private final IFacilityInfoService facilityInfoService;

    @Operation(summary = "[根据类型]查询配套信息列表")
    @GetMapping("list")
    public Result<List<FacilityInfo>> listFacility(
            @Parameter(
                    description = "标签类型：1-公寓，2-房间；不传查全部",
                    example = "1",
                    required = false
            )
            @RequestParam(required = false)
            ItemType type
    ) {
        LambdaQueryWrapper<FacilityInfo> lambdaQueryWrapper = new LambdaQueryWrapper<FacilityInfo>()
                .eq(type != null, FacilityInfo::getType, type);
        return Result.ok(facilityInfoService.list(lambdaQueryWrapper));
    }

    @Operation(summary = "新增或修改配套信息")
    @PostMapping("saveOrUpdate")
    public Result<Void> saveOrUpdate(@RequestBody FacilityInfo facilityInfo) {
        facilityInfoService.saveOrUpdate(facilityInfo);
        return Result.ok();
    }

    @Operation(summary = "根据id删除配套信息")
    @DeleteMapping("deleteById")
    public Result<Void> removeFacilityById(@RequestParam Long id) {
        facilityInfoService.removeById(id);
        return Result.ok();
    }

}
