package com.oigit.admin.iam.app;

import com.oigit.admin.iam.domain.model.DataScopeSummary;
import com.oigit.admin.iam.domain.model.IamDept;
import com.oigit.admin.iam.domain.model.IamMenu;
import com.oigit.admin.iam.domain.model.IamRole;
import com.oigit.admin.iam.domain.model.IamStaff;
import com.oigit.admin.iam.domain.model.PermissionSnapshot;
import com.oigit.admin.iam.dto.rsp.CurrentStaffRspDTO;
import com.oigit.admin.iam.dto.rsp.DataScopeSummaryRspDTO;
import com.oigit.admin.iam.dto.rsp.DeptSummaryRspDTO;
import com.oigit.admin.iam.dto.rsp.MeRspDTO;
import com.oigit.admin.iam.dto.rsp.MenuNodeRspDTO;
import com.oigit.admin.iam.dto.rsp.RoleScopeRspDTO;
import com.oigit.admin.iam.dto.rsp.RoleSummaryRspDTO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PermissionSnapshotMapper {

    private PermissionSnapshotMapper() {}

    public static CurrentStaffRspDTO toCurrentStaff(IamStaff staff, DeptSummaryRspDTO dept) {
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

    public static DeptSummaryRspDTO toDeptSummary(IamDept dept) {
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

    public static RoleSummaryRspDTO toRoleSummary(IamRole role) {
        RoleSummaryRspDTO dto = new RoleSummaryRspDTO();
        dto.setRoleId(role.getId());
        dto.setRoleCode(role.getRoleCode());
        dto.setRoleName(role.getRoleName());
        dto.setStatus(role.getStatus() == null ? null : role.getStatus().getCode());
        dto.setDataScopeType(
                role.getDataScopeType() == null ? null : role.getDataScopeType().getCode());
        dto.setSortOrder(role.getSortOrder());
        dto.setSystemBuiltIn(role.getSystemBuiltIn());
        return dto;
    }

    public static MenuNodeRspDTO toMenuNode(IamMenu menu) {
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

    public static MeRspDTO toMeRspDTO(PermissionSnapshot snapshot) {
        MeRspDTO dto = new MeRspDTO();
        DeptSummaryRspDTO dept = toDeptSummary(snapshot.getDept());
        dto.setStaff(toCurrentStaff(snapshot.getStaff(), dept));
        dto.setDept(dept);
        dto.setRoles(
                snapshot.getRoles().stream().map(PermissionSnapshotMapper::toRoleSummary).toList());
        dto.setPermissions(new ArrayList<>(snapshot.getPermissions()));
        dto.setMenus(buildMenuTree(snapshot.getMenus()));
        dto.setDataScopeSummary(toDataScopeSummary(snapshot.getDataScopeSummary()));
        dto.setMustChangePassword(snapshot.isMustChangePassword());
        dto.setPermissionFingerprint(snapshot.getPermissionFingerprint());
        return dto;
    }

    private static DataScopeSummaryRspDTO toDataScopeSummary(DataScopeSummary summary) {
        DataScopeSummaryRspDTO dto = new DataScopeSummaryRspDTO();
        dto.setEffectiveType(summary.getEffectiveType());
        dto.setDeptIds(new ArrayList<>(summary.getDeptIds()));
        dto.setDeptNames(new ArrayList<>(summary.getDeptNames()));
        dto.setIncludeSelf(summary.isIncludeSelf());
        dto.setDescription(summary.getDescription());
        dto.setRoleScopes(
                summary.getRoleScopes().stream()
                        .map(
                                scope -> {
                                    RoleScopeRspDTO role = new RoleScopeRspDTO();
                                    role.setRoleId(scope.getRoleId());
                                    role.setRoleCode(scope.getRoleCode());
                                    role.setRoleName(scope.getRoleName());
                                    role.setScopeType(scope.getScopeType());
                                    role.setDeptIds(new ArrayList<>(scope.getDeptIds()));
                                    role.setDeptNames(new ArrayList<>(scope.getDeptNames()));
                                    return role;
                                })
                        .toList());
        return dto;
    }

    private static List<MenuNodeRspDTO> buildMenuTree(List<IamMenu> menus) {
        Map<Long, MenuNodeRspDTO> byId = new LinkedHashMap<>();
        menus.stream()
                .sorted(
                        Comparator.comparing(
                                        (IamMenu item) ->
                                                item.getSortOrder() == null
                                                        ? 0
                                                        : item.getSortOrder())
                                .thenComparing(IamMenu::getId))
                .forEach(menu -> byId.put(menu.getId(), PermissionSnapshotMapper.toMenuNode(menu)));
        List<MenuNodeRspDTO> roots = new ArrayList<>();
        for (IamMenu menu :
                menus.stream()
                        .sorted(
                                Comparator.comparing(
                                                (IamMenu item) ->
                                                        item.getSortOrder() == null
                                                                ? 0
                                                                : item.getSortOrder())
                                        .thenComparing(IamMenu::getId))
                        .toList()) {
            MenuNodeRspDTO node = byId.get(menu.getId());
            if (menu.getParentId() != null && byId.containsKey(menu.getParentId())) {
                byId.get(menu.getParentId()).getChildren().add(node);
            } else {
                roots.add(node);
            }
        }
        return roots;
    }
}
