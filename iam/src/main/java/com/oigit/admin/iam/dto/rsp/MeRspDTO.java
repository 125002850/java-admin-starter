package com.oigit.admin.iam.dto.rsp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "IAM当前用户权限快照")
public class MeRspDTO {
    @Schema(description = "当前员工信息", requiredMode = Schema.RequiredMode.REQUIRED)
    private CurrentStaffRspDTO staff;

    @Schema(description = "部门摘要")
    private DeptSummaryRspDTO dept;

    @Schema(description = "角色摘要", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<RoleSummaryRspDTO> roles = new ArrayList<>();

    @Schema(description = "权限标识集合", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> permissions = new ArrayList<>();

    @Schema(description = "授权菜单树", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<MenuNodeRspDTO> menus = new ArrayList<>();

    @Schema(description = "数据权限摘要", requiredMode = Schema.RequiredMode.REQUIRED)
    private DataScopeSummaryRspDTO dataScopeSummary;

    @Schema(description = "是否必须修改密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean mustChangePassword;

    @Schema(description = "权限快照指纹")
    private String permissionFingerprint;

    public CurrentStaffRspDTO getStaff() {
        return staff;
    }

    public void setStaff(CurrentStaffRspDTO staff) {
        this.staff = staff;
    }

    public DeptSummaryRspDTO getDept() {
        return dept;
    }

    public void setDept(DeptSummaryRspDTO dept) {
        this.dept = dept;
    }

    public List<RoleSummaryRspDTO> getRoles() {
        return roles;
    }

    public void setRoles(List<RoleSummaryRspDTO> roles) {
        this.roles = roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public List<MenuNodeRspDTO> getMenus() {
        return menus;
    }

    public void setMenus(List<MenuNodeRspDTO> menus) {
        this.menus = menus;
    }

    public DataScopeSummaryRspDTO getDataScopeSummary() {
        return dataScopeSummary;
    }

    public void setDataScopeSummary(DataScopeSummaryRspDTO dataScopeSummary) {
        this.dataScopeSummary = dataScopeSummary;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public String getPermissionFingerprint() {
        return permissionFingerprint;
    }

    public void setPermissionFingerprint(String permissionFingerprint) {
        this.permissionFingerprint = permissionFingerprint;
    }
}
