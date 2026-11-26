package org.twiglau.lease.web.admin.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.twiglau.lease.model.entity.LeaseAgreement;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.twiglau.lease.web.admin.vo.agreement.AgreementQueryVo;
import org.twiglau.lease.web.admin.vo.agreement.AgreementVo;

/**
 * <p>
 * 租约信息表 Mapper 接口
 * </p>
 *
 * @author zjw
 * @since 2024-06-05
 */
public interface LeaseAgreementMapper extends BaseMapper<LeaseAgreement> {

    IPage<AgreementVo> pageAgreementByQuery(IPage<AgreementVo> page, AgreementQueryVo queryVo);
}
