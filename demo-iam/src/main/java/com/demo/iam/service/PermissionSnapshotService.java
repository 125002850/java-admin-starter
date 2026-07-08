package com.demo.iam.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.demo.core.exception.BizException;
import com.demo.iam.dto.IamAuthDTO.DataScopeSummaryRspDTO;
import com.demo.iam.dto.IamAuthDTO.DeptSummaryRspDTO;
import com.demo.iam.dto.IamAuthDTO.MenuNodeRspDTO;
import com.demo.iam.dto.IamAuthDTO.RoleScopeRspDTO;
import com.demo.iam.dto.IamAuthDTO.RoleSummaryRspDTO;
import com.demo.iam.enums.DataScopeType;
import com.demo.iam.enums.IamErrorCode;
import com.demo.iam.enums.IamStatus;
import com.demo.iam.infra.entity.IamDeptEntity;
import com.demo.iam.infra.entity.IamMenuEntity;
import com.demo.iam.infra.entity.IamRoleDataScopeDeptEntity;
import com.demo.iam.infra.entity.IamRoleEntity;
import com.demo.iam.infra.entity.IamRoleMenuEntity;
import com.demo.iam.infra.entity.IamStaffEntity;
import com.demo.iam.infra.entity.IamStaffRoleEntity;
import com.demo.iam.infra.mapper.IamDeptMapper;
import com.demo.iam.infra.mapper.IamMenuMapper;
import com.demo.iam.infra.mapper.IamRoleDataScopeDeptMapper;
import com.demo.iam.infra.mapper.IamRoleMapper;
import com.demo.iam.infra.mapper.IamRoleMenuMapper;
import com.demo.iam.infra.mapper.IamStaffMapper;
import com.demo.iam.infra.mapper.IamStaffRoleMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PermissionSnapshotService {

    public static final String SUPER_ADMIN_ROLE_CODE = "SUPER_ADMIN";

    private final IamStaffMapper staffMapper;
    private final IamDeptMapper deptMapper;
    private final IamRoleMapper roleMapper;
    private final IamMenuMapper menuMapper;
    private final IamStaffRoleMapper staffRoleMapper;
    private final IamRoleMenuMapper roleMenuMapper;
    private final IamRoleDataScopeDeptMapper roleDataScopeDeptMapper;

    public PermissionSnapshotService(
            IamStaffMapper staffMapper,
            IamDeptMapper deptMapper,
            IamRoleMapper roleMapper,
            IamMenuMapper menuMapper,
            IamStaffRoleMapper staffRoleMapper,
            IamRoleMenuMapper roleMenuMapper,
            IamRoleDataScopeDeptMapper roleDataScopeDeptMapper
    ) {
        this.staffMapper = staffMapper;
        this.deptMapper = deptMapper;
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.staffRoleMapper = staffRoleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.roleDataScopeDeptMapper = roleDataScopeDeptMapper;
    }

    @Transactional(readOnly = true)
    public PermissionSnapshot loadByStaffId(Long staffId) {
        IamStaffEntity staff = staffMapper.selectById(staffId);
        if (staff == null || staff.getStatus() != IamStatus.ENABLED) {
            throw new AuthenticationCredentialsNotFoundException("staff disabled or not found");
        }
        IamDeptEntity dept = deptMapper.selectById(staff.getDeptId());
        DeptSummaryRspDTO deptSummary = PermissionSnapshotMapper.toDeptSummary(dept);

        List<IamRoleEntity> roles = listEnabledRoles(staffId);
        boolean superAdmin = roles.stream().anyMatch(role -> SUPER_ADMIN_ROLE_CODE.equals(role.getRoleCode()));
        List<IamMenuEntity> menus = superAdmin ? listAllEnabledMenus() : listEnabledMenusByRoles(roles);
        Set<String> permissions = menus.stream()
                .map(IamMenuEntity::getPermissionCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        DataScopeSummaryRspDTO dataScopeSummary = buildDataScopeSummary(staff, roles, superAdmin);
        List<RoleSummaryRspDTO> roleSummaries = roles.stream()
                .map(PermissionSnapshotMapper::toRoleSummary)
                .toList();
        List<MenuNodeRspDTO> menuTree = buildMenuTree(menus);
        return new PermissionSnapshot(
                staff,
                deptSummary,
                roleSummaries,
                permissions,
                menuTree,
                dataScopeSummary,
                superAdmin,
                fingerprint(staff, roleSummaries, permissions, menus, dataScopeSummary)
        );
    }

    public IamStaffEntity requireStaff(Long staffId) {
        IamStaffEntity staff = staffMapper.selectById(staffId);
        if (staff == null) {
            throw new BizException(IamErrorCode.STAFF_NOT_FOUND);
        }
        return staff;
    }

    private List<IamRoleEntity> listEnabledRoles(Long staffId) {
        List<Long> roleIds = staffRoleMapper.selectList(
                        Wrappers.<IamStaffRoleEntity>lambdaQuery().eq(IamStaffRoleEntity::getStaffId, staffId)
                ).stream()
                .map(IamStaffRoleEntity::getRoleId)
                .toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectList(
                Wrappers.<IamRoleEntity>lambdaQuery()
                        .in(IamRoleEntity::getId, roleIds)
                        .eq(IamRoleEntity::getStatus, IamStatus.ENABLED)
                        .orderByAsc(IamRoleEntity::getSortOrder)
                        .orderByAsc(IamRoleEntity::getId)
        );
    }

    private List<IamMenuEntity> listAllEnabledMenus() {
        return menuMapper.selectList(
                Wrappers.<IamMenuEntity>lambdaQuery()
                        .eq(IamMenuEntity::getStatus, IamStatus.ENABLED)
                        .orderByAsc(IamMenuEntity::getSortOrder)
                        .orderByAsc(IamMenuEntity::getId)
        );
    }

    private List<IamMenuEntity> listEnabledMenusByRoles(List<IamRoleEntity> roles) {
        List<Long> roleIds = roles.stream().map(IamRoleEntity::getId).toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        Set<Long> menuIds = roleMenuMapper.selectList(
                        Wrappers.<IamRoleMenuEntity>lambdaQuery().in(IamRoleMenuEntity::getRoleId, roleIds)
                ).stream()
                .map(IamRoleMenuEntity::getMenuId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (menuIds.isEmpty()) {
            return List.of();
        }
        List<IamMenuEntity> allEnabledMenus = listAllEnabledMenus();
        Map<Long, IamMenuEntity> allById = allEnabledMenus.stream()
                .collect(Collectors.toMap(IamMenuEntity::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
        Set<Long> visibleIds = new LinkedHashSet<>(menuIds);
        for (Long menuId : menuIds) {
            IamMenuEntity current = allById.get(menuId);
            while (current != null && current.getParentId() != null) {
                visibleIds.add(current.getParentId());
                current = allById.get(current.getParentId());
            }
        }
        return allEnabledMenus.stream()
                .filter(menu -> visibleIds.contains(menu.getId()))
                .toList();
    }

    private DataScopeSummaryRspDTO buildDataScopeSummary(IamStaffEntity staff, List<IamRoleEntity> roles, boolean superAdmin) {
        DataScopeSummaryRspDTO summary = new DataScopeSummaryRspDTO();
        if (superAdmin || roles.stream().anyMatch(role -> role.getDataScopeType() == DataScopeType.ALL)) {
            summary.setEffectiveType(DataScopeType.ALL.getCode());
            summary.setIncludeSelf(false);
            summary.setDescription(DataScopeType.ALL.getDesc());
            summary.setRoleScopes(buildRoleScopes(staff, roles));
            return summary;
        }

        Set<Long> deptIds = new LinkedHashSet<>();
        boolean includeSelf = false;
        Set<DataScopeType> nonAllTypes = new LinkedHashSet<>();
        for (IamRoleEntity role : roles) {
            DataScopeType scopeType = role.getDataScopeType() == null ? DataScopeType.SELF : role.getDataScopeType();
            nonAllTypes.add(scopeType);
            if (scopeType == DataScopeType.DEPT_AND_CHILD && staff.getDeptId() != null) {
                deptIds.addAll(resolveDeptAndChildren(staff.getDeptId()));
            } else if (scopeType == DataScopeType.DEPT_ONLY && staff.getDeptId() != null) {
                deptIds.add(staff.getDeptId());
            } else if (scopeType == DataScopeType.CUSTOM_DEPT) {
                deptIds.addAll(listCustomDeptIds(role.getId()));
            } else if (scopeType == DataScopeType.SELF) {
                includeSelf = true;
            }
        }
        if (roles.isEmpty()) {
            includeSelf = true;
            nonAllTypes.add(DataScopeType.SELF);
        }
        summary.setEffectiveType(nonAllTypes.size() == 1 ? nonAllTypes.iterator().next().getCode() : DataScopeType.MIXED.getCode());
        summary.setDeptIds(new ArrayList<>(deptIds));
        summary.setDeptNames(listDeptNames(deptIds));
        summary.setIncludeSelf(includeSelf);
        summary.setRoleScopes(buildRoleScopes(staff, roles));
        summary.setDescription(describeDataScope(summary));
        return summary;
    }

    private List<RoleScopeRspDTO> buildRoleScopes(IamStaffEntity staff, List<IamRoleEntity> roles) {
        List<RoleScopeRspDTO> scopes = new ArrayList<>();
        for (IamRoleEntity role : roles) {
            DataScopeType scopeType = role.getDataScopeType() == null ? DataScopeType.SELF : role.getDataScopeType();
            RoleScopeRspDTO dto = new RoleScopeRspDTO();
            dto.setRoleId(role.getId());
            dto.setRoleCode(role.getRoleCode());
            dto.setRoleName(role.getRoleName());
            dto.setScopeType(scopeType.getCode());
            Set<Long> deptIds = new LinkedHashSet<>();
            if (scopeType == DataScopeType.DEPT_AND_CHILD && staff.getDeptId() != null) {
                deptIds.addAll(resolveDeptAndChildren(staff.getDeptId()));
            } else if (scopeType == DataScopeType.DEPT_ONLY && staff.getDeptId() != null) {
                deptIds.add(staff.getDeptId());
            } else if (scopeType == DataScopeType.CUSTOM_DEPT) {
                deptIds.addAll(listCustomDeptIds(role.getId()));
            }
            dto.setDeptIds(new ArrayList<>(deptIds));
            dto.setDeptNames(listDeptNames(deptIds));
            scopes.add(dto);
        }
        return scopes;
    }

    public List<Long> resolveDeptAndChildren(Long deptId) {
        if (deptId == null) {
            return List.of();
        }
        List<IamDeptEntity> depts = deptMapper.selectList(Wrappers.<IamDeptEntity>lambdaQuery());
        Map<Long, List<IamDeptEntity>> childrenByParent = depts.stream()
                .filter(item -> item.getParentId() != null)
                .collect(Collectors.groupingBy(IamDeptEntity::getParentId, LinkedHashMap::new, Collectors.toList()));
        List<Long> ids = new ArrayList<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        queue.add(deptId);
        while (!queue.isEmpty()) {
            Long current = queue.removeFirst();
            ids.add(current);
            for (IamDeptEntity child : childrenByParent.getOrDefault(current, List.of())) {
                queue.add(child.getId());
            }
        }
        return ids;
    }

    private List<Long> listCustomDeptIds(Long roleId) {
        return roleDataScopeDeptMapper.selectList(
                        Wrappers.<IamRoleDataScopeDeptEntity>lambdaQuery()
                                .eq(IamRoleDataScopeDeptEntity::getRoleId, roleId)
                ).stream()
                .map(IamRoleDataScopeDeptEntity::getDeptId)
                .toList();
    }

    private List<String> listDeptNames(Set<Long> deptIds) {
        if (deptIds.isEmpty()) {
            return List.of();
        }
        return deptMapper.selectBatchIds(deptIds).stream()
                .sorted(Comparator.comparing(IamDeptEntity::getId))
                .map(IamDeptEntity::getDeptName)
                .toList();
    }

    public List<MenuNodeRspDTO> buildMenuTree(List<IamMenuEntity> menus) {
        Map<Long, MenuNodeRspDTO> byId = new LinkedHashMap<>();
        menus.stream()
                .sorted(Comparator.comparing((IamMenuEntity item) -> item.getSortOrder() == null ? 0 : item.getSortOrder())
                        .thenComparing(IamMenuEntity::getId))
                .forEach(menu -> byId.put(menu.getId(), PermissionSnapshotMapper.toMenuNode(menu)));
        List<MenuNodeRspDTO> roots = new ArrayList<>();
        for (IamMenuEntity menu : menus.stream()
                .sorted(Comparator.comparing((IamMenuEntity item) -> item.getSortOrder() == null ? 0 : item.getSortOrder())
                        .thenComparing(IamMenuEntity::getId))
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

    private String describeDataScope(DataScopeSummaryRspDTO summary) {
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
            IamStaffEntity staff,
            List<RoleSummaryRspDTO> roles,
            Set<String> permissions,
            List<IamMenuEntity> menus,
            DataScopeSummaryRspDTO dataScopeSummary
    ) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String source = staff.getId() + "|" + staff.getStatus() + "|" + staff.getMustChangePassword()
                    + "|" + roles.stream().map(RoleSummaryRspDTO::getRoleCode).collect(Collectors.joining(","))
                    + "|" + String.join(",", permissions)
                    + "|" + menus.stream().map(item -> String.valueOf(item.getId())).collect(Collectors.joining(","))
                    + "|" + dataScopeSummary.getEffectiveType()
                    + "|" + dataScopeSummary.getDeptIds()
                    + "|" + dataScopeSummary.isIncludeSelf();
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
