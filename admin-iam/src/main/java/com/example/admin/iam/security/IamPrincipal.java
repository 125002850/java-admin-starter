package com.example.admin.iam.security;

import com.example.admin.iam.service.PermissionSnapshot;

public class IamPrincipal {

    private final PermissionSnapshot snapshot;

    public IamPrincipal(PermissionSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public PermissionSnapshot getSnapshot() {
        return snapshot;
    }

    public Long getStaffId() {
        return snapshot.getStaffId();
    }

    public String getUsername() {
        return snapshot.getUsername();
    }

    public String getStaffName() {
        return snapshot.getStaffName();
    }

    public boolean isSuperAdmin() {
        return snapshot.isSuperAdmin();
    }

    public boolean hasPermission(String permissionCode) {
        return isSuperAdmin() || snapshot.getPermissions().contains(permissionCode);
    }
}
