package org.twiglau.lease.web.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.twiglau.lease.model.entity.GraphInfo;
import org.twiglau.lease.model.enums.ItemType;
import org.twiglau.lease.web.admin.mapper.GraphInfoMapper;
import org.twiglau.lease.web.admin.service.IGraphInfoService;
import org.twiglau.lease.web.admin.vo.graph.GraphVo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 图片信息表 服务实现类
 * </p>
 *
 * @author zjw
 * @since 2024-06-05
 */
@Service
@AllArgsConstructor
public class GraphInfoServiceImpl extends ServiceImpl<GraphInfoMapper, GraphInfo> implements IGraphInfoService {

    private final GraphInfoMapper graphInfoMapper;

    @Override
    public List<GraphVo> selectListByItemTypeAndId(ItemType itemType, Long itemId) {
        return graphInfoMapper.selectListByItemTypeAndId(itemType, itemId);
    }

    /**
     * 根据公寓id删除图片信息
     * @param apartmentId 公寓id
     * @return 删除结果
     */
    @Override
    public Boolean removeByApartmentId(Long apartmentId) {
        LambdaQueryWrapper<GraphInfo> graphInfoQueryWrapper = Wrappers.lambdaQuery(GraphInfo.class)
                .eq(GraphInfo::getItemId, apartmentId)
                .eq(GraphInfo::getItemType, ItemType.APARTMENT);
        return this.remove(graphInfoQueryWrapper);
    }

    /**
     * 根据房间id删除图片信息
     * @param roomInfoId 房间id
     * @return 删除结果
     */
    @Override
    public Boolean removeByRoomInfoId(Long roomInfoId) {
        LambdaQueryWrapper<GraphInfo> graphInfoQueryWrapper = Wrappers.lambdaQuery(GraphInfo.class)
                .eq(GraphInfo::getItemId, roomInfoId)
                .eq(GraphInfo::getItemType, ItemType.ROOM);
        return this.remove(graphInfoQueryWrapper);
    }
}
