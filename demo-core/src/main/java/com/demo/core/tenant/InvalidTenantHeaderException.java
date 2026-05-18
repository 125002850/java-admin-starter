package com.demo.core.tenant;

public class InvalidTenantHeaderException extends RuntimeException {

    public InvalidTenantHeaderException(String tenantIdHeader) {
        super("X-Tenant-Id 值非法: " + tenantIdHeader);
    }
}
