package com.oigit.admin.iam.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "员工角色分配请求")
public class StaffRolesAssignReqDTO extends StaffIdReqDTO {
    @NotNull
    @Schema(description = "角色ID集合", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> roleIds = new ArrayList<>();

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }
}
