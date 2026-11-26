package org.twiglau.lease.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = "标签类型")
public enum ItemType implements BaseEnum {

    @Schema(description = "公寓") APARTMENT(1, "公寓"),

    @Schema(description = "房间") ROOM(2, "房间");


    @EnumValue
    @JsonValue
    private final Integer code;
    private final String name;

    @Override
    public Integer getCode() {
        return this.code;
    }


    @Override
    public String getName() {
        return name;
    }

    ItemType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

}
