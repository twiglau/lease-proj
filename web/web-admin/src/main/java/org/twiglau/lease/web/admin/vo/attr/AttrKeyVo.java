package org.twiglau.lease.web.admin.vo.attr;

import lombok.EqualsAndHashCode;
import org.twiglau.lease.model.entity.AttrKey;
import org.twiglau.lease.model.entity.AttrValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;


@EqualsAndHashCode(callSuper = true)
@Data
public class AttrKeyVo extends AttrKey {

    @Schema(description = "属性value列表")
    private List<AttrValue> attrValueList;
}
