package com.oigit.admin.operationaudit.dto.rsp;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "操作日志详情")
public class OperationAuditLogDetailRspDTO extends OperationAuditLogRspDTO {

    private String operatorUsername;
    private String operatorRealName;
    private String requestMethod;
    private String requestPath;
    private String traceId;
    private String requestParams;
    private Integer httpStatus;
    private Integer resultCode;
    private String errorMessage;

    public String getOperatorUsername() { return operatorUsername; }
    public void setOperatorUsername(String operatorUsername) { this.operatorUsername = operatorUsername; }
    public String getOperatorRealName() { return operatorRealName; }
    public void setOperatorRealName(String operatorRealName) { this.operatorRealName = operatorRealName; }
    public String getRequestMethod() { return requestMethod; }
    public void setRequestMethod(String requestMethod) { this.requestMethod = requestMethod; }
    public String getRequestPath() { return requestPath; }
    public void setRequestPath(String requestPath) { this.requestPath = requestPath; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getRequestParams() { return requestParams; }
    public void setRequestParams(String requestParams) { this.requestParams = requestParams; }
    public Integer getHttpStatus() { return httpStatus; }
    public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }
    public Integer getResultCode() { return resultCode; }
    public void setResultCode(Integer resultCode) { this.resultCode = resultCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
