package org.twiglau.lease.web.admin.controller.lease;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.twiglau.lease.common.result.Result;
import org.twiglau.lease.model.enums.AppointmentStatus;
import org.twiglau.lease.web.admin.service.IViewAppointmentService;
import org.twiglau.lease.web.admin.vo.appointment.AppointmentQueryVo;
import org.twiglau.lease.web.admin.vo.appointment.AppointmentVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.twiglau.lease.web.admin.vo.room.RoomItemVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 预约看房管理
 * </p>
 *
 * @author zjw
 * @since 2024-06-05
 */
@Tag(name = "预约看房管理")
@RequestMapping("/admin/appointment")
@RestController
@RequiredArgsConstructor
public class ViewAppointmentController {

    private final IViewAppointmentService viewAppointmentService;

    @Operation(summary = "分页查询预约信息")
    @GetMapping("page")
    public Result<IPage<AppointmentVo>> page(@RequestParam long current, @RequestParam long size, AppointmentQueryVo queryVo) {
        IPage<AppointmentVo> page = new Page<>(current, size);
        IPage<AppointmentVo> list = viewAppointmentService.pageAppointmentVo(page, queryVo);
        return Result.ok(list);
    }

    @Operation(summary = "根据id更新预约状态")
    @PostMapping("updateStatusById")
    public Result<String> updateStatusById(@RequestParam Long id, @RequestParam AppointmentStatus status) {
        viewAppointmentService.updateStatusById(id, status);
        return Result.ok();
    }

}
