package com.oigit.admin.core.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oigit.admin.core.logging.HttpLogSanitizer;
import com.oigit.admin.core.logging.HttpLoggingProperties;
import com.oigit.admin.core.operator.OperatorContext;
import com.oigit.admin.core.operator.ClientIpResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.oigit.admin.core.audit.OperationAuditRequestAttributes.METADATA;
import static com.oigit.admin.core.audit.OperationAuditRequestAttributes.RESULT_CODE;
import static com.oigit.admin.core.audit.OperationAuditRequestAttributes.RESULT_MESSAGE;
import static com.oigit.admin.core.trace.TraceIdFilter.TRACE_ID_MDC_KEY;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 15)
public class OperationAuditFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(OperationAuditFilter.class);
    private static final int MAX_REQUEST_PARAMS_BYTES = 8 * 1024;
    private static final int SUCCESS_CODE = 200;

    private final OperationAuditWriter writer;
    private final HttpLogSanitizer sanitizer;
    private ClientIpResolver clientIpResolver = HttpServletRequest::getRemoteAddr;

    @Autowired
    public OperationAuditFilter(
            ObjectProvider<OperationAuditWriter> writerProvider,
            ObjectMapper objectMapper,
            HttpLoggingProperties loggingProperties,
            ObjectProvider<ClientIpResolver> resolverProvider
    ) {
        this.clientIpResolver = resolverProvider.getIfAvailable(() -> HttpServletRequest::getRemoteAddr);
        this.writer = writerProvider.getIfAvailable();
        this.sanitizer = new HttpLogSanitizer(objectMapper, loggingProperties.getSensitiveFields());
    }

    OperationAuditFilter(
            OperationAuditWriter writer,
            ObjectMapper objectMapper,
            HttpLoggingProperties loggingProperties
    ) {
        this.writer = writer;
        this.sanitizer = new HttpLogSanitizer(objectMapper, loggingProperties.getSensitiveFields());
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return HttpMethod.OPTIONS.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        LocalDateTime operationTime = LocalDateTime.now();
        ContentCachingRequestWrapper requestWrapper = wrapRequest(request);
        Throwable failure = null;
        try {
            filterChain.doFilter(requestWrapper, response);
        } catch (ServletException | IOException | RuntimeException exception) {
            failure = exception;
            throw exception;
        } finally {
            writeAuditSafely(requestWrapper, response, failure, startedAt, operationTime);
        }
    }

    private ContentCachingRequestWrapper wrapRequest(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper wrapper) {
            return wrapper;
        }
        return new ContentCachingRequestWrapper(request, MAX_REQUEST_PARAMS_BYTES + 1);
    }

    private void writeAuditSafely(
            ContentCachingRequestWrapper request,
            HttpServletResponse response,
            Throwable failure,
            long startedAt,
            LocalDateTime operationTime
    ) {
        Object metadataValue = request.getAttribute(METADATA);
        if (!(metadataValue instanceof OperationAuditMetadata metadata) || writer == null) {
            return;
        }
        try {
            int httpStatus = response.getStatus();
            if (failure != null && httpStatus < HttpServletResponse.SC_BAD_REQUEST) {
                httpStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            }
            Integer resultCode = integerAttribute(request, RESULT_CODE);
            boolean success = httpStatus < HttpServletResponse.SC_BAD_REQUEST
                    && resultCode != null
                    && resultCode == SUCCESS_CODE;
            String resultMessage = stringAttribute(request, RESULT_MESSAGE);
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            writer.write(new OperationAuditCommand(
                    bounded(metadata.moduleCode(), 100),
                    bounded(metadata.moduleName(), 128),
                    metadata.action(),
                    bounded(metadata.description(), 255),
                    (Long) request.getAttribute(OperatorContext.REQUEST_ATTRIBUTE_OPERATOR_ID),
                    bounded(stringAttribute(request, "audit.operator.username"), 128),
                    bounded(stringAttribute(request, "audit.operator.realName"), 128),
                    bounded(request.getMethod(), 16),
                    bounded(request.getRequestURI(), 512),
                    bounded(clientIp(request), 64),
                    bounded(MDC.get(TRACE_ID_MDC_KEY), 128),
                    requestParams(request),
                    success ? OperationAuditResultStatus.SUCCESS : OperationAuditResultStatus.FAILED,
                    httpStatus,
                    resultCode,
                    success ? null : bounded(defaultErrorMessage(resultMessage), 1000),
                    durationMs,
                    operationTime
            ));
        } catch (Exception exception) {
            log.error("Failed to persist operation audit log for {} {}", request.getMethod(),
                    request.getRequestURI(), exception);
        }
    }

    private String requestParams(ContentCachingRequestWrapper request) {
        String query = sanitizer.sanitizeQuery(request.getQueryString());
        byte[] body = request.getContentAsByteArray();
        boolean truncated = body.length > MAX_REQUEST_PARAMS_BYTES;
        String sanitizedBody = sanitizer.sanitizeBody(
                body,
                request.getContentType(),
                request.getCharacterEncoding(),
                true,
                truncated,
                MAX_REQUEST_PARAMS_BYTES
        );
        boolean hasQuery = !"-".equals(query);
        boolean hasBody = !"-".equals(sanitizedBody);
        if (!hasQuery) {
            return boundedUtf8(sanitizedBody, MAX_REQUEST_PARAMS_BYTES);
        }
        if (!hasBody) {
            return boundedUtf8(query, MAX_REQUEST_PARAMS_BYTES);
        }
        return boundedUtf8("query=" + query + "\nbody=" + sanitizedBody, MAX_REQUEST_PARAMS_BYTES);
    }

    private String clientIp(HttpServletRequest request) {
        return clientIpResolver.resolveClientIp(request);
    }

    private Integer integerAttribute(HttpServletRequest request, String name) {
        Object value = request.getAttribute(name);
        return value instanceof Integer integer ? integer : null;
    }

    private String stringAttribute(HttpServletRequest request, String name) {
        Object value = request.getAttribute(name);
        return value instanceof String string ? string : null;
    }

    private String defaultErrorMessage(String message) {
        return StringUtils.hasText(message) ? sanitizer.singleLine(message) : "请求处理失败";
    }

    private String bounded(String value, int limit) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = sanitizer.singleLine(value);
        if (normalized.length() <= limit) {
            return normalized;
        }
        int contentLength = Math.max(0, limit - 3);
        return normalized.substring(0, contentLength) + "...";
    }

    private String boundedUtf8(String value, int maxBytes) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) {
            return value;
        }
        int end = maxBytes - 3;
        while (end > 0 && (bytes[end] & 0xC0) == 0x80) {
            end--;
        }
        return new String(bytes, 0, end, StandardCharsets.UTF_8) + "...";
    }
}
