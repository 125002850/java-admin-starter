package com.oigit.admin.iam.event;

import com.oigit.admin.iam.enums.OperationLogAction;
import com.oigit.admin.iam.enums.OperationLogModule;
import java.time.LocalDateTime;

public record OperationLogEvent(
        Long operatorId,
        String operatorUsername,
        String operatorStaffName,
        OperationLogModule module,
        OperationLogAction action,
        String requestPath,
        String httpMethod,
        String requestSummary,
        String responseSummary,
        boolean success,
        String errorMessage,
        String ip,
        String userAgent,
        long costMillis,
        LocalDateTime operationTime
) {
}
