package com.oigit.admin.iam.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oigit.admin.core.operator.OperatorContext;
import com.oigit.admin.iam.domain.model.IamRole;
import com.oigit.admin.iam.domain.model.IamStaff;
import com.oigit.admin.iam.domain.model.PageSlice;
import com.oigit.admin.iam.domain.model.PermissionSnapshot;
import com.oigit.admin.iam.domain.query.StaffQuery;
import com.oigit.admin.iam.domain.repository.StaffRepository;
import com.oigit.admin.iam.enums.DataScopeType;
import com.oigit.admin.iam.enums.IamStatus;
import com.oigit.admin.iam.infra.persistence.entity.IamRoleEntity;
import com.oigit.admin.iam.infra.persistence.entity.IamStaffEntity;
import com.oigit.admin.iam.infra.persistence.entity.IamStaffRoleEntity;
import com.oigit.admin.iam.infra.persistence.mapper.IamRoleMapper;
import com.oigit.admin.iam.infra.persistence.mapper.IamStaffMapper;
import com.oigit.admin.iam.infra.persistence.mapper.IamStaffRoleMapper;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class MyBatisStaffRepository implements StaffRepository {
    private final IamStaffMapper staffMapper;
    private final IamRoleMapper roleMapper;
    private final IamStaffRoleMapper staffRoleMapper;

    public MyBatisStaffRepository(
            IamStaffMapper staffMapper,
            IamRoleMapper roleMapper,
            IamStaffRoleMapper staffRoleMapper) {
        this.staffMapper = staffMapper;
        this.roleMapper = roleMapper;
        this.staffRoleMapper = staffRoleMapper;
    }

    public IamStaff findById(Long id) {
        return IamPersistenceConverter.toDomain(staffMapper.selectById(id));
    }

    public IamStaff findByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return IamPersistenceConverter.toDomain(
                staffMapper.selectOne(
                        Wrappers.<IamStaffEntity>lambdaQuery()
                                .eq(IamStaffEntity::getUsername, username.trim())
                                .last("limit 1")));
    }

    public void save(IamStaff model) {
        IamStaffEntity entity = IamPersistenceConverter.toEntity(model);
        if (entity.getId() == null) {
            staffMapper.insert(entity);
            model.setId(entity.getId());
        } else {
            staffMapper.updateById(entity);
        }
        model.setVersion(entity.getVersion());
    }

    public void delete(Long staffId) {
        staffRoleMapper.delete(
                Wrappers.<IamStaffRoleEntity>lambdaQuery()
                        .eq(IamStaffRoleEntity::getStaffId, staffId));
        staffMapper.softDeleteById(staffId, operatorId());
    }

    public boolean usernameExists(String value, Long excludeId) {
        return staffMapper.selectCount(
                        Wrappers.<IamStaffEntity>lambdaQuery()
                                .eq(IamStaffEntity::getUsername, value)
                                .ne(excludeId != null, IamStaffEntity::getId, excludeId))
                > 0;
    }

    public boolean staffCodeExists(String value, Long excludeId) {
        return staffMapper.selectCount(
                        Wrappers.<IamStaffEntity>lambdaQuery()
                                .eq(IamStaffEntity::getStaffCode, value)
                                .ne(excludeId != null, IamStaffEntity::getId, excludeId))
                > 0;
    }

    public boolean hasStaffInDept(Long deptId) {
        return staffMapper.selectCount(
                        Wrappers.<IamStaffEntity>lambdaQuery()
                                .eq(IamStaffEntity::getDeptId, deptId))
                > 0;
    }

    public List<IamRole> listRoles(Long staffId) {
        return listRolesByStaffIds(List.of(staffId)).getOrDefault(staffId, List.of());
    }

    public Map<Long, List<IamRole>> listRolesByStaffIds(Collection<Long> staffIds) {
        Set<Long> ids = normalizeIds(staffIds);
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<IamStaffRoleEntity> relations =
                staffRoleMapper.selectList(
                        Wrappers.<IamStaffRoleEntity>lambdaQuery()
                                .in(IamStaffRoleEntity::getStaffId, ids)
                                .orderByAsc(IamStaffRoleEntity::getId));
        Set<Long> roleIds =
                relations.stream()
                        .map(IamStaffRoleEntity::getRoleId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, IamRole> roles =
                roleMapper.selectBatchIds(roleIds).stream()
                        .map(IamPersistenceConverter::toDomain)
                        .collect(
                                Collectors.toMap(
                                        IamRole::getId,
                                        role -> role,
                                        (left, ignored) -> left,
                                        LinkedHashMap::new));
        return relations.stream()
                .filter(relation -> roles.containsKey(relation.getRoleId()))
                .collect(
                        Collectors.groupingBy(
                                IamStaffRoleEntity::getStaffId,
                                LinkedHashMap::new,
                                Collectors.mapping(
                                        relation -> roles.get(relation.getRoleId()),
                                        Collectors.toList())));
    }

    public void replaceRoles(Long staffId, Collection<Long> roleIds) {
        staffRoleMapper.delete(
                Wrappers.<IamStaffRoleEntity>lambdaQuery()
                        .eq(IamStaffRoleEntity::getStaffId, staffId));
        for (Long roleId : roleIds) {
            IamStaffRoleEntity relation = new IamStaffRoleEntity();
            relation.setStaffId(staffId);
            relation.setRoleId(roleId);
            relation.setDeleted(0L);
            staffRoleMapper.insert(relation);
        }
    }

    public Long superAdminRoleId() {
        IamRoleEntity role =
                roleMapper.selectOne(
                        Wrappers.<IamRoleEntity>lambdaQuery()
                                .eq(IamRoleEntity::getRoleCode, "SUPER_ADMIN")
                                .last("limit 1"));
        return role == null ? null : role.getId();
    }

    public boolean hasRole(Long staffId, Long roleId) {
        return roleId != null
                && staffRoleMapper.selectCount(
                                Wrappers.<IamStaffRoleEntity>lambdaQuery()
                                        .eq(IamStaffRoleEntity::getStaffId, staffId)
                                        .eq(IamStaffRoleEntity::getRoleId, roleId))
                        > 0;
    }

    public long countOtherEnabledStaffWithRole(Long staffId, Long roleId) {
        List<Long> ids =
                staffRoleMapper
                        .selectList(
                                Wrappers.<IamStaffRoleEntity>lambdaQuery()
                                        .eq(IamStaffRoleEntity::getRoleId, roleId))
                        .stream()
                        .map(IamStaffRoleEntity::getStaffId)
                        .toList();
        if (ids.isEmpty()) {
            return 0;
        }
        return staffMapper.selectCount(
                Wrappers.<IamStaffEntity>lambdaQuery()
                        .in(IamStaffEntity::getId, ids)
                        .ne(IamStaffEntity::getId, staffId)
                        .eq(IamStaffEntity::getStatus, IamStatus.ENABLED));
    }

    public PageSlice<IamStaff> page(
            StaffQuery reqDTO, PermissionSnapshot snapshot, Collection<Long> deptIds) {
        var query = Wrappers.<IamStaffEntity>lambdaQuery().orderByDesc(IamStaffEntity::getId);
        if (StringUtils.hasText(reqDTO.getKeyword())) {
            query.and(
                    wrapper ->
                            wrapper.like(IamStaffEntity::getUsername, reqDTO.getKeyword())
                                    .or()
                                    .like(IamStaffEntity::getStaffCode, reqDTO.getKeyword())
                                    .or()
                                    .like(IamStaffEntity::getStaffName, reqDTO.getKeyword())
                                    .or()
                                    .like(IamStaffEntity::getPhone, reqDTO.getKeyword()));
        }
        if (deptIds != null) {
            if (deptIds.isEmpty()) {
                query.eq(IamStaffEntity::getDeptId, -1L);
            } else {
                query.in(IamStaffEntity::getDeptId, deptIds);
            }
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
        Page<IamStaffEntity> page =
                staffMapper.selectPage(new Page<>(reqDTO.getPageNo(), reqDTO.getPageSize()), query);
        return new PageSlice<>(
                page.getRecords().stream().map(IamPersistenceConverter::toDomain).toList(),
                page.getTotal());
    }

    private void applyDataScope(
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<IamStaffEntity> query,
            PermissionSnapshot snapshot) {
        if (snapshot == null
                || snapshot.isSuperAdmin()
                || DataScopeType.ALL
                        .getCode()
                        .equals(snapshot.getDataScopeSummary().getEffectiveType())) {
            return;
        }
        Set<Long> deptIds = new LinkedHashSet<>(snapshot.getDataScopeSummary().getDeptIds());
        boolean includeSelf = snapshot.getDataScopeSummary().isIncludeSelf();
        if (deptIds.isEmpty() && !includeSelf) {
            query.eq(IamStaffEntity::getId, -1L);
            return;
        }
        query.and(
                wrapper -> {
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

    private static Set<Long> normalizeIds(Collection<Long> ids) {
        if (ids == null) {
            return Set.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0L)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Long operatorId() {
        Long id = OperatorContext.getOperatorId();
        return id == null ? 0L : id;
    }
}
