package com.oigit.admin.iam.domain.model;

import com.oigit.admin.iam.enums.OperationLogAction;
import com.oigit.admin.iam.enums.OperationLogModule;

import java.time.LocalDateTime;

/** 操作日志查询快照。 */
public record IamOperationLog(
        Long id,
        Long operatorId,
        String operatorUsername,
        String operatorStaffName,
        OperationLogModule module,
        OperationLogAction action,
        String requestPath,
        String httpMethod,
        String requestSummary,
        String responseSummary,
        Boolean success,
        String errorMessage,
        String ip,
        String userAgent,
        Long costMillis,
        LocalDateTime operationTime) {}
