package com.oigit.admin.iam.infra.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.oigit.admin.core.mybatis.BaseEntity;
import com.oigit.admin.iam.enums.OperationLogAction;
import com.oigit.admin.iam.enums.OperationLogModule;
import java.time.LocalDateTime;

@TableName("sys_operation_log")
public class IamOperationLogEntity extends BaseEntity {

    @TableId
    private Long id;

    @TableField("operator_id")
    private Long operatorId;

    @TableField("operator_username")
    private String operatorUsername;

    @TableField("operator_staff_name")
    private String operatorStaffName;

    @TableField("module")
    private OperationLogModule module;

    @TableField("action")
    private OperationLogAction action;

    @TableField("request_path")
    private String requestPath;

    @TableField("http_method")
    private String httpMethod;

    @TableField("request_summary")
    private String requestSummary;

    @TableField("response_summary")
    private String responseSummary;

    @TableField("success")
    private Boolean success;

    @TableField("error_message")
    private String errorMessage;

    @TableField("ip")
    private String ip;

    @TableField("user_agent")
    private String userAgent;

    @TableField("cost_millis")
    private Long costMillis;

    @TableField("operation_time")
    private LocalDateTime operationTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorUsername() {
        return operatorUsername;
    }

    public void setOperatorUsername(String operatorUsername) {
        this.operatorUsername = operatorUsername;
    }

    public String getOperatorStaffName() {
        return operatorStaffName;
    }

    public void setOperatorStaffName(String operatorStaffName) {
        this.operatorStaffName = operatorStaffName;
    }

    public OperationLogModule getModule() {
        return module;
    }

    public void setModule(OperationLogModule module) {
        this.module = module;
    }

    public OperationLogAction getAction() {
        return action;
    }

    public void setAction(OperationLogAction action) {
        this.action = action;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getRequestSummary() {
        return requestSummary;
    }

    public void setRequestSummary(String requestSummary) {
        this.requestSummary = requestSummary;
    }

    public String getResponseSummary() {
        return responseSummary;
    }

    public void setResponseSummary(String responseSummary) {
        this.responseSummary = responseSummary;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Long getCostMillis() {
        return costMillis;
    }

    public void setCostMillis(Long costMillis) {
        this.costMillis = costMillis;
    }

    public LocalDateTime getOperationTime() {
        return operationTime;
    }

    public void setOperationTime(LocalDateTime operationTime) {
        this.operationTime = operationTime;
    }
}
