package com.oigit.admin.iam.dto.req;

import com.oigit.admin.core.web.PageReqDTO;
import com.oigit.admin.iam.enums.LoginResult;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "登录日志分页请求")
public class LoginLogPageReqDTO extends PageReqDTO {
    @Schema(description = "用户名")
    public String username;

    @Schema(description = "员工姓名")
    public String staffName;

    @Schema(description = "结果：SUCCESS/FAIL")
    public LoginResult result;

    @Schema(description = "IP地址")
    public String ip;

    @Schema(description = "操作时间范围")
    public DateTimeRangeReqDTO operationTimeRange;
}
