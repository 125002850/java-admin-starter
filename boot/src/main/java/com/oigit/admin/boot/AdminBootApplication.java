package com.oigit.admin.boot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.oigit.admin")
@MapperScan({
    "com.oigit.admin.iam.infra.mapper",
    "com.oigit.admin.dict.infra.mapper",
    "com.oigit.admin.export.infra.mapper"
})
public class AdminBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminBootApplication.class, args);
    }
}
