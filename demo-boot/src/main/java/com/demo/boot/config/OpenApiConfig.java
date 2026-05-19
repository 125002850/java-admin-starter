package com.demo.boot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.models.GroupedOpenApi;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI demoOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("java-demo API 文档")
                .description("java-demo 项目接口文档")
                .version("v1")
                .contact(new Contact().name("java-demo")));
    }

    @Bean
    public GroupedOpenApi mdmDictApi() {
        return GroupedOpenApi.builder()
            .group("mdm-dict")
            .packagesToScan("com.demo.mdm.controller")
            .pathsToMatch("/api/mdm/dict/**")
            .build();
    }
}
