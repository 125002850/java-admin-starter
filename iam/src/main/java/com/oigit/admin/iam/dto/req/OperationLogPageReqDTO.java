package com.oigit.admin.iam.dto.req;

import com.oigit.admin.core.web.PageReqDTO;
import com.oigit.admin.iam.enums.OperationLogAction;
import com.oigit.admin.iam.enums.OperationLogModule;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "操作日志分页请求")
public class OperationLogPageReqDTO extends PageReqDTO {
    @Schema(description = "操作人ID")
    public Long operatorId;

    @Schema(description = "操作人用户名")
    public String operatorUsername;

    @Schema(description = "操作人员工姓名")
    public String operatorStaffName;

    @Schema(description = "模块")
    public OperationLogModule module;

    @Schema(description = "动作")
    public OperationLogAction action;

    @Schema(description = "是否成功")
    public Boolean success;

    @Schema(description = "请求路径")
    public String requestPath;

    @Schema(description = "操作时间范围")
    public DateTimeRangeReqDTO operationTimeRange;
}
