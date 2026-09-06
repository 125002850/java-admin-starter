package com.oigit.admin.iam.dto.rsp;

import com.oigit.admin.core.translation.Translate;
import com.oigit.admin.core.translation.TranslationTypes;
import com.oigit.admin.iam.enums.LoginEventType;
import com.oigit.admin.iam.enums.LoginFailureReason;
import com.oigit.admin.iam.enums.LoginResult;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "登录日志响应")
public class LoginLogRspDTO {
    public Long logId;

    @Translate(type = TranslationTypes.USER_NAME, targetField = "staffName")
    public Long staffId;

    public String username;

    @Schema(description = "员工姓名")
    public String staffName;

    public LoginEventType eventType;
    public LoginResult result;
    public LoginFailureReason failureReason;
    public String ip;
    public String userAgent;
    public String tokenId;
    public LocalDateTime operationTime;
}
