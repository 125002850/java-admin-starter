package com.oigit.admin.boot.config;

import com.oigit.admin.boot.web.OperationAuditHandlerInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class OperationAuditWebConfig implements WebMvcConfigurer {

    private final OperationAuditHandlerInterceptor operationAuditHandlerInterceptor;

    public OperationAuditWebConfig(OperationAuditHandlerInterceptor operationAuditHandlerInterceptor) {
        this.operationAuditHandlerInterceptor = operationAuditHandlerInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(operationAuditHandlerInterceptor);
    }
}
