package com.oigit.admin.iam.dto.rsp;

import com.oigit.admin.iam.enums.OperationLogAction;
import com.oigit.admin.iam.enums.OperationLogModule;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "操作日志响应")
public class OperationLogRspDTO {
    public Long logId;
    public Long operatorId;
    public String operatorUsername;
    public String operatorStaffName;
    public OperationLogModule module;
    public OperationLogAction action;
    public String requestPath;
    public String httpMethod;
    public String requestSummary;
    public String responseSummary;
    public Boolean success;
    public String errorMessage;
    public String ip;
    public String userAgent;
    public Long costMillis;
    public LocalDateTime operationTime;
}
