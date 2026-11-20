package org.twiglau.hellominio;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.twiglau.hellominio.service.MinioService;

import java.time.Duration;

/** 启动即跑一遍对象存储全流程，用于学习验证；跑完自动退出（非 web 应用） */
@Component
@RequiredArgsConstructor
public class DemoRunner implements CommandLineRunner {

    private final MinioService minioService;

    @Override
    public void run(String... args) {
        String key = "demo/hello.txt";

        System.out.println("\n========== MinIO 最小 Demo 开始 ==========");
        minioService.ensureBucket();
        minioService.upload(key, "Hello MinIO from Spring Boot!");
        minioService.download(key);
        minioService.list();
        minioService.presignedUrl(key, Duration.ofMinutes(10));
        minioService.delete(key);
        System.out.println("========== MinIO 最小 Demo 结束 ==========\n");
    }
}
