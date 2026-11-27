package org.twiglau.lease.web.admin.vo.apartment;


import org.twiglau.lease.model.entity.ApartmentInfo;
import org.twiglau.lease.web.admin.vo.graph.GraphVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;


@Schema(description = "公寓信息")
@Data
public class ApartmentSubmitVo extends ApartmentInfo {

    @Schema(description="公寓配套ids")
    private List<Long> facilityInfoIds;

    @Schema(description="公寓标签ids")
    private List<Long> labelIds;

    @Schema(description="公寓杂费值ids")
    private List<Long> feeValueIds;

    @Schema(description="公寓图片ids")
    private List<GraphVo> graphVoList;

}
