package com.oigit.admin.iam.dto.req;

import com.oigit.admin.iam.enums.DataScopeType;
import com.oigit.admin.iam.enums.IamStatus;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

@Schema(description = "创建角色请求")
public class RoleCreateReqDTO {
    @NotBlank
    @Schema(
            description = "角色编码",
            example = "OPS_ADMIN",
            requiredMode = Schema.RequiredMode.REQUIRED)
    public String roleCode;

    @NotBlank
    @Schema(description = "角色名称", example = "运营管理员", requiredMode = Schema.RequiredMode.REQUIRED)
    public String roleName;

    @Schema(description = "排序")
    public Integer sortOrder;

    @Schema(description = "状态")
    public IamStatus status;

    @Schema(description = "数据权限范围")
    public DataScopeType dataScopeType;

    @Schema(description = "备注")
    public String remark;
}
