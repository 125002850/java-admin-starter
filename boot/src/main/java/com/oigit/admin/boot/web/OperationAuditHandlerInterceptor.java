package com.oigit.admin.boot.web;

import com.oigit.admin.core.audit.OperationAudit;
import com.oigit.admin.core.audit.OperationAuditMetadata;
import com.oigit.admin.core.audit.OperationAuditModule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import static com.oigit.admin.core.audit.OperationAuditRequestAttributes.METADATA;

@Component
public class OperationAuditHandlerInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        OperationAudit audit = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(), OperationAudit.class
        );
        if (audit == null) {
            return true;
        }
        OperationAuditModule module = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getBeanType(), OperationAuditModule.class
        );
        Tag tag = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), Tag.class);
        Operation operation = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), Operation.class);
        if (module == null || tag == null || operation == null
                || !StringUtils.hasText(tag.name()) || !StringUtils.hasText(operation.summary())) {
            throw new IllegalStateException("Operation audit endpoint must declare module, @Tag.name and @Operation.summary: "
                    + handlerMethod.getMethod().toGenericString());
        }
        if (request.getAttribute(com.oigit.admin.core.operator.OperatorContext.REQUEST_ATTRIBUTE_OPERATOR_ID) != null) {
            request.setAttribute("audit.operator.username", com.oigit.admin.core.operator.OperatorContext.getOperatorName());
            request.setAttribute("audit.operator.realName", com.oigit.admin.core.operator.OperatorContext.getOperatorRealName());
        }
        request.setAttribute(METADATA, new OperationAuditMetadata(
                module.code(), tag.name(), audit.action(), operation.summary()
        ));
        return true;
    }
}
