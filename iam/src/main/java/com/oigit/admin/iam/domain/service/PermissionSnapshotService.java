package com.oigit.admin.iam.domain.service;

import static com.oigit.admin.iam.domain.service.IamDomainRules.descendants;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.iam.domain.model.DataScopeSummary;
import com.oigit.admin.iam.domain.model.IamDept;
import com.oigit.admin.iam.domain.model.IamMenu;
import com.oigit.admin.iam.domain.model.IamRole;
import com.oigit.admin.iam.domain.model.IamStaff;
import com.oigit.admin.iam.domain.model.PermissionSnapshot;
import com.oigit.admin.iam.domain.model.RoleScope;
import com.oigit.admin.iam.domain.repository.DeptRepository;
import com.oigit.admin.iam.domain.repository.MenuRepository;
import com.oigit.admin.iam.domain.repository.RoleRepository;
import com.oigit.admin.iam.domain.repository.StaffRepository;
import com.oigit.admin.iam.enums.DataScopeType;
import com.oigit.admin.iam.enums.IamErrorCode;
import com.oigit.admin.iam.enums.IamStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class PermissionSnapshotService {
    public static final String SUPER_ADMIN_ROLE_CODE = "SUPER_ADMIN";
    private final StaffRepository staffRepository;
    private final DeptRepository deptRepository;
    private final RoleRepository roleRepository;
    private final MenuRepository menuRepository;

    public PermissionSnapshotService(
            StaffRepository staffRepository,
            DeptRepository deptRepository,
            RoleRepository roleRepository,
            MenuRepository menuRepository) {
        this.staffRepository = staffRepository;
        this.deptRepository = deptRepository;
        this.roleRepository = roleRepository;
        this.menuRepository = menuRepository;
    }

    public PermissionSnapshot loadByStaffId(Long staffId) {
        IamStaff staff = staffRepository.findById(staffId);
        if (staff == null || staff.getStatus() != IamStatus.ENABLED) {
            throw new BizException(IamErrorCode.AUTH_STAFF_DISABLED);
        }
        List<IamRole> roles =
                staffRepository.listRoles(staffId).stream()
                        .filter(role -> role.getStatus() == IamStatus.ENABLED)
                        .sorted(
                                Comparator.comparing(
                                                IamRole::getSortOrder,
                                                Comparator.nullsFirst(Comparator.naturalOrder()))
                                        .thenComparing(IamRole::getId))
                        .toList();
        boolean superAdmin =
                roles.stream().anyMatch(role -> SUPER_ADMIN_ROLE_CODE.equals(role.getRoleCode()));
        List<Long> roleIds = roles.stream().map(IamRole::getId).toList();
        List<IamMenu> allMenus = filterEnabledWithEnabledAncestors(menuRepository.listAll(null));
        List<IamMenu> menus =
                superAdmin
                        ? allMenus
                        : visibleMenus(allMenus, roleRepository.listMenuIdsByRoleIds(roleIds));
        Set<String> permissions =
                menus.stream()
                        .map(IamMenu::getPermissionCode)
                        .filter(IamDomainRules::hasText)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        List<IamDept> allDepts =
                deptRepository.listAll(null).stream()
                        .sorted(Comparator.comparing(IamDept::getId))
                        .toList();
        IamDept dept =
                allDepts.stream()
                        .filter(item -> Objects.equals(item.getId(), staff.getDeptId()))
                        .findFirst()
                        .orElse(null);
        Map<Long, List<Long>> customDeptIds = roleRepository.listDataScopeDeptIdsByRoleIds(roleIds);
        DataScopeSummary summary =
                buildDataScopeSummary(staff, roles, superAdmin, customDeptIds, allDepts);
        return new PermissionSnapshot(
                staff,
                dept,
                roles,
                permissions,
                menus,
                summary,
                superAdmin,
                fingerprint(staff, roles, permissions, menus, summary));
    }

    private List<IamMenu> visibleMenus(
            List<IamMenu> allMenus, Map<Long, List<Long>> menuIdsByRole) {
        Set<Long> menuIds =
                menuIdsByRole.values().stream()
                        .flatMap(Collection::stream)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, IamMenu> allById =
                allMenus.stream().collect(Collectors.toMap(IamMenu::getId, item -> item));
        Set<Long> visibleIds = new LinkedHashSet<>(menuIds);
        for (Long menuId : menuIds) {
            IamMenu current = allById.get(menuId);
            while (current != null && current.getParentId() != null) {
                visibleIds.add(current.getParentId());
                current = allById.get(current.getParentId());
            }
        }
        return allMenus.stream().filter(menu -> visibleIds.contains(menu.getId())).toList();
    }

    private List<String> listDeptNames(Set<Long> ids, List<IamDept> allDepts) {
        return allDepts.stream()
                .filter(dept -> ids.contains(dept.getId()))
                .sorted(Comparator.comparing(IamDept::getId))
                .map(IamDept::getDeptName)
                .toList();
    }

    private List<IamMenu> filterEnabledWithEnabledAncestors(List<IamMenu> allMenus) {
        Map<Long, IamMenu> allById =
                allMenus.stream()
                        .collect(
                                Collectors.toMap(
                                        IamMenu::getId,
                                        item -> item,
                                        (a, b) -> a,
                                        LinkedHashMap::new));
        return allMenus.stream()
                .filter(
                        menu -> {
                            if (menu.getStatus() != IamStatus.ENABLED) {
                                return false;
                            }
                            IamMenu current = menu;
                            Set<Long> visited = new LinkedHashSet<>();
                            while (current.getParentId() != null) {
                                if (!visited.add(current.getId())) {
                                    return false;
                                }
                                IamMenu parent = allById.get(current.getParentId());
                                if (parent == null || parent.getStatus() != IamStatus.ENABLED) {
                                    return false;
                                }
                                current = parent;
                            }
                            return true;
                        })
                .toList();
    }

    private DataScopeSummary buildDataScopeSummary(
            IamStaff staff,
            List<IamRole> roles,
            boolean superAdmin,
            Map<Long, List<Long>> customDeptIds,
            List<IamDept> allDepts) {
        DataScopeSummary summary = new DataScopeSummary();
        if (superAdmin
                || roles.stream().anyMatch(role -> role.getDataScopeType() == DataScopeType.ALL)) {
            summary.setEffectiveType(DataScopeType.ALL.getCode());
            summary.setIncludeSelf(false);
            summary.setDescription(DataScopeType.ALL.getDesc());
            summary.setRoleScopes(buildRoleScopes(staff, roles, customDeptIds, allDepts));
            return summary;
        }

        Set<Long> deptIds = new LinkedHashSet<>();
        boolean includeSelf = false;
        Set<DataScopeType> nonAllTypes = new LinkedHashSet<>();
        for (IamRole role : roles) {
            DataScopeType scopeType =
                    role.getDataScopeType() == null ? DataScopeType.SELF : role.getDataScopeType();
            nonAllTypes.add(scopeType);
            if (scopeType == DataScopeType.DEPT_AND_CHILD && staff.getDeptId() != null) {
                deptIds.addAll(descendants(List.of(staff.getDeptId()), allDepts));
            } else if (scopeType == DataScopeType.DEPT_ONLY && staff.getDeptId() != null) {
                deptIds.add(staff.getDeptId());
            } else if (scopeType == DataScopeType.CUSTOM_DEPT) {
                deptIds.addAll(customDeptIds.getOrDefault(role.getId(), List.of()));
            } else if (scopeType == DataScopeType.SELF) {
                includeSelf = true;
            }
        }
        if (roles.isEmpty()) {
            includeSelf = true;
            nonAllTypes.add(DataScopeType.SELF);
        }
        summary.setEffectiveType(
                nonAllTypes.size() == 1
                        ? nonAllTypes.iterator().next().getCode()
                        : DataScopeType.MIXED.getCode());
        summary.setDeptIds(new ArrayList<>(deptIds));
        summary.setDeptNames(listDeptNames(deptIds, allDepts));
        summary.setIncludeSelf(includeSelf);
        summary.setRoleScopes(buildRoleScopes(staff, roles, customDeptIds, allDepts));
        summary.setDescription(describeDataScope(summary));
        return summary;
    }

    private List<RoleScope> buildRoleScopes(
            IamStaff staff,
            List<IamRole> roles,
            Map<Long, List<Long>> customDeptIds,
            List<IamDept> allDepts) {
        List<RoleScope> scopes = new ArrayList<>();
        for (IamRole role : roles) {
            DataScopeType scopeType =
                    role.getDataScopeType() == null ? DataScopeType.SELF : role.getDataScopeType();
            RoleScope dto = new RoleScope();
            dto.setRoleId(role.getId());
            dto.setRoleCode(role.getRoleCode());
            dto.setRoleName(role.getRoleName());
            dto.setScopeType(scopeType.getCode());
            Set<Long> deptIds = new LinkedHashSet<>();
            if (scopeType == DataScopeType.DEPT_AND_CHILD && staff.getDeptId() != null) {
                deptIds.addAll(descendants(List.of(staff.getDeptId()), allDepts));
            } else if (scopeType == DataScopeType.DEPT_ONLY && staff.getDeptId() != null) {
                deptIds.add(staff.getDeptId());
            } else if (scopeType == DataScopeType.CUSTOM_DEPT) {
                deptIds.addAll(customDeptIds.getOrDefault(role.getId(), List.of()));
            }
            dto.setDeptIds(new ArrayList<>(deptIds));
            dto.setDeptNames(listDeptNames(deptIds, allDepts));
            scopes.add(dto);
        }
        return scopes;
    }

    private String describeDataScope(DataScopeSummary summary) {
        if (Objects.equals(summary.getEffectiveType(), DataScopeType.ALL.getCode())) {
            return DataScopeType.ALL.getDesc();
        }
        List<String> parts = new ArrayList<>();
        if (!summary.getDeptIds().isEmpty()) {
            parts.add("部门数据");
        }
        if (summary.isIncludeSelf()) {
            parts.add(DataScopeType.SELF.getDesc());
        }
        if (parts.isEmpty()) {
            return "无可访问数据";
        }
        return String.join(" + ", parts);
    }

    private String fingerprint(
            IamStaff staff,
            List<IamRole> roles,
            Set<String> permissions,
            List<IamMenu> menus,
            DataScopeSummary dataScopeSummary) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String source =
                    staff.getId()
                            + "|"
                            + staff.getStatus()
                            + "|"
                            + staff.getMustChangePassword()
                            + "|"
                            + roles.stream()
                                    .map(IamRole::getRoleCode)
                                    .collect(Collectors.joining(","))
                            + "|"
                            + String.join(",", permissions)
                            + "|"
                            + menus.stream()
                                    .map(item -> String.valueOf(item.getId()))
                                    .collect(Collectors.joining(","))
                            + "|"
                            + dataScopeSummary.getEffectiveType()
                            + "|"
                            + dataScopeSummary.getDeptIds()
                            + "|"
                            + dataScopeSummary.isIncludeSelf();
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 12 && i < hash.length; i++) {
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("failed to create permission fingerprint", ex);
        }
    }
}
