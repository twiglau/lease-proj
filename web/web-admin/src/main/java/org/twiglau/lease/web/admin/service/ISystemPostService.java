package org.twiglau.lease.web.admin.service;

import org.twiglau.lease.model.entity.SystemPost;
import com.baomidou.mybatisplus.extension.service.IService;
import org.twiglau.lease.model.enums.BaseStatus;

/**
 * <p>
 * 岗位信息表 服务类
 * </p>
 *
 * @author zjw
 * @since 2024-06-05
 */
public interface ISystemPostService extends IService<SystemPost> {

    /**
     * 根据id修改状态
     * @param id 岗位id
     * @param status 修改后的状态
     */
    void updateStatusByPostId(Long id, BaseStatus status);
}
