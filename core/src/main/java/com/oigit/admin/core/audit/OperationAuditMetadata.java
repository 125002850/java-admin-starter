package com.oigit.admin.core.audit;

public record OperationAuditMetadata(
        String moduleCode,
        String moduleName,
        OperationAuditAction action,
        String description
) {
}
