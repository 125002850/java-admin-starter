package com.oigit.admin.iam.domain.model;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PermissionSnapshot {
    private final IamStaff staff;
    private final IamDept dept;
    private final List<IamRole> roles;
    private final Set<String> permissions;
    private final List<IamMenu> menus;
    private final DataScopeSummary dataScopeSummary;
    private final boolean superAdmin;
    private final String permissionFingerprint;

    public PermissionSnapshot(
            IamStaff staff,
            IamDept dept,
            List<IamRole> roles,
            Set<String> permissions,
            List<IamMenu> menus,
            DataScopeSummary dataScopeSummary,
            boolean superAdmin,
            String permissionFingerprint) {
        this.staff = staff;
        this.dept = dept;
        this.roles = List.copyOf(roles);
        this.permissions = new LinkedHashSet<>(permissions);
        this.menus = List.copyOf(menus);
        this.dataScopeSummary = dataScopeSummary;
        this.superAdmin = superAdmin;
        this.permissionFingerprint = permissionFingerprint;
    }

    public Long getStaffId() {
        return staff.getId();
    }

    public String getUsername() {
        return staff.getUsername();
    }

    public String getStaffName() {
        return staff.getStaffName();
    }

    public Long getDeptId() {
        return staff.getDeptId();
    }

    public boolean isMustChangePassword() {
        return Boolean.TRUE.equals(staff.getMustChangePassword());
    }

    public boolean isSuperAdmin() {
        return superAdmin;
    }

    public IamStaff getStaff() {
        return staff;
    }

    public IamDept getDept() {
        return dept;
    }

    public List<IamRole> getRoles() {
        return roles;
    }

    public List<IamMenu> getMenus() {
        return menus;
    }

    public Set<String> getPermissions() {
        return new LinkedHashSet<>(permissions);
    }

    public DataScopeSummary getDataScopeSummary() {
        return dataScopeSummary;
    }

    public String getPermissionFingerprint() {
        return permissionFingerprint;
    }
}
