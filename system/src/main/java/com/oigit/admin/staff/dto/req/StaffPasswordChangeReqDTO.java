package com.oigit.admin.staff.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "当前登录人修改 SSO 密码；账号由可信网关身份确定")
public record StaffPasswordChangeReqDTO(
        @NotBlank(message = "请输入当前密码")
        @Schema(description = "当前密码", format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
        String oldPassword,
        @NotBlank(message = "请输入新密码")
        @Schema(description = "新密码，强度要求由 SSO 校验", format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
        String newPassword,
        @NotBlank(message = "请再次输入新密码")
        @Schema(description = "确认新密码", format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
        String repeatPassword) {

    @Override
    public String toString() {
        return "StaffPasswordChangeReqDTO[REDACTED]";
    }
}
