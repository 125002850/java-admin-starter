package com.demo.iam.dto;

import com.demo.core.web.PageReqDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public final class IamLogDTO {

    private IamLogDTO() {
    }

    @Schema(description = "登录日志分页请求")
    public static class LoginLogPageReqDTO extends PageReqDTO {
        @Schema(description = "用户名")
        public String username;
        @Schema(description = "结果：SUCCESS/FAIL")
        public String result;
    }

    @Schema(description = "操作日志分页请求")
    public static class OperationLogPageReqDTO extends PageReqDTO {
        @Schema(description = "操作人ID")
        public Long operatorId;
        @Schema(description = "模块")
        public String module;
        @Schema(description = "动作")
        public String action;
    }

    @Schema(description = "日志ID请求")
    public static class LogIdReqDTO {
        @NotNull
        @Schema(description = "日志ID", requiredMode = Schema.RequiredMode.REQUIRED)
        public Long logId;
    }

    @Schema(description = "登录日志响应")
    public static class LoginLogRspDTO {
        public Long logId;
        public Long staffId;
        public String username;
        public String eventType;
        public String result;
        public String failureReason;
        public String ip;
        public String userAgent;
        public String tokenId;
        public LocalDateTime operationTime;
    }

    @Schema(description = "操作日志响应")
    public static class OperationLogRspDTO {
        public Long logId;
        public Long operatorId;
        public String operatorUsername;
        public String operatorStaffName;
        public String module;
        public String action;
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
}
