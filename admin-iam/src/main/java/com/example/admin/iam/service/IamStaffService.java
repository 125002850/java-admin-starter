package com.example.admin.iam.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.admin.core.exception.BizException;
import com.example.admin.core.operator.OperatorContext;
import com.example.admin.iam.dto.IamStaffDTO.StaffCreateReqDTO;
import com.example.admin.iam.dto.IamStaffDTO.StaffPageReqDTO;
import com.example.admin.iam.dto.IamStaffDTO.StaffUpdateReqDTO;
import com.example.admin.iam.enums.DataScopeType;
import com.example.admin.iam.enums.IamErrorCode;
import com.example.admin.iam.enums.IamStatus;
import com.example.admin.iam.infra.entity.IamDeptEntity;
import com.example.admin.iam.infra.entity.IamRoleEntity;
import com.example.admin.iam.infra.entity.IamStaffRoleEntity;
import com.example.admin.iam.infra.entity.IamStaffEntity;
import com.example.admin.iam.infra.mapper.IamDeptMapper;
import com.example.admin.iam.infra.mapper.IamRoleMapper;
import com.example.admin.iam.infra.mapper.IamStaffMapper;
import com.example.admin.iam.infra.mapper.IamStaffRoleMapper;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class IamStaffService {

    private final IamStaffMapper staffMapper;
    private final IamDeptMapper deptMapper;
    private final IamRoleMapper roleMapper;
    private final IamStaffRoleMapper staffRoleMapper;

    public IamStaffService(
            IamStaffMapper staffMapper,
            IamDeptMapper deptMapper,
            IamRoleMapper roleMapper,
            IamStaffRoleMapper staffRoleMapper
    ) {
        this.staffMapper = staffMapper;
        this.deptMapper = deptMapper;
        this.roleMapper = roleMapper;
        this.staffRoleMapper = staffRoleMapper;
    }

    public Page<IamStaffEntity> page(StaffPageReqDTO reqDTO, PermissionSnapshot snapshot) {
        var query = Wrappers.<IamStaffEntity>lambdaQuery()
                .orderByDesc(IamStaffEntity::getId);
        if (StringUtils.hasText(reqDTO.getKeyword())) {
            query.and(wrapper -> wrapper
                    .like(IamStaffEntity::getUsername, reqDTO.getKeyword())
                    .or()
                    .like(IamStaffEntity::getStaffCode, reqDTO.getKeyword())
                    .or()
                    .like(IamStaffEntity::getStaffName, reqDTO.getKeyword())
                    .or()
                    .like(IamStaffEntity::getPhone, reqDTO.getKeyword()));
        }
        if (reqDTO.getDeptIds() != null && !reqDTO.getDeptIds().isEmpty()) {
            query.in(IamStaffEntity::getDeptId, resolveDeptAndChildren(reqDTO.getDeptIds()));
        } else if (reqDTO.getDeptId() != null) {
            query.in(IamStaffEntity::getDeptId, resolveDeptAndChildren(List.of(reqDTO.getDeptId())));
        }
        if (reqDTO.getStatuses() != null && !reqDTO.getStatuses().isEmpty()) {
            query.in(IamStaffEntity::getStatus, reqDTO.getStatuses());
        } else if (reqDTO.getStatus() != null) {
            query.eq(IamStaffEntity::getStatus, reqDTO.getStatus());
        }
        if (StringUtils.hasText(reqDTO.getStaffCode())) {
            query.like(IamStaffEntity::getStaffCode, reqDTO.getStaffCode());
        }
        if (StringUtils.hasText(reqDTO.getUsername())) {
            query.like(IamStaffEntity::getUsername, reqDTO.getUsername());
        }
        if (StringUtils.hasText(reqDTO.getStaffName())) {
            query.like(IamStaffEntity::getStaffName, reqDTO.getStaffName());
        }
        if (reqDTO.getCreateTimeRange() != null) {
            if (reqDTO.getCreateTimeRange().getStartTime() != null) {
                query.ge(IamStaffEntity::getCreateTime, reqDTO.getCreateTimeRange().getStartTime());
            }
            if (reqDTO.getCreateTimeRange().getEndTime() != null) {
                query.le(IamStaffEntity::getCreateTime, reqDTO.getCreateTimeRange().getEndTime());
            }
        }
        applyDataScope(query, snapshot);
        return staffMapper.selectPage(new Page<>(reqDTO.getPageNo(), reqDTO.getPageSize()), query);
    }

    public IamStaffEntity findByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return staffMapper.selectOne(
                Wrappers.<IamStaffEntity>lambdaQuery()
                        .eq(IamStaffEntity::getUsername, username.trim())
                        .last("limit 1")
        );
    }

    public IamStaffEntity requireById(Long staffId) {
        IamStaffEntity staff = staffMapper.selectById(staffId);
        if (staff == null) {
            throw new BizException(IamErrorCode.STAFF_NOT_FOUND);
        }
        return staff;
    }

    public void assertUsernameAvailable(String username, Long excludeId) {
        var query = Wrappers.<IamStaffEntity>lambdaQuery().eq(IamStaffEntity::getUsername, username);
        if (excludeId != null) {
            query.ne(IamStaffEntity::getId, excludeId);
        }
        Long count = staffMapper.selectCount(query);
        if (count != null && count > 0L) {
            throw new BizException(IamErrorCode.STAFF_USERNAME_DUPLICATED);
        }
    }

    public void assertStaffCodeAvailable(String staffCode, Long excludeId) {
        var query = Wrappers.<IamStaffEntity>lambdaQuery().eq(IamStaffEntity::getStaffCode, staffCode);
        if (excludeId != null) {
            query.ne(IamStaffEntity::getId, excludeId);
        }
        Long count = staffMapper.selectCount(query);
        if (count != null && count > 0L) {
            throw new BizException(IamErrorCode.STAFF_CODE_DUPLICATED);
        }
    }

    public boolean isEnabled(IamStaffEntity staff) {
        return staff != null && staff.getStatus() == IamStatus.ENABLED;
    }

    public void updatePassword(Long staffId, String passwordHash, boolean mustChangePassword) {
        IamStaffEntity staff = requireById(staffId);
        staff.setPasswordHash(passwordHash);
        staff.setMustChangePassword(mustChangePassword);
        staff.setPasswordUpdatedTime(LocalDateTime.now());
        staffMapper.updateById(staff);
    }

    public IamStaffEntity create(StaffCreateReqDTO reqDTO, String passwordHash) {
        assertSuperAdminRoleNotRequested(reqDTO.getRoleIds(), superAdminRoleId());
        assertUsernameAvailable(reqDTO.getUsername(), null);
        assertStaffCodeAvailable(reqDTO.getStaffCode(), null);
        requireAssignableDept(reqDTO.getDeptId());
        IamStaffEntity entity = new IamStaffEntity();
        entity.setUsername(reqDTO.getUsername());
        entity.setPasswordHash(passwordHash);
        entity.setStaffCode(reqDTO.getStaffCode());
        entity.setStaffName(reqDTO.getStaffName());
        entity.setDeptId(reqDTO.getDeptId());
        entity.setPhone(reqDTO.getPhone());
        entity.setEmail(reqDTO.getEmail());
        entity.setAvatar(reqDTO.getAvatar());
        entity.setStatus(reqDTO.getStatus() == null ? IamStatus.ENABLED : reqDTO.getStatus());
        entity.setMustChangePassword(true);
        entity.setPasswordUpdatedTime(LocalDateTime.now());
        entity.setRemark(reqDTO.getRemark());
        entity.setDeleted(0L);
        staffMapper.insert(entity);
        assignRoles(entity.getId(), reqDTO.getRoleIds());
        return entity;
    }

    public void update(StaffUpdateReqDTO reqDTO) {
        IamStaffEntity entity = requireById(reqDTO.getStaffId());
        assertStaffCodeAvailable(reqDTO.getStaffCode(), reqDTO.getStaffId());
        requireAssignableDept(reqDTO.getDeptId());
        IamStatus newStatus = reqDTO.getStatus() == null ? entity.getStatus() : reqDTO.getStatus();
        if (newStatus == IamStatus.DISABLED && entity.getStatus() != IamStatus.DISABLED) {
            assertCanRemoveSuperAdminCapability(reqDTO.getStaffId());
        }
        entity.setStaffCode(reqDTO.getStaffCode());
        entity.setStaffName(reqDTO.getStaffName());
        entity.setDeptId(reqDTO.getDeptId());
        entity.setPhone(reqDTO.getPhone());
        entity.setEmail(reqDTO.getEmail());
        entity.setAvatar(reqDTO.getAvatar());
        entity.setStatus(newStatus);
        entity.setRemark(reqDTO.getRemark());
        staffMapper.updateById(entity);
    }

    public void updateStatus(Long staffId, IamStatus status) {
        IamStaffEntity entity = requireById(staffId);
        if (status == IamStatus.DISABLED) {
            assertCanRemoveSuperAdminCapability(staffId);
        }
        entity.setStatus(status);
        staffMapper.updateById(entity);
    }

    public void delete(Long staffId) {
        requireById(staffId);
        assertCanRemoveSuperAdminCapability(staffId);
        staffRoleMapper.delete(Wrappers.<IamStaffRoleEntity>lambdaQuery().eq(IamStaffRoleEntity::getStaffId, staffId));
        staffMapper.softDeleteById(staffId, operatorId());
    }

    public void assignRoles(Long staffId, List<Long> roleIds) {
        requireById(staffId);
        List<Long> safeRoleIds = roleIds == null ? List.of() : roleIds;
        Long superAdminRoleId = superAdminRoleId();
        if (isSuperAdminStaff(staffId, superAdminRoleId)) {
            throw new BizException(IamErrorCode.STAFF_SUPER_ADMIN_PROTECTED);
        }
        assertSuperAdminRoleNotRequested(safeRoleIds, superAdminRoleId);
        staffRoleMapper.delete(Wrappers.<IamStaffRoleEntity>lambdaQuery().eq(IamStaffRoleEntity::getStaffId, staffId));
        for (Long roleId : new LinkedHashSet<>(safeRoleIds)) {
            if (roleMapper.selectById(roleId) == null) {
                throw new BizException(IamErrorCode.ROLE_NOT_FOUND);
            }
            IamStaffRoleEntity entity = new IamStaffRoleEntity();
            entity.setStaffId(staffId);
            entity.setRoleId(roleId);
            entity.setDeleted(0L);
            staffRoleMapper.insert(entity);
        }
    }

    public List<IamRoleEntity> listRoles(Long staffId) {
        List<Long> roleIds = staffRoleMapper.selectList(
                        Wrappers.<IamStaffRoleEntity>lambdaQuery().eq(IamStaffRoleEntity::getStaffId, staffId)
                ).stream()
                .map(IamStaffRoleEntity::getRoleId)
                .toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectBatchIds(roleIds);
    }

    public IamDeptEntity findDept(Long deptId) {
        return deptId == null ? null : deptMapper.selectById(deptId);
    }

    private IamDeptEntity requireAssignableDept(Long deptId) {
        IamDeptEntity dept = deptMapper.selectById(deptId);
        if (dept == null) {
            throw new BizException(IamErrorCode.DEPT_NOT_FOUND);
        }
        if (dept.getStatus() == IamStatus.DISABLED) {
            throw new BizException(IamErrorCode.DEPT_DISABLED);
        }
        return dept;
    }

    public void assertInDataScope(Long targetStaffId, PermissionSnapshot snapshot) {
        if (snapshot == null || snapshot.isSuperAdmin()
                || DataScopeType.ALL.getCode().equals(snapshot.getDataScopeSummary().getEffectiveType())) {
            return;
        }
        if (snapshot.getStaffId().equals(targetStaffId) && snapshot.getDataScopeSummary().isIncludeSelf()) {
            return;
        }
        IamStaffEntity target = staffMapper.selectById(targetStaffId);
        if (target == null) {
            throw new BizException(IamErrorCode.STAFF_NOT_FOUND);
        }
        Set<Long> deptIds = new LinkedHashSet<>(snapshot.getDataScopeSummary().getDeptIds());
        if (target.getDeptId() != null && deptIds.contains(target.getDeptId())) {
            return;
        }
        throw new BizException(IamErrorCode.STAFF_OUT_OF_DATA_SCOPE);
    }

    private void applyDataScope(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<IamStaffEntity> query, PermissionSnapshot snapshot) {
        if (snapshot == null || snapshot.isSuperAdmin()
                || DataScopeType.ALL.getCode().equals(snapshot.getDataScopeSummary().getEffectiveType())) {
            return;
        }
        Set<Long> deptIds = new LinkedHashSet<>(snapshot.getDataScopeSummary().getDeptIds());
        boolean includeSelf = snapshot.getDataScopeSummary().isIncludeSelf();
        if (deptIds.isEmpty() && !includeSelf) {
            query.eq(IamStaffEntity::getId, -1L);
            return;
        }
        query.and(wrapper -> {
            boolean hasDept = !deptIds.isEmpty();
            if (hasDept) {
                wrapper.in(IamStaffEntity::getDeptId, deptIds);
            }
            if (includeSelf) {
                if (hasDept) {
                    wrapper.or();
                }
                wrapper.eq(IamStaffEntity::getId, snapshot.getStaffId());
            }
        });
    }

    private boolean isSuperAdminStaff(Long staffId) {
        return isSuperAdminStaff(staffId, superAdminRoleId());
    }

    private boolean isSuperAdminStaff(Long staffId, Long superAdminRoleId) {
        if (superAdminRoleId == null) {
            return false;
        }
        Long count = staffRoleMapper.selectCount(
                Wrappers.<IamStaffRoleEntity>lambdaQuery()
                        .eq(IamStaffRoleEntity::getStaffId, staffId)
                        .eq(IamStaffRoleEntity::getRoleId, superAdminRoleId)
        );
        return count != null && count > 0L;
    }

    private Set<Long> resolveDeptAndChildren(List<Long> rootDeptIds) {
        List<IamDeptEntity> depts = deptMapper.selectList(Wrappers.<IamDeptEntity>lambdaQuery());
        Map<Long, List<Long>> childrenByParent = depts.stream()
                .filter(dept -> dept.getParentId() != null)
                .collect(Collectors.groupingBy(
                        IamDeptEntity::getParentId,
                        LinkedHashMap::new,
                        Collectors.mapping(IamDeptEntity::getId, Collectors.toList())
                ));
        Set<Long> resolvedIds = new LinkedHashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>(rootDeptIds);
        while (!queue.isEmpty()) {
            Long deptId = queue.removeFirst();
            if (!resolvedIds.add(deptId)) {
                continue;
            }
            queue.addAll(childrenByParent.getOrDefault(deptId, List.of()));
        }
        return resolvedIds;
    }

    private void assertSuperAdminRoleNotRequested(List<Long> roleIds, Long superAdminRoleId) {
        if (superAdminRoleId != null && roleIds != null && roleIds.contains(superAdminRoleId)) {
            throw new BizException(IamErrorCode.STAFF_SUPER_ADMIN_PROTECTED);
        }
    }

    private void assertCanRemoveSuperAdminCapability(Long targetStaffId) {
        if (!isSuperAdminStaff(targetStaffId)) {
            return;
        }
        Long superAdminRoleId = superAdminRoleId();
        List<Long> staffIds = staffRoleMapper.selectList(
                        Wrappers.<IamStaffRoleEntity>lambdaQuery().eq(IamStaffRoleEntity::getRoleId, superAdminRoleId)
                ).stream()
                .map(IamStaffRoleEntity::getStaffId)
                .toList();
        if (staffIds.isEmpty()) {
            throw new BizException(IamErrorCode.STAFF_SUPER_ADMIN_REQUIRED);
        }
        Long activeCount = staffMapper.selectCount(
                Wrappers.<IamStaffEntity>lambdaQuery()
                        .in(IamStaffEntity::getId, staffIds)
                        .ne(IamStaffEntity::getId, targetStaffId)
                        .eq(IamStaffEntity::getStatus, IamStatus.ENABLED)
        );
        if (activeCount == null || activeCount == 0L) {
            throw new BizException(IamErrorCode.STAFF_SUPER_ADMIN_REQUIRED);
        }
    }

    private Long superAdminRoleId() {
        IamRoleEntity role = roleMapper.selectOne(
                Wrappers.<IamRoleEntity>lambdaQuery()
                        .eq(IamRoleEntity::getRoleCode, PermissionSnapshotService.SUPER_ADMIN_ROLE_CODE)
                        .last("limit 1")
        );
        return role == null ? null : role.getId();
    }

    private Long operatorId() {
        Long operatorId = OperatorContext.getOperatorId();
        return operatorId == null ? 0L : operatorId;
    }
}
