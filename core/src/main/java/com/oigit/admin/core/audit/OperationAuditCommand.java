package com.oigit.admin.core.audit;

import java.time.LocalDateTime;

public record OperationAuditCommand(
        String moduleCode,
        String moduleName,
        OperationAuditAction action,
        String description,
        Long operatorId,
        String operatorUsername,
        String operatorRealName,
        String requestMethod,
        String requestPath,
        String clientIp,
        String traceId,
        String requestParams,
        OperationAuditResultStatus resultStatus,
        Integer httpStatus,
        Integer resultCode,
        String errorMessage,
        Long durationMs,
        LocalDateTime operationTime
) {
}
