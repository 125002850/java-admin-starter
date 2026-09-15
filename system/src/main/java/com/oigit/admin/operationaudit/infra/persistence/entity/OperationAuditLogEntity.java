package com.oigit.admin.operationaudit.infra.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.oigit.admin.core.mybatis.BaseEntity;

import java.time.LocalDateTime;

@TableName("sys_operation_audit_log")
public class OperationAuditLogEntity extends BaseEntity {

    private Long id;
    @TableField("module_code")
    private String moduleCode;
    @TableField("module_name")
    private String moduleName;
    @TableField("action_code")
    private String action;
    @TableField("operation_description")
    private String description;
    @TableField("operator_id")
    private Long operatorId;
    @TableField("operator_name")
    private String operatorName;
    @TableField("operator_username")
    private String operatorUsername;
    @TableField("operator_real_name")
    private String operatorRealName;
    @TableField("request_method")
    private String requestMethod;
    @TableField("request_path")
    private String requestPath;
    @TableField("client_ip")
    private String clientIp;
    @TableField("trace_id")
    private String traceId;
    @TableField("request_params")
    private String requestParams;
    @TableField("result_status")
    private String resultStatus;
    @TableField("http_status")
    private Integer httpStatus;
    @TableField("result_code")
    private Integer resultCode;
    @TableField("error_message")
    private String errorMessage;
    @TableField("duration_ms")
    private Long durationMs;
    @TableField("operation_time")
    private LocalDateTime operationTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getModuleCode() { return moduleCode; }
    public void setModuleCode(String moduleCode) { this.moduleCode = moduleCode; }
    public String getModuleName() { return moduleName; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public String getOperatorUsername() { return operatorUsername; }
    public void setOperatorUsername(String operatorUsername) { this.operatorUsername = operatorUsername; }
    public String getOperatorRealName() { return operatorRealName; }
    public void setOperatorRealName(String operatorRealName) { this.operatorRealName = operatorRealName; }
    public String getRequestMethod() { return requestMethod; }
    public void setRequestMethod(String requestMethod) { this.requestMethod = requestMethod; }
    public String getRequestPath() { return requestPath; }
    public void setRequestPath(String requestPath) { this.requestPath = requestPath; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getRequestParams() { return requestParams; }
    public void setRequestParams(String requestParams) { this.requestParams = requestParams; }
    public String getResultStatus() { return resultStatus; }
    public void setResultStatus(String resultStatus) { this.resultStatus = resultStatus; }
    public Integer getHttpStatus() { return httpStatus; }
    public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }
    public Integer getResultCode() { return resultCode; }
    public void setResultCode(Integer resultCode) { this.resultCode = resultCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public LocalDateTime getOperationTime() { return operationTime; }
    public void setOperationTime(LocalDateTime operationTime) { this.operationTime = operationTime; }
}
