package com.demo.boot;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.demo")
@MapperScan(basePackages = {
    "com.demo.core.operator",
    "com.demo.mdm.dict.infra.mapper",
    "com.demo.mdm.export.infra.mapper"
}, annotationClass = Mapper.class)
public class DemoBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoBootApplication.class, args);
    }
}
