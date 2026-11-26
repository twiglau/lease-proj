package org.twiglau.lease.web.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.twiglau.lease.model.entity.AttrKey;
import org.twiglau.lease.web.admin.mapper.AttrKeyMapper;
import org.twiglau.lease.web.admin.service.IAttrKeyService;
import org.twiglau.lease.web.admin.vo.attr.AttrKeyVo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 房间基本属性表 服务实现类
 * </p>
 *
 * @author zjw
 * @since 2024-06-05
 */
@Service
@AllArgsConstructor
public class AttrKeyServiceImpl extends ServiceImpl<AttrKeyMapper, AttrKey> implements IAttrKeyService {

    private final AttrKeyMapper attrKeyMapper;

    @Override
    public List<AttrKeyVo> listAttrInfo() {
        return attrKeyMapper.listAttrInfo();
    }

    /**
     * 根据attrKeyIdList查询attrKeyMap
     * @param attrKeyIdList attrKeyId集合
     * @return attrKeyMap
     */
    @Override
    public Map<Long, AttrKey> selectAttrKeyMapByAttrKeyIdList(List<Long> attrKeyIdList) {
        return baseMapper.selectBatchIds(attrKeyIdList)
                .stream()
                .collect(Collectors.toMap(AttrKey::getId, attrKey -> attrKey));
    }
}
