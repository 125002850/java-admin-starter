package com.oigit.admin.boot.web;

import com.oigit.admin.core.web.R;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import static com.oigit.admin.core.audit.OperationAuditRequestAttributes.RESULT_CODE;
import static com.oigit.admin.core.audit.OperationAuditRequestAttributes.RESULT_MESSAGE;

@Order(Ordered.LOWEST_PRECEDENCE)
@ControllerAdvice(basePackages = "com.oigit.admin")
public class OperationAuditResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        if (body instanceof R<?> result && request instanceof ServletServerHttpRequest servletRequest) {
            servletRequest.getServletRequest().setAttribute(RESULT_CODE, result.getCode());
            servletRequest.getServletRequest().setAttribute(RESULT_MESSAGE, result.getMsg());
        }
        return body;
    }
}
