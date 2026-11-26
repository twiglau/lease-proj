package org.twiglau.helloknife4j.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 文档元信息配置：标题、描述、版本。
 * 不写这个 Bean 文档也能出来，但会有默认的 "OpenAPI definition"，这里自定义一下。
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI demoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("hello-knife4j 接口文档")
                        .description("Knife4j + OpenAPI3 最小演练")
                        .version("v1.0"));
    }

    @Bean
    public GroupedOpenApi userAPI () {
        return GroupedOpenApi.builder()
                .group("用户信息管理")
                .pathsToMatch("/user/**")
                .build();
    }
}
