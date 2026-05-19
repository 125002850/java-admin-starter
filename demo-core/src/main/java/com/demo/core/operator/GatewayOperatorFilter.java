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
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class GatewayOperatorFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GatewayOperatorFilter.class);
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_NAME = "X-User-Name";

    private final ObjectMapper objectMapper;

    public GatewayOperatorFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String userIdHeader = request.getHeader(HEADER_USER_ID);
        String userNameHeader = request.getHeader(HEADER_USER_NAME);

        if (userIdHeader != null) {
            Long userId;
            try {
                userId = Long.valueOf(userIdHeader);
            } catch (NumberFormatException exception) {
                log.warn("Invalid X-User-Id header: {}", userIdHeader);
                writeErrorResponse(response, "X-User-Id 值非法: " + userIdHeader);
                return;
            }
            OperatorContext.set(userId, userNameHeader);
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
}
