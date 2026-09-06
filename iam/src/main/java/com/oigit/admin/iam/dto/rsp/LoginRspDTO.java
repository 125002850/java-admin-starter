package com.oigit.admin.iam.dto.rsp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "IAM登录响应")
public class LoginRspDTO extends TokenRspDTO {
    @Schema(description = "当前员工信息", requiredMode = Schema.RequiredMode.REQUIRED)
    private CurrentStaffRspDTO staff;

    @Schema(description = "是否必须修改密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean mustChangePassword;

    @Schema(description = "角色摘要")
    private List<RoleSummaryRspDTO> roles = new ArrayList<>();

    @Schema(description = "权限标识集合")
    private List<String> permissions = new ArrayList<>();

    @Schema(description = "授权菜单树")
    private List<MenuNodeRspDTO> menus = new ArrayList<>();

    @Schema(description = "数据权限摘要")
    private DataScopeSummaryRspDTO dataScopeSummary;

    @Schema(description = "权限快照指纹")
    private String permissionFingerprint;

    public CurrentStaffRspDTO getStaff() {
        return staff;
    }

    public void setStaff(CurrentStaffRspDTO staff) {
        this.staff = staff;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
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

    public String getPermissionFingerprint() {
        return permissionFingerprint;
    }

    public void setPermissionFingerprint(String permissionFingerprint) {
        this.permissionFingerprint = permissionFingerprint;
    }
}
