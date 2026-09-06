package com.oigit.admin.iam.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.oigit.admin.core.operator.OperatorContext;
import com.oigit.admin.iam.domain.model.IamMenu;
import com.oigit.admin.iam.domain.repository.MenuRepository;
import com.oigit.admin.iam.infra.persistence.entity.IamMenuEntity;
import com.oigit.admin.iam.infra.persistence.mapper.IamMenuMapper;

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
public class MyBatisMenuRepository implements MenuRepository {
    private final IamMenuMapper menuMapper;

    public MyBatisMenuRepository(IamMenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    public IamMenu findById(Long id) {
        return IamPersistenceConverter.toDomain(menuMapper.selectById(id));
    }

    public Map<Long, IamMenu> findByIds(Collection<Long> sourceIds) {
        Set<Long> ids = normalizeIds(sourceIds);
        if (ids.isEmpty()) {
            return Map.of();
        }
        return menuMapper.selectBatchIds(ids).stream()
                .map(IamPersistenceConverter::toDomain)
                .collect(
                        Collectors.toMap(
                                IamMenu::getId,
                                item -> item,
                                (left, ignored) -> left,
                                LinkedHashMap::new));
    }

    public List<IamMenu> listAll(String keyword) {
        var query =
                Wrappers.<IamMenuEntity>lambdaQuery()
                        .orderByAsc(IamMenuEntity::getSortOrder)
                        .orderByAsc(IamMenuEntity::getId);
        if (StringUtils.hasText(keyword)) {
            query.and(
                    wrapper ->
                            wrapper.like(IamMenuEntity::getMenuCode, keyword)
                                    .or()
                                    .like(IamMenuEntity::getMenuName, keyword)
                                    .or()
                                    .like(IamMenuEntity::getPermissionCode, keyword));
        }
        return menuMapper.selectList(query).stream()
                .map(IamPersistenceConverter::toDomain)
                .toList();
    }

    public void save(IamMenu model) {
        IamMenuEntity entity = IamPersistenceConverter.toEntity(model);
        if (entity.getId() == null) {
            menuMapper.insert(entity);
            model.setId(entity.getId());
        } else {
            menuMapper.updateById(entity);
        }
        model.setVersion(entity.getVersion());
    }

    public void delete(Long id) {
        menuMapper.softDeleteById(id, operatorId());
    }

    public boolean hasChildren(Long id) {
        return menuMapper.selectCount(
                        Wrappers.<IamMenuEntity>lambdaQuery().eq(IamMenuEntity::getParentId, id))
                > 0;
    }

    public boolean codeExists(String value, Long excludeId) {
        return menuMapper.selectCount(
                        Wrappers.<IamMenuEntity>lambdaQuery()
                                .eq(IamMenuEntity::getMenuCode, value)
                                .ne(excludeId != null, IamMenuEntity::getId, excludeId))
                > 0;
    }

    public boolean permissionExists(String value, Long excludeId) {
        return menuMapper.selectCount(
                        Wrappers.<IamMenuEntity>lambdaQuery()
                                .eq(IamMenuEntity::getPermissionCode, value)
                                .ne(excludeId != null, IamMenuEntity::getId, excludeId))
                > 0;
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
