package com.oigit.admin.operationaudit.domain.model;

import com.oigit.admin.core.audit.OperationAuditAction;
import com.oigit.admin.core.audit.OperationAuditResultStatus;

import java.time.LocalDateTime;

public record OperationAuditLog(
        Long id,
        String moduleCode,
        String moduleName,
        OperationAuditAction action,
        String description,
        Long operatorId,
        String operatorName,
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
