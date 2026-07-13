package com.example.admin.boot;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.admin")
@MapperScan(basePackages = {
    "com.example.admin.core.operator",
    "com.example.admin.mdm.dict.infra.mapper",
    "com.example.admin.mdm.export.infra.mapper"
}, annotationClass = Mapper.class)
public class AdminBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminBootApplication.class, args);
    }
}
