package org.twiglau.lease.web.admin.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.twiglau.lease.model.entity.ViewAppointment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.twiglau.lease.web.admin.vo.appointment.AppointmentQueryVo;
import org.twiglau.lease.web.admin.vo.appointment.AppointmentVo;

/**
 * <p>
 * 预约看房信息表 Mapper 接口
 * </p>
 *
 * @author zjw
 * @since 2024-06-05
 */
public interface ViewAppointmentMapper extends BaseMapper<ViewAppointment> {

    IPage<AppointmentVo> pageAppointmentVo(IPage<AppointmentVo> page, AppointmentQueryVo queryVo);
}
