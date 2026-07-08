package com.demo.iam.service;

import com.demo.iam.dto.IamAuthDTO.DataScopeSummaryRspDTO;
import com.demo.iam.dto.IamAuthDTO.DeptSummaryRspDTO;
import com.demo.iam.dto.IamAuthDTO.MeRspDTO;
import com.demo.iam.dto.IamAuthDTO.MenuNodeRspDTO;
import com.demo.iam.dto.IamAuthDTO.RoleSummaryRspDTO;
import com.demo.iam.infra.entity.IamStaffEntity;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PermissionSnapshot {

    private final IamStaffEntity staff;
    private final DeptSummaryRspDTO dept;
    private final List<RoleSummaryRspDTO> roles;
    private final Set<String> permissions;
    private final List<MenuNodeRspDTO> menus;
    private final DataScopeSummaryRspDTO dataScopeSummary;
    private final boolean superAdmin;
    private final String permissionFingerprint;

    public PermissionSnapshot(
            IamStaffEntity staff,
            DeptSummaryRspDTO dept,
            List<RoleSummaryRspDTO> roles,
            Set<String> permissions,
            List<MenuNodeRspDTO> menus,
            DataScopeSummaryRspDTO dataScopeSummary,
            boolean superAdmin,
            String permissionFingerprint
    ) {
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

    public Set<String> getPermissions() {
        return new LinkedHashSet<>(permissions);
    }

    public DataScopeSummaryRspDTO getDataScopeSummary() {
        return dataScopeSummary;
    }

    public MeRspDTO toMeRspDTO() {
        MeRspDTO dto = new MeRspDTO();
        dto.setStaff(PermissionSnapshotMapper.toCurrentStaff(staff, dept));
        dto.setDept(dept);
        dto.setRoles(new ArrayList<>(roles));
        dto.setPermissions(new ArrayList<>(permissions));
        dto.setMenus(new ArrayList<>(menus));
        dto.setDataScopeSummary(dataScopeSummary);
        dto.setMustChangePassword(isMustChangePassword());
        dto.setPermissionFingerprint(permissionFingerprint);
        return dto;
    }
}
