package com.oigit.admin.core.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.oigit.admin.core.trace.TraceIdFilter.TRACE_ID_MDC_KEY;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class HttpRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpRequestLoggingFilter.class);
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_REAL_IP = "X-Real-IP";

    private final HttpLoggingProperties properties;
    private final HttpLogSanitizer sanitizer;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public HttpRequestLoggingFilter(HttpLoggingProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.sanitizer = new HttpLogSanitizer(objectMapper, properties.getSensitiveFields());
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        if (!properties.isEnabled() || HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return properties.getExcludedPaths().stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        ContentCachingRequestWrapper requestWrapper = wrapRequest(request);
        LimitedContentCachingResponseWrapper responseWrapper = new LimitedContentCachingResponseWrapper(
                response,
                properties.getMaxResponseBodyBytes()
        );
        Throwable failure = null;
        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } catch (ServletException | IOException | RuntimeException exception) {
            failure = exception;
            throw exception;
        } finally {
            long costMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            log.info("{}", buildLogMessage(requestWrapper, responseWrapper, failure, costMillis));
        }
    }

    private ContentCachingRequestWrapper wrapRequest(HttpServletRequest request) {
        int cacheLimit = properties.getMaxRequestBodyBytes() + 1;
        return new ContentCachingRequestWrapper(request, cacheLimit);
    }

    private String buildLogMessage(ContentCachingRequestWrapper request,
                                   LimitedContentCachingResponseWrapper response,
                                   Throwable failure,
                                   long costMillis) {
        int status = response.getStatus();
        if (failure != null && status < HttpServletResponse.SC_BAD_REQUEST) {
            status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
        String requestBody = requestBody(request);
        String responseBody = responseBody(response);
        return "REQ DONE\n"
                + "+---------------- request ----------------\n"
                + "| method  : " + sanitizer.singleLine(request.getMethod()) + "\n"
                + "| uri     : " + sanitizer.singleLine(request.getRequestURI()) + "\n"
                + "| traceId : " + valueOrDash(MDC.get(TRACE_ID_MDC_KEY)) + "\n"
                + "| userId  : " + valueOrDash(request.getHeader(HEADER_USER_ID)) + "\n"
                + "| ip      : " + clientIp(request) + "\n"
                + "| query   : " + bounded(sanitizer.sanitizeQuery(request.getQueryString()), 2048) + "\n"
                + "| body    : " + requestBody + "\n"
                + "+---------------- response ---------------\n"
                + "| status  : " + status + "\n"
                + "| cost    : " + costMillis + "ms\n"
                + "| body    : " + responseBody + "\n"
                + "+----------------------------------------";
    }

    private String requestBody(ContentCachingRequestWrapper request) {
        byte[] body = request.getContentAsByteArray();
        boolean truncated = body.length > properties.getMaxRequestBodyBytes();
        return sanitizer.sanitizeBody(
                body,
                request.getContentType(),
                request.getCharacterEncoding(),
                properties.isRequestBodyEnabled(),
                truncated,
                properties.getMaxRequestBodyBytes()
        );
    }

    private String responseBody(LimitedContentCachingResponseWrapper response) {
        return sanitizer.sanitizeBody(
                response.getCachedContent(),
                response.getContentType(),
                response.getCharacterEncoding(),
                properties.isResponseBodyEnabled(),
                response.isContentTruncated(),
                properties.getMaxResponseBodyBytes()
        );
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(HEADER_FORWARDED_FOR);
        if (StringUtils.hasText(forwardedFor)) {
            return valueOrDash(forwardedFor.split(",", 2)[0]);
        }
        String realIp = request.getHeader(HEADER_REAL_IP);
        return StringUtils.hasText(realIp) ? valueOrDash(realIp) : valueOrDash(request.getRemoteAddr());
    }

    private String valueOrDash(String value) {
        if (!StringUtils.hasText(value)) {
            return "-";
        }
        String sanitized = sanitizer.singleLine(value);
        return bounded(sanitized, 256);
    }

    private String bounded(String value, int limit) {
        return value.length() <= limit ? value : value.substring(0, limit) + "...";
    }
}
