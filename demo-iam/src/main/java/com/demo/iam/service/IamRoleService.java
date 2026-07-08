package com.demo.iam.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.demo.core.exception.BizException;
import com.demo.core.operator.OperatorContext;
import com.demo.iam.dto.IamRoleDTO.RoleCreateReqDTO;
import com.demo.iam.dto.IamRoleDTO.RoleDataScopeAssignReqDTO;
import com.demo.iam.dto.IamRoleDTO.RoleMenusAssignReqDTO;
import com.demo.iam.dto.IamRoleDTO.RolePageReqDTO;
import com.demo.iam.dto.IamRoleDTO.RoleUpdateReqDTO;
import com.demo.iam.enums.DataScopeType;
import com.demo.iam.enums.IamErrorCode;
import com.demo.iam.enums.IamStatus;
import com.demo.iam.infra.entity.IamRoleDataScopeDeptEntity;
import com.demo.iam.infra.entity.IamRoleEntity;
import com.demo.iam.infra.entity.IamRoleMenuEntity;
import com.demo.iam.infra.entity.IamStaffRoleEntity;
import com.demo.iam.infra.mapper.IamRoleDataScopeDeptMapper;
import com.demo.iam.infra.mapper.IamRoleMapper;
import com.demo.iam.infra.mapper.IamRoleMenuMapper;
import com.demo.iam.infra.mapper.IamStaffRoleMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class IamRoleService {

    private final IamRoleMapper roleMapper;
    private final IamStaffRoleMapper staffRoleMapper;
    private final IamRoleMenuMapper roleMenuMapper;
    private final IamRoleDataScopeDeptMapper roleDataScopeDeptMapper;

    public IamRoleService(
            IamRoleMapper roleMapper,
            IamStaffRoleMapper staffRoleMapper,
            IamRoleMenuMapper roleMenuMapper,
            IamRoleDataScopeDeptMapper roleDataScopeDeptMapper
    ) {
        this.roleMapper = roleMapper;
        this.staffRoleMapper = staffRoleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.roleDataScopeDeptMapper = roleDataScopeDeptMapper;
    }

    public Page<IamRoleEntity> page(RolePageReqDTO reqDTO) {
        var query = Wrappers.<IamRoleEntity>lambdaQuery()
                .orderByAsc(IamRoleEntity::getSortOrder)
                .orderByAsc(IamRoleEntity::getId);
        if (StringUtils.hasText(reqDTO.keyword)) {
            query.and(wrapper -> wrapper
                    .like(IamRoleEntity::getRoleCode, reqDTO.keyword)
                    .or()
                    .like(IamRoleEntity::getRoleName, reqDTO.keyword));
        }
        if (reqDTO.status != null) {
            query.eq(IamRoleEntity::getStatus, reqDTO.status);
        }
        return roleMapper.selectPage(new Page<>(reqDTO.getPageNo(), reqDTO.getPageSize()), query);
    }

    public IamRoleEntity requireById(Long roleId) {
        IamRoleEntity role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BizException(IamErrorCode.ROLE_NOT_FOUND);
        }
        return role;
    }

    public IamRoleEntity create(RoleCreateReqDTO reqDTO) {
        assertUnique(reqDTO.roleCode, reqDTO.roleName, null);
        IamRoleEntity entity = new IamRoleEntity();
        fill(entity, reqDTO.roleCode, reqDTO.roleName, reqDTO.sortOrder, reqDTO.status, reqDTO.dataScopeType, reqDTO.remark);
        entity.setSystemBuiltIn(false);
        entity.setDeleted(0L);
        roleMapper.insert(entity);
        return entity;
    }

    public void update(RoleUpdateReqDTO reqDTO) {
        IamRoleEntity entity = requireById(reqDTO.roleId);
        if (isSuperAdmin(entity)) {
            if (!PermissionSnapshotService.SUPER_ADMIN_ROLE_CODE.equals(reqDTO.roleCode)
                    || reqDTO.dataScopeType != DataScopeType.ALL
                    || reqDTO.status == IamStatus.DISABLED) {
                throw new BizException(IamErrorCode.ROLE_SUPER_ADMIN_PROTECTED);
            }
        }
        assertUnique(reqDTO.roleCode, reqDTO.roleName, reqDTO.roleId);
        fill(entity, reqDTO.roleCode, reqDTO.roleName, reqDTO.sortOrder, reqDTO.status, reqDTO.dataScopeType, reqDTO.remark);
        roleMapper.updateById(entity);
    }

    public void updateStatus(Long roleId, IamStatus status) {
        IamRoleEntity entity = requireById(roleId);
        if (isSuperAdmin(entity) && status == IamStatus.DISABLED) {
            throw new BizException(IamErrorCode.ROLE_SUPER_ADMIN_PROTECTED);
        }
        entity.setStatus(status);
        roleMapper.updateById(entity);
    }

    public void delete(Long roleId) {
        IamRoleEntity entity = requireById(roleId);
        if (isSuperAdmin(entity)) {
            throw new BizException(IamErrorCode.ROLE_SUPER_ADMIN_PROTECTED);
        }
        Long bindingCount = staffRoleMapper.selectCount(
                Wrappers.<IamStaffRoleEntity>lambdaQuery().eq(IamStaffRoleEntity::getRoleId, roleId)
        );
        if (bindingCount != null && bindingCount > 0L) {
            throw new BizException(IamErrorCode.ROLE_HAS_STAFF);
        }
        roleMapper.softDeleteById(roleId, operatorId());
    }

    public void assignMenus(RoleMenusAssignReqDTO reqDTO) {
        IamRoleEntity role = requireById(reqDTO.roleId);
        if (isSuperAdmin(role)) {
            throw new BizException(IamErrorCode.ROLE_SUPER_ADMIN_PROTECTED);
        }
        roleMenuMapper.delete(Wrappers.<IamRoleMenuEntity>lambdaQuery().eq(IamRoleMenuEntity::getRoleId, reqDTO.roleId));
        for (Long menuId : reqDTO.menuIds) {
            IamRoleMenuEntity entity = new IamRoleMenuEntity();
            entity.setRoleId(reqDTO.roleId);
            entity.setMenuId(menuId);
            entity.setDeleted(0L);
            roleMenuMapper.insert(entity);
        }
    }

    public void assignDataScope(RoleDataScopeAssignReqDTO reqDTO) {
        IamRoleEntity role = requireById(reqDTO.roleId);
        if (isSuperAdmin(role)) {
            throw new BizException(IamErrorCode.ROLE_SUPER_ADMIN_PROTECTED);
        }
        role.setDataScopeType(reqDTO.dataScopeType);
        roleMapper.updateById(role);
        roleDataScopeDeptMapper.delete(
                Wrappers.<IamRoleDataScopeDeptEntity>lambdaQuery().eq(IamRoleDataScopeDeptEntity::getRoleId, reqDTO.roleId)
        );
        if (reqDTO.dataScopeType == DataScopeType.CUSTOM_DEPT) {
            for (Long deptId : reqDTO.deptIds) {
                IamRoleDataScopeDeptEntity entity = new IamRoleDataScopeDeptEntity();
                entity.setRoleId(reqDTO.roleId);
                entity.setDeptId(deptId);
                entity.setDeleted(0L);
                roleDataScopeDeptMapper.insert(entity);
            }
        }
    }

    public List<Long> listMenuIds(Long roleId) {
        return roleMenuMapper.selectList(
                        Wrappers.<IamRoleMenuEntity>lambdaQuery().eq(IamRoleMenuEntity::getRoleId, roleId)
                ).stream()
                .map(IamRoleMenuEntity::getMenuId)
                .toList();
    }

    public List<Long> listDataScopeDeptIds(Long roleId) {
        return roleDataScopeDeptMapper.selectList(
                        Wrappers.<IamRoleDataScopeDeptEntity>lambdaQuery().eq(IamRoleDataScopeDeptEntity::getRoleId, roleId)
                ).stream()
                .map(IamRoleDataScopeDeptEntity::getDeptId)
                .toList();
    }

    private void assertUnique(String roleCode, String roleName, Long excludeId) {
        var codeQuery = Wrappers.<IamRoleEntity>lambdaQuery().eq(IamRoleEntity::getRoleCode, roleCode);
        var nameQuery = Wrappers.<IamRoleEntity>lambdaQuery().eq(IamRoleEntity::getRoleName, roleName);
        if (excludeId != null) {
            codeQuery.ne(IamRoleEntity::getId, excludeId);
            nameQuery.ne(IamRoleEntity::getId, excludeId);
        }
        Long codeCount = roleMapper.selectCount(codeQuery);
        if (codeCount != null && codeCount > 0L) {
            throw new BizException(IamErrorCode.ROLE_CODE_DUPLICATED);
        }
        Long nameCount = roleMapper.selectCount(nameQuery);
        if (nameCount != null && nameCount > 0L) {
            throw new BizException(IamErrorCode.ROLE_NAME_DUPLICATED);
        }
    }

    private void fill(IamRoleEntity entity, String roleCode, String roleName, Integer sortOrder, IamStatus status, DataScopeType dataScopeType, String remark) {
        entity.setRoleCode(roleCode);
        entity.setRoleName(roleName);
        entity.setSortOrder(sortOrder == null ? 0 : sortOrder);
        entity.setStatus(status == null ? IamStatus.ENABLED : status);
        entity.setDataScopeType(dataScopeType == null ? DataScopeType.SELF : dataScopeType);
        entity.setRemark(remark);
    }

    private boolean isSuperAdmin(IamRoleEntity role) {
        return PermissionSnapshotService.SUPER_ADMIN_ROLE_CODE.equals(role.getRoleCode());
    }

    private Long operatorId() {
        Long operatorId = OperatorContext.getOperatorId();
        return operatorId == null ? 0L : operatorId;
    }
}
