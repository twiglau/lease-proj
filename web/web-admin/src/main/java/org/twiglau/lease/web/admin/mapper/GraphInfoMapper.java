package org.twiglau.lease.web.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.twiglau.lease.model.entity.GraphInfo;
import org.twiglau.lease.model.enums.ItemType;
import org.twiglau.lease.web.admin.vo.graph.GraphVo;

import java.util.List;

/**
 * <p>
 * 图片信息表 Mapper 接口
 * </p>
 *
 * @author zjw
 * @since 2024-06-05
 */
public interface GraphInfoMapper extends BaseMapper<GraphInfo> {

    List<GraphVo> selectListByItemTypeAndId(ItemType itemType, Long itemId);
}
