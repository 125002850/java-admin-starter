package com.oigit.admin.boot;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.oigit.admin")
@MapperScan(basePackages = {
    "com.oigit.admin.core.operator",
    "com.oigit.admin.dict.infra.mapper",
    "com.oigit.admin.export.infra.mapper"
}, annotationClass = Mapper.class)
public class AdminBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminBootApplication.class, args);
    }
}
