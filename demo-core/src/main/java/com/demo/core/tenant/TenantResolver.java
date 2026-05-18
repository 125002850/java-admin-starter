package com.demo.core.tenant;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

public interface TenantResolver {

    String TENANT_ID_HEADER = "X-Tenant-Id";

    Optional<Long> resolve(HttpServletRequest request);
}
