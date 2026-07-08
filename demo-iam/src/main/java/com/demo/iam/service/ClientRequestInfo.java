package com.demo.iam.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.util.StringUtils;

public record ClientRequestInfo(String ip, String userAgent) {

    public static ClientRequestInfo current() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return new ClientRequestInfo(null, null);
        }
        HttpServletRequest request = attributes.getRequest();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String ip = StringUtils.hasText(forwardedFor) ? forwardedFor.split(",")[0].trim() : request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        return new ClientRequestInfo(ip, userAgent);
    }
}
