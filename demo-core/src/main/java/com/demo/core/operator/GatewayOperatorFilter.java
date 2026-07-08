package com.demo.core.operator;

import com.demo.core.exception.CommonErrorCode;
import com.demo.core.web.R;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

@Component
@ConditionalOnProperty(prefix = "platform.operator", name = "gateway-filter-enabled", havingValue = "true")
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class GatewayOperatorFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GatewayOperatorFilter.class);
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_NAME = "X-User-Name";
    private static final String HEADER_USER_PHONE = "X-User-Phone";
    private static final String HEADER_REAL_NAME = "X-Real-Name";
    private static final String HEADER_USER_CODE = "X-User-Code";

    private final ObjectMapper objectMapper;
    private final CacheUserService cacheUserService;
    private final TaskExecutor cacheUserTaskExecutor;
    private final long cacheUserUpdateWindowMillis;
    private final LongSupplier currentTimeMillisSupplier;
    private final ConcurrentHashMap<Long, CacheUserUpdateWindow> cacheUserUpdateWindows = new ConcurrentHashMap<>();

    @Autowired
    public GatewayOperatorFilter(ObjectMapper objectMapper,
                                 CacheUserService cacheUserService,
                                 @Qualifier("cacheUserTaskExecutor") TaskExecutor cacheUserTaskExecutor,
                                 CacheUserProperties cacheUserProperties) {
        this(objectMapper, cacheUserService, cacheUserTaskExecutor,
                cacheUserProperties.getUpdateWindowSeconds() * 1000L, System::currentTimeMillis);
    }

    GatewayOperatorFilter(ObjectMapper objectMapper,
                          CacheUserService cacheUserService,
                          TaskExecutor cacheUserTaskExecutor,
                          long cacheUserUpdateWindowMillis,
                          LongSupplier currentTimeMillisSupplier) {
        this.objectMapper = objectMapper;
        this.cacheUserService = cacheUserService;
        this.cacheUserTaskExecutor = cacheUserTaskExecutor;
        this.cacheUserUpdateWindowMillis = Math.max(0L, cacheUserUpdateWindowMillis);
        this.currentTimeMillisSupplier = currentTimeMillisSupplier;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {


        String userIdHeader = normalize(request.getHeader(HEADER_USER_ID));
        String userNameHeader = decodeAndNormalize(request.getHeader(HEADER_USER_NAME));
        String userPhoneHeader = normalize(request.getHeader(HEADER_USER_PHONE));
        String realNameHeader = decodeAndNormalize(request.getHeader(HEADER_REAL_NAME));
        String userCodeHeader = normalize(request.getHeader(HEADER_USER_CODE));

        if (userIdHeader != null) {
            Long userId;
            try {
                userId = Long.valueOf(userIdHeader);
            } catch (NumberFormatException exception) {
                log.warn("Invalid X-User-Id header: {}", userIdHeader);
                writeErrorResponse(response, "X-User-Id 值非法: " + userIdHeader);
                return;
            }
            OperatorContext.set(userId, userNameHeader, userPhoneHeader, realNameHeader);
            submitCacheUserUpdate(userId, userNameHeader, userPhoneHeader, realNameHeader, userCodeHeader);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            OperatorContext.clear();
        }
    }

    private void writeErrorResponse(HttpServletResponse response, String message) throws IOException {
        R<Void> result = R.fail(CommonErrorCode.PARAM_ERROR, message);
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), result);
    }

    private void submitCacheUserUpdate(Long userId,
                                       String userName,
                                       String userPhone,
                                       String realName,
                                       String userCode) {
        if (!hasCacheUserPayload(userName, userPhone, realName, userCode)
                || !shouldSubmitCacheUserUpdate(userId, userName, userPhone, realName, userCode)) {
            return;
        }
        try {
            cacheUserTaskExecutor.execute(() -> {
                try {
                    cacheUserService.upsert(userId, userName, userPhone, realName, userCode);
                } catch (Exception e) {
                    log.warn("Failed to upsert cache_user for userId={}", userId, e);
                }
            });
        } catch (RuntimeException e) {
            log.warn("Failed to submit cache_user upsert task for userId={}", userId, e);
        }
    }

    private boolean shouldSubmitCacheUserUpdate(Long userId,
                                                String userName,
                                                String userPhone,
                                                String realName,
                                                String userCode) {
        long now = currentTimeMillisSupplier.getAsLong();
        boolean[] shouldSubmit = {false};
        cacheUserUpdateWindows.compute(userId, (ignored, existing) -> {
            if (existing != null
                    && now - existing.lastSubmittedAtMillis() < cacheUserUpdateWindowMillis
                    && existing.matches(userName, userPhone, realName, userCode)) {
                return existing;
            }
            shouldSubmit[0] = true;
            return new CacheUserUpdateWindow(now, userName, userPhone, realName, userCode);
        });
        return shouldSubmit[0];
    }

    private static boolean hasCacheUserPayload(String userName,
                                               String userPhone,
                                               String realName,
                                               String userCode) {
        return userName != null || userPhone != null || realName != null || userCode != null;
    }

    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String decodeAndNormalize(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        try {
            return normalize(URLDecoder.decode(normalized, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException exception) {
            return normalized;
        }
    }

    public record CacheUserUpdateWindow(long lastSubmittedAtMillis,
                                        String userName,
                                        String userPhone,
                                        String realName,
                                        String userCode) {

        public boolean matches(String userName, String userPhone, String realName, String userCode) {
            return Objects.equals(this.userName, userName)
                    && Objects.equals(this.userPhone, userPhone)
                    && Objects.equals(this.realName, realName)
                    && Objects.equals(this.userCode, userCode);
        }
    }
}
