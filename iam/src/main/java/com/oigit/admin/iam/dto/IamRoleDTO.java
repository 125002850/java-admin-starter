package com.oigit.admin.iam.dto;

import com.oigit.admin.core.web.PageReqDTO;
import com.oigit.admin.iam.enums.DataScopeType;
import com.oigit.admin.iam.enums.IamStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class IamRoleDTO {

    private IamRoleDTO() {
    }

    @Schema(description = "角色分页请求")
    public static class RolePageReqDTO extends PageReqDTO {
        @Schema(description = "关键字，匹配角色编码或名称")
        public String keyword;
        @Schema(description = "状态")
        public IamStatus status;
    }

    @Schema(description = "角色ID请求")
    public static class RoleIdReqDTO {
        @NotNull
        @Schema(description = "角色ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        public Long roleId;
    }

    @Schema(description = "创建角色请求")
    public static class RoleCreateReqDTO {
        @NotBlank
        @Schema(description = "角色编码", example = "OPS_ADMIN", requiredMode = Schema.RequiredMode.REQUIRED)
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

    @Schema(description = "更新角色请求")
    public static class RoleUpdateReqDTO extends RoleCreateReqDTO {
        @NotNull
        @Schema(description = "角色ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        public Long roleId;
    }

    @Schema(description = "角色状态更新请求")
    public static class RoleStatusUpdateReqDTO extends RoleIdReqDTO {
        @NotNull
        @Schema(description = "状态", example = "DISABLED", requiredMode = Schema.RequiredMode.REQUIRED)
        public IamStatus status;
    }

    @Schema(description = "角色菜单分配请求")
    public static class RoleMenusAssignReqDTO extends RoleIdReqDTO {
        @NotNull
        @Schema(description = "菜单ID集合", requiredMode = Schema.RequiredMode.REQUIRED)
        public List<Long> menuIds = new ArrayList<>();
    }

    @Schema(description = "角色数据权限分配请求")
    public static class RoleDataScopeAssignReqDTO extends RoleIdReqDTO {
        @NotNull
        @Schema(description = "数据权限范围", requiredMode = Schema.RequiredMode.REQUIRED)
        public DataScopeType dataScopeType;
        @Schema(description = "自定义部门ID集合")
        public List<Long> deptIds = new ArrayList<>();
    }

    @Schema(description = "角色响应")
    public static class RoleRspDTO {
        public Long roleId;
        public String roleCode;
        public String roleName;
        public Integer sortOrder;
        public String status;
        public String dataScopeType;
        public Boolean systemBuiltIn;
        public String remark;
        public List<Long> menuIds = new ArrayList<>();
        public List<Long> dataScopeDeptIds = new ArrayList<>();
        public LocalDateTime createTime;
        public LocalDateTime updateTime;
        @Schema(description = "创建人用户名", example = "admin")
        public String createBy;
        @Schema(description = "更新人用户名", example = "admin")
        public String updateBy;
    }
}
