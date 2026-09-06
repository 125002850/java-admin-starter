package com.oigit.admin.iam.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "角色菜单分配请求")
public class RoleMenusAssignReqDTO extends RoleIdReqDTO {
    @NotNull
    @Schema(description = "菜单ID集合", requiredMode = Schema.RequiredMode.REQUIRED)
    public List<Long> menuIds = new ArrayList<>();
}
