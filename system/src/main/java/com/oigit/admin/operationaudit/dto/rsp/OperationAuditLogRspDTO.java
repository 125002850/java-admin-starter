package com.oigit.admin.operationaudit.dto.rsp;

import com.oigit.admin.core.audit.OperationAuditAction;
import com.oigit.admin.core.audit.OperationAuditResultStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "操作日志列表项")
public class OperationAuditLogRspDTO {

    private Long logId;
    private String moduleCode;
    private String moduleName;
    private OperationAuditAction action;
    private String description;
    private Long operatorId;
    private String operatorName;
    private String clientIp;
    private OperationAuditResultStatus resultStatus;
    private Long durationMs;
    private LocalDateTime operationTime;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public String getModuleCode() { return moduleCode; }
    public void setModuleCode(String moduleCode) { this.moduleCode = moduleCode; }
    public String getModuleName() { return moduleName; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }
    public OperationAuditAction getAction() { return action; }
    public void setAction(OperationAuditAction action) { this.action = action; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public OperationAuditResultStatus getResultStatus() { return resultStatus; }
    public void setResultStatus(OperationAuditResultStatus resultStatus) { this.resultStatus = resultStatus; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public LocalDateTime getOperationTime() { return operationTime; }
    public void setOperationTime(LocalDateTime operationTime) { this.operationTime = operationTime; }
}
