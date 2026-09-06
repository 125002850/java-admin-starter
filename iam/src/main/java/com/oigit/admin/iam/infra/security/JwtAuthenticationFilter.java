package com.oigit.admin.iam.infra.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oigit.admin.core.operator.OperatorContext;
import com.oigit.admin.core.web.R;
import com.oigit.admin.iam.app.PermissionAppService;
import com.oigit.admin.iam.domain.model.PermissionSnapshot;
import com.oigit.admin.iam.enums.IamErrorCode;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> MUST_CHANGE_ALLOWED_PATHS =
            List.of("/api/iam/auth/me", "/api/iam/auth/logout", "/api/iam/auth/password/change");

    private final JwtService jwtService;
    private final PermissionAppService permissionSnapshotService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            PermissionAppService permissionSnapshotService,
            ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.permissionSnapshotService = permissionSnapshotService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            JwtClaims claims = jwtService.parse(token);
            PermissionSnapshot snapshot = permissionSnapshotService.loadByStaffId(claims.staffId());
            IamPrincipal principal = new IamPrincipal(snapshot);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            OperatorContext.set(
                    principal.getStaffId(),
                    principal.getUsername(),
                    null,
                    principal.getStaffName());
            request.setAttribute(
                    OperatorContext.REQUEST_ATTRIBUTE_OPERATOR_ID, principal.getStaffId());
            if (snapshot.isMustChangePassword()
                    && !isMustChangePasswordAllowed(request.getRequestURI())) {
                IamAccessDeniedHandler.writeForbidden(
                        objectMapper, response, R.fail(IamErrorCode.AUTH_MUST_CHANGE_PASSWORD));
                return;
            }
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            OperatorContext.clear();
            IamAccessDeniedHandler.writeUnauthorized(objectMapper, response);
        } finally {
            SecurityContextHolder.clearContext();
            OperatorContext.clear();
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return StringUtils.hasText(token) ? token : null;
    }

    private boolean isMustChangePasswordAllowed(String path) {
        return MUST_CHANGE_ALLOWED_PATHS.contains(path);
    }
}
