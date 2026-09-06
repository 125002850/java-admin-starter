package com.oigit.admin.iam.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oigit.admin.core.operator.OperatorContext;
import com.oigit.admin.iam.domain.model.IamRole;
import com.oigit.admin.iam.domain.model.PageSlice;
import com.oigit.admin.iam.domain.query.RoleQuery;
import com.oigit.admin.iam.domain.repository.RoleRepository;
import com.oigit.admin.iam.infra.persistence.entity.IamRoleDataScopeDeptEntity;
import com.oigit.admin.iam.infra.persistence.entity.IamRoleEntity;
import com.oigit.admin.iam.infra.persistence.entity.IamRoleMenuEntity;
import com.oigit.admin.iam.infra.persistence.entity.IamStaffRoleEntity;
import com.oigit.admin.iam.infra.persistence.mapper.IamRoleDataScopeDeptMapper;
import com.oigit.admin.iam.infra.persistence.mapper.IamRoleMapper;
import com.oigit.admin.iam.infra.persistence.mapper.IamRoleMenuMapper;
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
public class MyBatisRoleRepository implements RoleRepository {
    private final IamRoleMapper roleMapper;
    private final IamStaffRoleMapper staffRoleMapper;
    private final IamRoleMenuMapper roleMenuMapper;
    private final IamRoleDataScopeDeptMapper roleDataScopeDeptMapper;

    public MyBatisRoleRepository(
            IamRoleMapper roleMapper,
            IamStaffRoleMapper staffRoleMapper,
            IamRoleMenuMapper roleMenuMapper,
            IamRoleDataScopeDeptMapper roleDataScopeDeptMapper) {
        this.roleMapper = roleMapper;
        this.staffRoleMapper = staffRoleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.roleDataScopeDeptMapper = roleDataScopeDeptMapper;
    }

    public IamRole findById(Long id) {
        return IamPersistenceConverter.toDomain(roleMapper.selectById(id));
    }

    public Map<Long, IamRole> findByIds(Collection<Long> sourceIds) {
        Set<Long> ids = normalizeIds(sourceIds);
        if (ids.isEmpty()) {
            return Map.of();
        }
        return roleMapper.selectBatchIds(ids).stream()
                .map(IamPersistenceConverter::toDomain)
                .collect(Collectors.toMap(IamRole::getId, item -> item));
    }

    public void save(IamRole model) {
        IamRoleEntity entity = IamPersistenceConverter.toEntity(model);
        if (entity.getId() == null) {
            roleMapper.insert(entity);
            model.setId(entity.getId());
        } else {
            roleMapper.updateById(entity);
        }
        model.setVersion(entity.getVersion());
    }

    public void delete(Long roleId) {
        roleMapper.softDeleteById(roleId, operatorId());
    }

    public boolean hasStaff(Long roleId) {
        return staffRoleMapper.selectCount(
                        Wrappers.<IamStaffRoleEntity>lambdaQuery()
                                .eq(IamStaffRoleEntity::getRoleId, roleId))
                > 0;
    }

    public boolean codeExists(String value, Long excludeId) {
        return roleMapper.selectCount(
                        Wrappers.<IamRoleEntity>lambdaQuery()
                                .eq(IamRoleEntity::getRoleCode, value)
                                .ne(excludeId != null, IamRoleEntity::getId, excludeId))
                > 0;
    }

    public boolean nameExists(String value, Long excludeId) {
        return roleMapper.selectCount(
                        Wrappers.<IamRoleEntity>lambdaQuery()
                                .eq(IamRoleEntity::getRoleName, value)
                                .ne(excludeId != null, IamRoleEntity::getId, excludeId))
                > 0;
    }

    public void replaceMenus(Long roleId, Collection<Long> ids) {
        roleMenuMapper.delete(
                Wrappers.<IamRoleMenuEntity>lambdaQuery().eq(IamRoleMenuEntity::getRoleId, roleId));
        for (Long id : ids) {
            IamRoleMenuEntity entity = new IamRoleMenuEntity();
            entity.setRoleId(roleId);
            entity.setMenuId(id);
            entity.setDeleted(0L);
            roleMenuMapper.insert(entity);
        }
    }

    public Map<Long, List<Long>> listMenuIdsByRoleIds(Collection<Long> sourceIds) {
        Set<Long> ids = normalizeIds(sourceIds);
        if (ids.isEmpty()) {
            return Map.of();
        }
        return roleMenuMapper
                .selectList(
                        Wrappers.<IamRoleMenuEntity>lambdaQuery()
                                .in(IamRoleMenuEntity::getRoleId, ids)
                                .orderByAsc(IamRoleMenuEntity::getId))
                .stream()
                .collect(
                        Collectors.groupingBy(
                                IamRoleMenuEntity::getRoleId,
                                LinkedHashMap::new,
                                Collectors.mapping(
                                        IamRoleMenuEntity::getMenuId, Collectors.toList())));
    }

    public void replaceDataScopeDepts(Long roleId, Collection<Long> ids) {
        roleDataScopeDeptMapper.delete(
                Wrappers.<IamRoleDataScopeDeptEntity>lambdaQuery()
                        .eq(IamRoleDataScopeDeptEntity::getRoleId, roleId));
        for (Long id : ids) {
            IamRoleDataScopeDeptEntity entity = new IamRoleDataScopeDeptEntity();
            entity.setRoleId(roleId);
            entity.setDeptId(id);
            entity.setDeleted(0L);
            roleDataScopeDeptMapper.insert(entity);
        }
    }

    public Map<Long, List<Long>> listDataScopeDeptIdsByRoleIds(Collection<Long> sourceIds) {
        Set<Long> ids = normalizeIds(sourceIds);
        if (ids.isEmpty()) {
            return Map.of();
        }
        return roleDataScopeDeptMapper
                .selectList(
                        Wrappers.<IamRoleDataScopeDeptEntity>lambdaQuery()
                                .in(IamRoleDataScopeDeptEntity::getRoleId, ids)
                                .orderByAsc(IamRoleDataScopeDeptEntity::getId))
                .stream()
                .collect(
                        Collectors.groupingBy(
                                IamRoleDataScopeDeptEntity::getRoleId,
                                LinkedHashMap::new,
                                Collectors.mapping(
                                        IamRoleDataScopeDeptEntity::getDeptId,
                                        Collectors.toList())));
    }

    public PageSlice<IamRole> page(RoleQuery reqDTO) {
        var query =
                Wrappers.<IamRoleEntity>lambdaQuery()
                        .orderByAsc(IamRoleEntity::getSortOrder)
                        .orderByAsc(IamRoleEntity::getId);
        if (StringUtils.hasText(reqDTO.keyword)) {
            query.and(
                    wrapper ->
                            wrapper.like(IamRoleEntity::getRoleCode, reqDTO.keyword)
                                    .or()
                                    .like(IamRoleEntity::getRoleName, reqDTO.keyword));
        }
        if (reqDTO.status != null) {
            query.eq(IamRoleEntity::getStatus, reqDTO.status);
        }
        Page<IamRoleEntity> page =
                roleMapper.selectPage(new Page<>(reqDTO.getPageNo(), reqDTO.getPageSize()), query);
        return new PageSlice<>(
                page.getRecords().stream().map(IamPersistenceConverter::toDomain).toList(),
                page.getTotal());
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
