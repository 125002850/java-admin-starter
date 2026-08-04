package com.oigit.admin.boot.web;

import com.oigit.admin.core.translation.TranslationEngine;
import com.oigit.admin.core.translation.TranslationScene;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice(basePackages = "com.oigit.admin")
public class TranslationResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private final TranslationEngine translationEngine;

    public TranslationResponseBodyAdvice(TranslationEngine translationEngine) {
        this.translationEngine = translationEngine;
    }

    @Override
    public boolean supports(
            MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            org.springframework.http.server.ServerHttpRequest request,
            org.springframework.http.server.ServerHttpResponse response
    ) {
        return translationEngine.translate(body, TranslationScene.WEB_RESPONSE);
    }
}
