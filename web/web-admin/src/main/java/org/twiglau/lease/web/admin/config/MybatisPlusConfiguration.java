package org.twiglau.lease.web.admin.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("org.twiglau.lease.web.admin.mapper")
public class MybatisPlusConfiguration {
}
