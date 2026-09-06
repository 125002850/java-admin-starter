package com.oigit.admin.iam.infra.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oigit.admin.core.exception.CommonErrorCode;
import com.oigit.admin.core.web.R;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;

import java.io.IOException;

public final class IamAccessDeniedHandler {

    private IamAccessDeniedHandler() {}

    public static void writeUnauthorized(ObjectMapper objectMapper, HttpServletResponse response)
            throws IOException {
        write(
                objectMapper,
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                R.fail(CommonErrorCode.UNAUTHORIZED));
    }

    public static void writeForbidden(ObjectMapper objectMapper, HttpServletResponse response)
            throws IOException {
        write(
                objectMapper,
                response,
                HttpServletResponse.SC_FORBIDDEN,
                R.fail(CommonErrorCode.FORBIDDEN));
    }

    public static void writeForbidden(
            ObjectMapper objectMapper, HttpServletResponse response, R<Void> body)
            throws IOException {
        write(objectMapper, response, HttpServletResponse.SC_FORBIDDEN, body);
    }

    private static void write(
            ObjectMapper objectMapper, HttpServletResponse response, int status, R<Void> body)
            throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
