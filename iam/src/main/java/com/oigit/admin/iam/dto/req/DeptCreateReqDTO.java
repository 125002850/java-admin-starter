package com.oigit.admin.iam.dto.req;

import com.oigit.admin.iam.enums.IamStatus;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

@Schema(description = "创建部门请求")
public class DeptCreateReqDTO {
    @Schema(description = "父部门ID")
    public Long parentId;

    @NotBlank
    @Schema(description = "部门编码", example = "RD", requiredMode = Schema.RequiredMode.REQUIRED)
    public String deptCode;

    @NotBlank
    @Schema(description = "部门名称", example = "研发部", requiredMode = Schema.RequiredMode.REQUIRED)
    public String deptName;

    @Schema(description = "排序", example = "10")
    public Integer sortOrder;

    @Schema(description = "状态", example = "ENABLED")
    public IamStatus status;

    @Schema(description = "备注")
    public String remark;
}
