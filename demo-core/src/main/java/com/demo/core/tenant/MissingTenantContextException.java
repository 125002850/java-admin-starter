package com.demo.core.tenant;

public class MissingTenantContextException extends RuntimeException {

    public MissingTenantContextException() {
        super("Missing tenant context for tenant-isolated query");
    }
}
