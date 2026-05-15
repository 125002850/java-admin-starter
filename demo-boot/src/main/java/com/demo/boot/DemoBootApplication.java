package com.demo.boot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.demo")
@MapperScan("com.demo")
public class DemoBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoBootApplication.class, args);
    }
}
