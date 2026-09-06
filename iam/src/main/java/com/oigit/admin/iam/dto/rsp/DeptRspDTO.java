package com.oigit.admin.iam.dto.rsp;

import com.oigit.admin.core.web.AuditRspDTO;
import com.oigit.admin.iam.enums.IamStatus;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "部门节点响应")
public class DeptRspDTO extends AuditRspDTO {
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
    public IamStatus status;

    @Schema(description = "备注")
    public String remark;

    @Schema(description = "子部门")
    public List<DeptRspDTO> children = new ArrayList<>();
}
