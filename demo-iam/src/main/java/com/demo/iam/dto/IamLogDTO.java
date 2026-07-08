package com.demo.iam.dto;

import com.demo.core.web.PageReqDTO;
import com.demo.iam.dto.IamCommonDTO.DateTimeRangeReqDTO;
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
        @Schema(description = "员工姓名")
        public String staffName;
        @Schema(description = "结果：SUCCESS/FAIL")
        public String result;
        @Schema(description = "IP地址")
        public String ip;
        @Schema(description = "操作时间范围")
        public DateTimeRangeReqDTO operationTimeRange;
    }

    @Schema(description = "操作日志分页请求")
    public static class OperationLogPageReqDTO extends PageReqDTO {
        @Schema(description = "操作人ID")
        public Long operatorId;
        @Schema(description = "操作人用户名")
        public String operatorUsername;
        @Schema(description = "操作人员工姓名")
        public String operatorStaffName;
        @Schema(description = "模块")
        public String module;
        @Schema(description = "动作")
        public String action;
        @Schema(description = "是否成功")
        public Boolean success;
        @Schema(description = "请求路径")
        public String requestPath;
        @Schema(description = "操作时间范围")
        public DateTimeRangeReqDTO operationTimeRange;
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
        @Schema(description = "员工姓名")
        public String staffName;
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
