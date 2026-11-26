package org.twiglau.hellominio;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.twiglau.hellominio.service.MinioService;

import java.time.Duration;

/** 启动即跑一遍对象存储全流程，用于学习验证；跑完自动退出（非 web 应用） */

/**
 * 启动流程：
 *                    main(String[] args)
 *                       程序入口
 *                          |
 *                    SpringApplication.run()
 *                      创建并刷新IoC容器
 *                          |
 *                     容器 refresh 完成
 *                      所有 Bean 已就绪
 *                          |
 *                    CommandLineRunner.run(args)
 *                      在这里写一次性逻辑
 *                          |
 *          |-----------------------------------|
 *      Web应用                               非Web应用
 *    继续监听，进程常驻                       run返回后JVM退出
 *
 * CommandLineRunner 是 Spring Boot 提供的「函数式接口」，它不是被程序其他调用的，
 * 而是「容器就绪后的回调钩子」--- Spring启动时会自动找出容器里所有实现它的Bean,挨个调用 run().
 */
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
