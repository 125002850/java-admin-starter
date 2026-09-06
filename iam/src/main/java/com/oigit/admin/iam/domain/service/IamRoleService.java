package com.oigit.admin.iam.domain.service;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.iam.domain.model.IamRole;
import com.oigit.admin.iam.domain.model.PageSlice;
import com.oigit.admin.iam.domain.query.RoleQuery;
import com.oigit.admin.iam.domain.repository.DeptRepository;
import com.oigit.admin.iam.domain.repository.MenuRepository;
import com.oigit.admin.iam.domain.repository.RoleRepository;
import com.oigit.admin.iam.enums.DataScopeType;
import com.oigit.admin.iam.enums.IamErrorCode;
import com.oigit.admin.iam.enums.IamStatus;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IamRoleService {
    private final RoleRepository roleRepository;
    private final MenuRepository menuRepository;
    private final DeptRepository deptRepository;

    public IamRoleService(
            RoleRepository roleRepository,
            MenuRepository menuRepository,
            DeptRepository deptRepository) {
        this.roleRepository = roleRepository;
        this.menuRepository = menuRepository;
        this.deptRepository = deptRepository;
    }

    public PageSlice<IamRole> page(RoleQuery query) {
        return roleRepository.page(query);
    }

    public IamRole requireById(Long id) {
        IamRole role = roleRepository.findById(id);
        if (role == null) {
            throw new BizException(IamErrorCode.ROLE_NOT_FOUND);
        }
        return role;
    }

    public IamRole create(IamRole role) {
        assertUnique(role, null);
        normalize(role);
        role.setSystemBuiltIn(false);
        roleRepository.save(role);
        return role;
    }

    public void update(IamRole changes) {
        IamRole role = requireById(changes.getId());
        if (isSuperAdmin(role)
                && (!PermissionSnapshotService.SUPER_ADMIN_ROLE_CODE.equals(changes.getRoleCode())
                        || changes.getDataScopeType() != DataScopeType.ALL
                        || changes.getStatus() == IamStatus.DISABLED)) {
            throw new BizException(IamErrorCode.ROLE_SUPER_ADMIN_PROTECTED);
        }
        assertUnique(changes, role.getId());
        normalize(changes);
        role.setRoleCode(changes.getRoleCode());
        role.setRoleName(changes.getRoleName());
        role.setSortOrder(changes.getSortOrder());
        role.setStatus(changes.getStatus());
        role.setDataScopeType(changes.getDataScopeType());
        role.setRemark(changes.getRemark());
        roleRepository.save(role);
    }

    public void updateStatus(Long roleId, IamStatus status) {
        IamRole role = requireById(roleId);
        if (isSuperAdmin(role) && status == IamStatus.DISABLED) {
            throw new BizException(IamErrorCode.ROLE_SUPER_ADMIN_PROTECTED);
        }
        role.setStatus(status);
        roleRepository.save(role);
    }

    public void delete(Long roleId) {
        assertNotSuperAdmin(requireById(roleId));
        if (roleRepository.hasStaff(roleId)) {
            throw new BizException(IamErrorCode.ROLE_HAS_STAFF);
        }
        roleRepository.delete(roleId);
    }

    public void assignMenus(Long roleId, List<Long> menuIds) {
        assertNotSuperAdmin(requireById(roleId));
        Set<Long> ids = new LinkedHashSet<>(menuIds);
        if (!menuRepository.findByIds(ids).keySet().containsAll(ids)) {
            throw new BizException(IamErrorCode.MENU_NOT_FOUND);
        }
        roleRepository.replaceMenus(roleId, ids);
    }

    public void assignDataScope(Long roleId, DataScopeType type, List<Long> deptIds) {
        IamRole role = requireById(roleId);
        assertNotSuperAdmin(role);
        Set<Long> ids = type == DataScopeType.CUSTOM_DEPT ? new LinkedHashSet<>(deptIds) : Set.of();
        if (!deptRepository.findByIds(ids).keySet().containsAll(ids)) {
            throw new BizException(IamErrorCode.DEPT_NOT_FOUND);
        }
        role.setDataScopeType(type);
        roleRepository.save(role);
        roleRepository.replaceDataScopeDepts(roleId, ids);
    }

    public List<Long> listMenuIds(Long roleId) {
        return listMenuIdsByRoleIds(List.of(roleId)).getOrDefault(roleId, List.of());
    }

    public List<Long> listDataScopeDeptIds(Long roleId) {
        return listDataScopeDeptIdsByRoleIds(List.of(roleId)).getOrDefault(roleId, List.of());
    }

    public Map<Long, List<Long>> listMenuIdsByRoleIds(Collection<Long> roleIds) {
        return roleRepository.listMenuIdsByRoleIds(roleIds);
    }

    public Map<Long, List<Long>> listDataScopeDeptIdsByRoleIds(Collection<Long> roleIds) {
        return roleRepository.listDataScopeDeptIdsByRoleIds(roleIds);
    }

    private void assertUnique(IamRole role, Long excludeId) {
        if (roleRepository.codeExists(role.getRoleCode(), excludeId)) {
            throw new BizException(IamErrorCode.ROLE_CODE_DUPLICATED);
        }
        if (roleRepository.nameExists(role.getRoleName(), excludeId)) {
            throw new BizException(IamErrorCode.ROLE_NAME_DUPLICATED);
        }
    }

    private void normalize(IamRole role) {
        if (role.getSortOrder() == null) {
            role.setSortOrder(0);
        }
        if (role.getStatus() == null) {
            role.setStatus(IamStatus.ENABLED);
        }
        if (role.getDataScopeType() == null) {
            role.setDataScopeType(DataScopeType.SELF);
        }
    }

    private boolean isSuperAdmin(IamRole role) {
        return PermissionSnapshotService.SUPER_ADMIN_ROLE_CODE.equals(role.getRoleCode());
    }

    private void assertNotSuperAdmin(IamRole role) {
        if (isSuperAdmin(role)) {
            throw new BizException(IamErrorCode.ROLE_SUPER_ADMIN_PROTECTED);
        }
    }
}
