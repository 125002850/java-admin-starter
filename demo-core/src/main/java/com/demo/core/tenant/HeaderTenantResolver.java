package com.demo.core.tenant;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Component
@Primary
public class HeaderTenantResolver implements TenantResolver {

    @Override
    public Optional<Long> resolve(HttpServletRequest request) {
        String tenantIdHeader = request.getHeader(TenantResolver.TENANT_ID_HEADER);
        if (!StringUtils.hasText(tenantIdHeader)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(tenantIdHeader));
        } catch (NumberFormatException exception) {
            throw new InvalidTenantHeaderException(tenantIdHeader);
        }
    }
}
