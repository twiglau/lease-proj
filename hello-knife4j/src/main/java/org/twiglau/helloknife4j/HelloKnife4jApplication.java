package org.twiglau.helloknife4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 3 + Knife4j 最小示例启动类。
 * 启动后访问 http://localhost:8081/doc.html 查看接口文档。
 */
@SpringBootApplication
public class HelloKnife4jApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelloKnife4jApplication.class, args);
    }
}
