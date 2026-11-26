package org.twiglau.helloknife4j.entity;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户信息实体")
public class User {
    @Schema(description = "用户ID")
    private String id;
    @Schema(description = "用户名称")
    private String name;
    @Schema(description = "用户邮箱")
    private String email;
    @Schema(description = "用户年龄")
    private Integer age;
}
