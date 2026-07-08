package com.demo.iam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class IamAuthDTO {

    private IamAuthDTO() {
    }

    @Schema(description = "IAM登录请求")
    public static class LoginReqDTO {
        @NotBlank
        @Schema(description = "用户名", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
        private String username;

        @NotBlank
        @Schema(description = "密码", example = "Admin@123456", requiredMode = Schema.RequiredMode.REQUIRED)
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    @Schema(description = "IAM刷新token请求")
    public static class RefreshReqDTO {
        @NotBlank
        @Schema(description = "refresh token", requiredMode = Schema.RequiredMode.REQUIRED)
        private String refreshToken;

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }

    @Schema(description = "IAM退出登录请求")
    public static class LogoutReqDTO {
        @Schema(description = "refresh token，可为空")
        private String refreshToken;

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }

    @Schema(description = "IAM修改本人密码请求")
    public static class ChangePasswordReqDTO {
        @NotBlank
        @Schema(description = "旧密码", requiredMode = Schema.RequiredMode.REQUIRED)
        private String oldPassword;

        @NotBlank
        @Schema(description = "新密码", requiredMode = Schema.RequiredMode.REQUIRED)
        private String newPassword;

        public String getOldPassword() {
            return oldPassword;
        }

        public void setOldPassword(String oldPassword) {
            this.oldPassword = oldPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }

    @Schema(description = "IAM token响应")
    public static class TokenRspDTO {
        @Schema(description = "access token", requiredMode = Schema.RequiredMode.REQUIRED)
        private String accessToken;

        @Schema(description = "refresh token", requiredMode = Schema.RequiredMode.REQUIRED)
        private String refreshToken;

        @Schema(description = "access token过期时间", example = "2026-07-08 10:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDateTime accessTokenExpiresAt;

        @Schema(description = "token类型", example = "Bearer", requiredMode = Schema.RequiredMode.REQUIRED)
        private String tokenType = "Bearer";

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public LocalDateTime getAccessTokenExpiresAt() {
            return accessTokenExpiresAt;
        }

        public void setAccessTokenExpiresAt(LocalDateTime accessTokenExpiresAt) {
            this.accessTokenExpiresAt = accessTokenExpiresAt;
        }

        public String getTokenType() {
            return tokenType;
        }

        public void setTokenType(String tokenType) {
            this.tokenType = tokenType;
        }
    }

    @Schema(description = "IAM登录响应")
    public static class LoginRspDTO extends TokenRspDTO {
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

    @Schema(description = "IAM修改密码响应")
    public static class ChangePasswordRspDTO extends TokenRspDTO {
        @Schema(description = "是否必须修改密码", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
        private boolean mustChangePassword;

        public boolean isMustChangePassword() {
            return mustChangePassword;
        }

        public void setMustChangePassword(boolean mustChangePassword) {
            this.mustChangePassword = mustChangePassword;
        }
    }

    @Schema(description = "IAM当前用户权限快照")
    public static class MeRspDTO {
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

    @Schema(description = "IAM当前员工摘要")
    public static class CurrentStaffRspDTO {
        private Long staffId;
        private String username;
        private String staffCode;
        private String staffName;
        private String avatar;
        private String phone;
        private String email;
        private String status;
        private Long deptId;
        private String deptName;

        public Long getStaffId() {
            return staffId;
        }

        public void setStaffId(Long staffId) {
            this.staffId = staffId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getStaffCode() {
            return staffCode;
        }

        public void setStaffCode(String staffCode) {
            this.staffCode = staffCode;
        }

        public String getStaffName() {
            return staffName;
        }

        public void setStaffName(String staffName) {
            this.staffName = staffName;
        }

        public String getAvatar() {
            return avatar;
        }

        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Long getDeptId() {
            return deptId;
        }

        public void setDeptId(Long deptId) {
            this.deptId = deptId;
        }

        public String getDeptName() {
            return deptName;
        }

        public void setDeptName(String deptName) {
            this.deptName = deptName;
        }
    }

    @Schema(description = "IAM部门摘要")
    public static class DeptSummaryRspDTO {
        private Long deptId;
        private String deptCode;
        private String deptName;
        private Long parentId;
        private String fullPath;
        private String status;

        public Long getDeptId() {
            return deptId;
        }

        public void setDeptId(Long deptId) {
            this.deptId = deptId;
        }

        public String getDeptCode() {
            return deptCode;
        }

        public void setDeptCode(String deptCode) {
            this.deptCode = deptCode;
        }

        public String getDeptName() {
            return deptName;
        }

        public void setDeptName(String deptName) {
            this.deptName = deptName;
        }

        public Long getParentId() {
            return parentId;
        }

        public void setParentId(Long parentId) {
            this.parentId = parentId;
        }

        public String getFullPath() {
            return fullPath;
        }

        public void setFullPath(String fullPath) {
            this.fullPath = fullPath;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    @Schema(description = "IAM角色摘要")
    public static class RoleSummaryRspDTO {
        private Long roleId;
        private String roleCode;
        private String roleName;
        private String status;
        private String dataScopeType;
        private Integer sortOrder;
        private Boolean systemBuiltIn;

        public Long getRoleId() {
            return roleId;
        }

        public void setRoleId(Long roleId) {
            this.roleId = roleId;
        }

        public String getRoleCode() {
            return roleCode;
        }

        public void setRoleCode(String roleCode) {
            this.roleCode = roleCode;
        }

        public String getRoleName() {
            return roleName;
        }

        public void setRoleName(String roleName) {
            this.roleName = roleName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getDataScopeType() {
            return dataScopeType;
        }

        public void setDataScopeType(String dataScopeType) {
            this.dataScopeType = dataScopeType;
        }

        public Integer getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
        }

        public Boolean getSystemBuiltIn() {
            return systemBuiltIn;
        }

        public void setSystemBuiltIn(Boolean systemBuiltIn) {
            this.systemBuiltIn = systemBuiltIn;
        }
    }

    @Schema(description = "IAM菜单节点")
    public static class MenuNodeRspDTO {
        private Long menuId;
        private Long parentId;
        private String menuCode;
        private String menuKey;
        private String menuName;
        private String menuType;
        private String routePath;
        private String componentPath;
        private String icon;
        private Integer sortOrder;
        private boolean hidden;
        private boolean cached;
        private String status;
        private String permissionCode;
        private List<MenuNodeRspDTO> children = new ArrayList<>();

        public Long getMenuId() {
            return menuId;
        }

        public void setMenuId(Long menuId) {
            this.menuId = menuId;
        }

        public Long getParentId() {
            return parentId;
        }

        public void setParentId(Long parentId) {
            this.parentId = parentId;
        }

        public String getMenuCode() {
            return menuCode;
        }

        public void setMenuCode(String menuCode) {
            this.menuCode = menuCode;
        }

        public String getMenuKey() {
            return menuKey;
        }

        public void setMenuKey(String menuKey) {
            this.menuKey = menuKey;
        }

        public String getMenuName() {
            return menuName;
        }

        public void setMenuName(String menuName) {
            this.menuName = menuName;
        }

        public String getMenuType() {
            return menuType;
        }

        public void setMenuType(String menuType) {
            this.menuType = menuType;
        }

        public String getRoutePath() {
            return routePath;
        }

        public void setRoutePath(String routePath) {
            this.routePath = routePath;
        }

        public String getComponentPath() {
            return componentPath;
        }

        public void setComponentPath(String componentPath) {
            this.componentPath = componentPath;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public Integer getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
        }

        public boolean isHidden() {
            return hidden;
        }

        public void setHidden(boolean hidden) {
            this.hidden = hidden;
        }

        public boolean isCached() {
            return cached;
        }

        public void setCached(boolean cached) {
            this.cached = cached;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getPermissionCode() {
            return permissionCode;
        }

        public void setPermissionCode(String permissionCode) {
            this.permissionCode = permissionCode;
        }

        public List<MenuNodeRspDTO> getChildren() {
            return children;
        }

        public void setChildren(List<MenuNodeRspDTO> children) {
            this.children = children;
        }
    }

    @Schema(description = "IAM数据权限摘要")
    public static class DataScopeSummaryRspDTO {
        private String effectiveType;
        private List<Long> deptIds = new ArrayList<>();
        private List<String> deptNames = new ArrayList<>();
        private boolean includeSelf;
        private List<RoleScopeRspDTO> roleScopes = new ArrayList<>();
        private String description;

        public String getEffectiveType() {
            return effectiveType;
        }

        public void setEffectiveType(String effectiveType) {
            this.effectiveType = effectiveType;
        }

        public List<Long> getDeptIds() {
            return deptIds;
        }

        public void setDeptIds(List<Long> deptIds) {
            this.deptIds = deptIds;
        }

        public List<String> getDeptNames() {
            return deptNames;
        }

        public void setDeptNames(List<String> deptNames) {
            this.deptNames = deptNames;
        }

        public boolean isIncludeSelf() {
            return includeSelf;
        }

        public void setIncludeSelf(boolean includeSelf) {
            this.includeSelf = includeSelf;
        }

        public List<RoleScopeRspDTO> getRoleScopes() {
            return roleScopes;
        }

        public void setRoleScopes(List<RoleScopeRspDTO> roleScopes) {
            this.roleScopes = roleScopes;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    @Schema(description = "IAM角色数据权限摘要")
    public static class RoleScopeRspDTO {
        private Long roleId;
        private String roleCode;
        private String roleName;
        private String scopeType;
        private List<Long> deptIds = new ArrayList<>();
        private List<String> deptNames = new ArrayList<>();

        public Long getRoleId() {
            return roleId;
        }

        public void setRoleId(Long roleId) {
            this.roleId = roleId;
        }

        public String getRoleCode() {
            return roleCode;
        }

        public void setRoleCode(String roleCode) {
            this.roleCode = roleCode;
        }

        public String getRoleName() {
            return roleName;
        }

        public void setRoleName(String roleName) {
            this.roleName = roleName;
        }

        public String getScopeType() {
            return scopeType;
        }

        public void setScopeType(String scopeType) {
            this.scopeType = scopeType;
        }

        public List<Long> getDeptIds() {
            return deptIds;
        }

        public void setDeptIds(List<Long> deptIds) {
            this.deptIds = deptIds;
        }

        public List<String> getDeptNames() {
            return deptNames;
        }

        public void setDeptNames(List<String> deptNames) {
            this.deptNames = deptNames;
        }
    }
}
