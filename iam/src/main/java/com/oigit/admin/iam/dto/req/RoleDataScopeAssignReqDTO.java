package com.oigit.admin.iam.dto.req;

import com.oigit.admin.iam.enums.DataScopeType;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "角色数据权限分配请求")
public class RoleDataScopeAssignReqDTO extends RoleIdReqDTO {
    @NotNull
    @Schema(description = "数据权限范围", requiredMode = Schema.RequiredMode.REQUIRED)
    public DataScopeType dataScopeType;

    @Schema(description = "自定义部门ID集合")
    public List<Long> deptIds = new ArrayList<>();
}
