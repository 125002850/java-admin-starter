package com.oigit.admin.iam.dto.rsp;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "IAM修改密码响应")
public class ChangePasswordRspDTO extends TokenRspDTO {
    @Schema(
            description = "是否必须修改密码",
            example = "false",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean mustChangePassword;

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }
}
