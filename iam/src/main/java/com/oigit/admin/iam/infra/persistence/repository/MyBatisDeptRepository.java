package com.oigit.admin.iam.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.oigit.admin.core.operator.OperatorContext;
import com.oigit.admin.iam.domain.model.IamDept;
import com.oigit.admin.iam.domain.repository.DeptRepository;
import com.oigit.admin.iam.infra.persistence.entity.IamDeptEntity;
import com.oigit.admin.iam.infra.persistence.mapper.IamDeptMapper;

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
public class MyBatisDeptRepository implements DeptRepository {
    private final IamDeptMapper deptMapper;

    public MyBatisDeptRepository(IamDeptMapper deptMapper) {
        this.deptMapper = deptMapper;
    }

    public IamDept findById(Long id) {
        return IamPersistenceConverter.toDomain(deptMapper.selectById(id));
    }

    public Map<Long, IamDept> findByIds(Collection<Long> sourceIds) {
        Set<Long> ids = normalizeIds(sourceIds);
        if (ids.isEmpty()) {
            return Map.of();
        }
        return deptMapper.selectBatchIds(ids).stream()
                .map(IamPersistenceConverter::toDomain)
                .collect(
                        Collectors.toMap(
                                IamDept::getId,
                                item -> item,
                                (left, ignored) -> left,
                                LinkedHashMap::new));
    }

    public List<IamDept> listAll(String keyword) {
        var query =
                Wrappers.<IamDeptEntity>lambdaQuery()
                        .orderByAsc(IamDeptEntity::getSortOrder)
                        .orderByAsc(IamDeptEntity::getId);
        if (StringUtils.hasText(keyword)) {
            query.and(
                    wrapper ->
                            wrapper.like(IamDeptEntity::getDeptCode, keyword)
                                    .or()
                                    .like(IamDeptEntity::getDeptName, keyword));
        }
        return deptMapper.selectList(query).stream()
                .map(IamPersistenceConverter::toDomain)
                .toList();
    }

    public void save(IamDept model) {
        IamDeptEntity entity = IamPersistenceConverter.toEntity(model);
        if (entity.getId() == null) {
            deptMapper.insert(entity);
            model.setId(entity.getId());
        } else {
            deptMapper.updateById(entity);
        }
        model.setVersion(entity.getVersion());
    }

    public void delete(Long id) {
        deptMapper.softDeleteById(id, operatorId());
    }

    public boolean hasChildren(Long id) {
        return deptMapper.selectCount(
                        Wrappers.<IamDeptEntity>lambdaQuery().eq(IamDeptEntity::getParentId, id))
                > 0;
    }

    public boolean codeExists(Long parentId, String code, Long excludeId) {
        return deptMapper.selectCount(
                        sameParent(parentId)
                                .eq(IamDeptEntity::getDeptCode, code)
                                .ne(excludeId != null, IamDeptEntity::getId, excludeId))
                > 0;
    }

    public boolean nameExists(Long parentId, String name, Long excludeId) {
        return deptMapper.selectCount(
                        sameParent(parentId)
                                .eq(IamDeptEntity::getDeptName, name)
                                .ne(excludeId != null, IamDeptEntity::getId, excludeId))
                > 0;
    }

    private com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<IamDeptEntity>
            sameParent(Long parentId) {
        return Wrappers.<IamDeptEntity>lambdaQuery()
                .isNull(parentId == null, IamDeptEntity::getParentId)
                .eq(parentId != null, IamDeptEntity::getParentId, parentId);
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
