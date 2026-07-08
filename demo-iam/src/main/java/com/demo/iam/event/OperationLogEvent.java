package com.demo.iam.event;

import com.demo.iam.enums.OperationLogAction;
import com.demo.iam.enums.OperationLogModule;
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
