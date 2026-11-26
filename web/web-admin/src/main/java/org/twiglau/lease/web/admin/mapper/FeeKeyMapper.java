package org.twiglau.lease.web.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.twiglau.lease.model.entity.FeeKey;
import org.twiglau.lease.web.admin.vo.fee.FeeKeyVo;

import java.util.List;

/**
 * <p>
 * 杂项费用名称表 Mapper 接口
 * </p>
 *
 * @author zjw
 * @since 2024-06-05
 */
public interface FeeKeyMapper extends BaseMapper<FeeKey> {

    List<FeeKeyVo> feeInfoList();
}
