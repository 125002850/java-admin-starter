package com.example.admin.iam.dto;

import com.example.admin.iam.enums.IamStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class IamDeptDTO {

    private IamDeptDTO() {
    }

    @Schema(description = "部门树查询请求")
    public static class DeptTreeReqDTO {
        @Schema(description = "关键字，匹配部门编码或名称")
        public String keyword;
    }

    @Schema(description = "部门ID请求")
    public static class DeptIdReqDTO {
        @NotNull
        @Schema(description = "部门ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        public Long deptId;
    }

    @Schema(description = "创建部门请求")
    public static class DeptCreateReqDTO {
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

    @Schema(description = "更新部门请求")
    public static class DeptUpdateReqDTO extends DeptCreateReqDTO {
        @NotNull
        @Schema(description = "部门ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        public Long deptId;
    }

    @Schema(description = "部门状态更新请求")
    public static class DeptStatusUpdateReqDTO extends DeptIdReqDTO {
        @NotNull
        @Schema(description = "状态", example = "DISABLED", requiredMode = Schema.RequiredMode.REQUIRED)
        public IamStatus status;
    }

    @Schema(description = "部门节点响应")
    public static class DeptRspDTO {
        @Schema(description = "部门ID")
        public Long deptId;
        @Schema(description = "父部门ID")
        public Long parentId;
        @Schema(description = "部门编码")
        public String deptCode;
        @Schema(description = "部门名称")
        public String deptName;
        @Schema(description = "完整路径")
        public String fullPath;
        @Schema(description = "排序")
        public Integer sortOrder;
        @Schema(description = "状态")
        public String status;
        @Schema(description = "备注")
        public String remark;
        @Schema(description = "子部门")
        public List<DeptRspDTO> children = new ArrayList<>();
        @Schema(description = "创建时间")
        public LocalDateTime createTime;
        @Schema(description = "更新时间")
        public LocalDateTime updateTime;
        @Schema(description = "创建人")
        public Long createBy;
        @Schema(description = "更新人")
        public Long updateBy;
    }
}
