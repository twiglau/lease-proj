package org.twiglau.lease.web.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.twiglau.lease.model.entity.ViewAppointment;
import com.baomidou.mybatisplus.extension.service.IService;
import org.twiglau.lease.model.enums.AppointmentStatus;
import org.twiglau.lease.web.admin.vo.appointment.AppointmentQueryVo;
import org.twiglau.lease.web.admin.vo.appointment.AppointmentVo;

/**
 * <p>
 * 预约看房信息表 服务类
 * </p>
 *
 * @author zjw
 * @since 2024-06-05
 */
public interface IViewAppointmentService extends IService<ViewAppointment> {

    /**
     * 根据id更新预约状态
     * @param id 预约信息id
     * @param status 预约状态
     */
    void updateStatusById(Long id, AppointmentStatus status);

    /**
     * 分页查询预约信息
     * @param page 分页对象
     * @param queryVo 查询条件
     * @return 预约信息列表
     */
    IPage<AppointmentVo> pageAppointmentVo(IPage<AppointmentVo> page, AppointmentQueryVo queryVo);
}
