package org.twiglau.hellomp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.twiglau.hellomp.mapper")
public class HelloMpApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelloMpApplication.class, args);
    }

}
