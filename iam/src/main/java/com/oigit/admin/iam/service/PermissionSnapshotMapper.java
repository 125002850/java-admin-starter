package com.oigit.admin.iam.service;

import com.oigit.admin.iam.dto.IamAuthDTO.CurrentStaffRspDTO;
import com.oigit.admin.iam.dto.IamAuthDTO.DeptSummaryRspDTO;
import com.oigit.admin.iam.dto.IamAuthDTO.MenuNodeRspDTO;
import com.oigit.admin.iam.dto.IamAuthDTO.RoleSummaryRspDTO;
import com.oigit.admin.iam.infra.entity.IamDeptEntity;
import com.oigit.admin.iam.infra.entity.IamMenuEntity;
import com.oigit.admin.iam.infra.entity.IamRoleEntity;
import com.oigit.admin.iam.infra.entity.IamStaffEntity;

public final class PermissionSnapshotMapper {

    private PermissionSnapshotMapper() {
    }

    public static CurrentStaffRspDTO toCurrentStaff(IamStaffEntity staff, DeptSummaryRspDTO dept) {
        CurrentStaffRspDTO dto = new CurrentStaffRspDTO();
        dto.setStaffId(staff.getId());
        dto.setUsername(staff.getUsername());
        dto.setStaffCode(staff.getStaffCode());
        dto.setStaffName(staff.getStaffName());
        dto.setAvatar(staff.getAvatar());
        dto.setPhone(staff.getPhone());
        dto.setEmail(staff.getEmail());
        dto.setStatus(staff.getStatus() == null ? null : staff.getStatus().getCode());
        dto.setDeptId(staff.getDeptId());
        dto.setDeptName(dept == null ? null : dept.getDeptName());
        return dto;
    }

    public static DeptSummaryRspDTO toDeptSummary(IamDeptEntity dept) {
        if (dept == null) {
            return null;
        }
        DeptSummaryRspDTO dto = new DeptSummaryRspDTO();
        dto.setDeptId(dept.getId());
        dto.setDeptCode(dept.getDeptCode());
        dto.setDeptName(dept.getDeptName());
        dto.setParentId(dept.getParentId());
        dto.setFullPath(dept.getFullPath());
        dto.setStatus(dept.getStatus() == null ? null : dept.getStatus().getCode());
        return dto;
    }

    public static RoleSummaryRspDTO toRoleSummary(IamRoleEntity role) {
        RoleSummaryRspDTO dto = new RoleSummaryRspDTO();
        dto.setRoleId(role.getId());
        dto.setRoleCode(role.getRoleCode());
        dto.setRoleName(role.getRoleName());
        dto.setStatus(role.getStatus() == null ? null : role.getStatus().getCode());
        dto.setDataScopeType(role.getDataScopeType() == null ? null : role.getDataScopeType().getCode());
        dto.setSortOrder(role.getSortOrder());
        dto.setSystemBuiltIn(role.getSystemBuiltIn());
        return dto;
    }

    public static MenuNodeRspDTO toMenuNode(IamMenuEntity menu) {
        MenuNodeRspDTO dto = new MenuNodeRspDTO();
        dto.setMenuId(menu.getId());
        dto.setParentId(menu.getParentId());
        dto.setMenuCode(menu.getMenuCode());
        dto.setMenuKey(menu.getMenuCode());
        dto.setMenuName(menu.getMenuName());
        dto.setMenuType(menu.getMenuType() == null ? null : menu.getMenuType().getCode());
        dto.setRoutePath(menu.getRoutePath());
        dto.setComponentPath(menu.getComponentPath());
        dto.setIcon(menu.getIcon());
        dto.setSortOrder(menu.getSortOrder());
        dto.setHidden(Boolean.TRUE.equals(menu.getHidden()));
        dto.setCached(Boolean.TRUE.equals(menu.getCached()));
        dto.setStatus(menu.getStatus() == null ? null : menu.getStatus().getCode());
        dto.setPermissionCode(menu.getPermissionCode());
        return dto;
    }
}
