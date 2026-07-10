package com.example.admin.boot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.admin")
@MapperScan({
    "com.example.admin.core.operator",
    "com.example.admin.iam.infra.mapper",
    "com.example.admin.dict.infra.mapper",
    "com.example.admin.export.infra.mapper"
})
public class AdminBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminBootApplication.class, args);
    }
}
