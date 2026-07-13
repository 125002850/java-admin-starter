package com.example.admin.iam.security;

import com.example.admin.iam.annotation.OperationLog;
import com.example.admin.iam.event.OperationLogEvent;
import com.example.admin.iam.service.ClientRequestInfo;
import com.example.admin.iam.service.ClientRequestInfoResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class OperationLogAspect {

private static final int MAX_SUMMARY_LENGTH = 2048;
    private static final Pattern SENSITIVE_JSON_FIELD = Pattern.compile(
            "(?i)(\\\\*\")(password|oldPassword|newPassword|accessToken|refreshToken|token|authorization)(\\\\*\")\\s*:\\s*(\\\\*\")(?:\\\\.|[^\"\\\\])*?(\\\\*\")"
    );

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final ClientRequestInfoResolver clientRequestInfoResolver;

    public OperationLogAspect(
            ApplicationEventPublisher eventPublisher,
            ObjectMapper objectMapper,
            ClientRequestInfoResolver clientRequestInfoResolver
    ) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.clientRequestInfoResolver = clientRequestInfoResolver;
    }

    @Around("@annotation(com.example.admin.iam.annotation.OperationLog)")
    public Object record(ProceedingJoinPoint joinPoint) throws Throwable {
        OperationLog annotation = AnnotatedElementUtils.findMergedAnnotation(
                ((MethodSignature) joinPoint.getSignature()).getMethod(),
                OperationLog.class
        );
        long started = System.currentTimeMillis();
        String requestSummary = summarize(joinPoint.getArgs());
        try {
            Object result = joinPoint.proceed();
            publish(annotation, requestSummary, summarize(result), true, null, System.currentTimeMillis() - started);
            return result;
        } catch (Throwable ex) {
            publish(annotation, requestSummary, null, false, truncate(ex.getMessage()), System.currentTimeMillis() - started);
            throw ex;
        }
    }

    private void publish(
            OperationLog annotation,
            String requestSummary,
            String responseSummary,
            boolean success,
            String errorMessage,
            long costMillis
    ) {
        ClientRequestInfo clientRequestInfo = clientRequestInfoResolver.current();
        HttpServletRequest request = currentRequest();
        IamPrincipal principal = CurrentIam.principal().orElse(null);
        eventPublisher.publishEvent(new OperationLogEvent(
                principal == null ? null : principal.getStaffId(),
                principal == null ? null : principal.getUsername(),
                principal == null ? null : principal.getStaffName(),
                annotation.module(),
                annotation.action(),
                request == null ? null : request.getRequestURI(),
                request == null ? null : request.getMethod().toUpperCase(Locale.ROOT),
                requestSummary,
                responseSummary,
                success,
                errorMessage,
                clientRequestInfo.ip(),
                clientRequestInfo.userAgent(),
                costMillis,
                LocalDateTime.now()
        ));
    }

    private HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        return attributes.getRequest();
    }

    private String summarize(Object value) {
        try {
            return truncate(mask(objectMapper.writeValueAsString(value)));
        } catch (Exception ex) {
            return null;
        }
    }

    private String mask(String source) {
        if (source == null) {
            return null;
        }
        return SENSITIVE_JSON_FIELD.matcher(source).replaceAll(mr ->
                mr.group(1) + mr.group(2) + mr.group(3) + ":"
                        + mr.group(4) + "***" + mr.group(5));
    }

    private String truncate(String value) {
        if (value == null || value.length() <= MAX_SUMMARY_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_SUMMARY_LENGTH);
    }
}
