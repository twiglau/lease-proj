package org.twiglau.helloknife4j.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用 OpenAPI3 注解描述接口，Knife4j 会自动把这些注解渲染成文档。
 * 注意：Spring Boot 3 用 io.swagger.v3.oas.annotations.*，不是旧的 io.swagger.annotations.*。
 */
@RestController
@RequestMapping("/demo")
@Tag(name = "演示接口", description = "knife4j 最小 demo")
public class DemoController {

    @GetMapping("/hello")
    @Operation(summary = "打招呼", description = "传入 name，返回一句问候语")
    public Map<String, String> hello(
            @Parameter(description = "姓名", required = true) @RequestParam String name) {
        return Map.of("message", "Hello, " + name + "!");
    }
}
