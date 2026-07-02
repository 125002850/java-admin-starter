package com.demo.boot.config;

import io.swagger.v3.oas.models.Operation;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class SpringDocOperationIdConfig {

    private static final String EXPLICIT_FLAG = "x-explicit-operation-id";

    @Bean
    public OperationCustomizer explicitOperationIdMarkerCustomizer() {
        return (operation, handlerMethod) -> {
            boolean explicit = hasExplicitOperationId(handlerMethod);
            operation.addExtension(EXPLICIT_FLAG, explicit);
            return operation;
        };
    }

    @Bean(name = "globalOpenApiCustomizer")
    public GlobalOpenApiCustomizer globalOpenApiCustomizer() {
        return openApi -> {
            Map<String, Integer> counts = new LinkedHashMap<>();
            openApi.getPaths().forEach((path, pathItem) -> {
                rewriteOperationId(path, pathItem.getGet(), counts);
                rewriteOperationId(path, pathItem.getPost(), counts);
                rewriteOperationId(path, pathItem.getPut(), counts);
                rewriteOperationId(path, pathItem.getDelete(), counts);
                rewriteOperationId(path, pathItem.getPatch(), counts);
                rewriteOperationId(path, pathItem.getHead(), counts);
                rewriteOperationId(path, pathItem.getOptions(), counts);
                rewriteOperationId(path, pathItem.getTrace(), counts);
            });
        };
    }

    private void rewriteOperationId(String path, Operation operation, Map<String, Integer> counts) {
        if (operation == null) {
            return;
        }
        boolean explicit = Boolean.TRUE.equals(
            operation.getExtensions() == null ? null : operation.getExtensions().get(EXPLICIT_FLAG)
        );
        if (!explicit) {
            operation.setOperationId(toOperationId(path));
        }
        if (operation.getExtensions() != null) {
            operation.getExtensions().remove(EXPLICIT_FLAG);
        }
        String operationId = operation.getOperationId();
        if (!StringUtils.hasText(operationId)) {
            return;
        }
        int count = counts.getOrDefault(operationId, 0);
        if (count > 0) {
            operation.setOperationId(operationId + "_" + count);
        }
        counts.put(operationId, count + 1);
    }

    String toOperationId(String path) {
        List<String> parts = new ArrayList<>();
        for (String segment : path.split("/")) {
            if (!StringUtils.hasText(segment)) {
                continue;
            }
            parts.add(segment);
        }
        if (!parts.isEmpty() && "api".equals(parts.get(0))) {
            parts = parts.subList(1, parts.size());
        }
        List<String> words = new ArrayList<>();
        for (String part : parts) {
            if (part.startsWith("{") && part.endsWith("}")) {
                String paramName = part.substring(1, part.length() - 1);
                words.add("by");
                for (String chunk : paramName.split("-")) {
                    words.add(chunk);
                }
                continue;
            }
            words.addAll(Arrays.asList(part.split("-")));
        }
        if (words.isEmpty()) {
            return "unnamedOperation";
        }
        StringBuilder sb = new StringBuilder(words.get(0));
        for (int i = 1; i < words.size(); i++) {
            sb.append(StringUtils.capitalize(words.get(i)));
        }
        return sb.toString();
    }

    private static boolean hasExplicitOperationId(HandlerMethod handlerMethod) {
        var annotation = AnnotatedElementUtils.findMergedAnnotation(
            handlerMethod.getMethod(),
            io.swagger.v3.oas.annotations.Operation.class
        );
        return annotation != null && StringUtils.hasText(annotation.operationId());
    }
}
