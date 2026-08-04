package com.oigit.admin.dict.infra.persistence.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.dict.domain.model.DictPage;
import com.oigit.admin.dict.domain.model.GlobalDictItem;
import com.oigit.admin.dict.domain.repository.GlobalDictItemRepository;
import com.oigit.admin.dict.enums.DictErrorCode;
import com.oigit.admin.dict.infra.persistence.entity.GlobalDictItemEntity;
import com.oigit.admin.dict.infra.persistence.service.GlobalDictItemPersistenceService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class MybatisGlobalDictItemRepository implements GlobalDictItemRepository {

    private final GlobalDictItemPersistenceService persistenceService;

    public MybatisGlobalDictItemRepository(GlobalDictItemPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @Override
    public boolean existsByTypeAndCode(String dictTypeCode, String dictItemCode, Long excludeId) {
        var query = Wrappers.<GlobalDictItemEntity>lambdaQuery()
                .eq(GlobalDictItemEntity::getDictTypeCode, dictTypeCode)
                .eq(GlobalDictItemEntity::getDictItemCode, dictItemCode);
        if (excludeId != null) {
            query.ne(GlobalDictItemEntity::getId, excludeId);
        }
        return persistenceService.count(query) > 0L;
    }

    @Override
    public Optional<GlobalDictItem> findById(Long id) {
        return Optional.ofNullable(persistenceService.getById(id)).map(this::toDomain);
    }

    @Override
    public List<GlobalDictItem> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return persistenceService.listByIds(ids).stream().map(this::toDomain).toList();
    }

    @Override
    public long countByTypeCode(String dictTypeCode) {
        return persistenceService.count(Wrappers.<GlobalDictItemEntity>lambdaQuery()
                .eq(GlobalDictItemEntity::getDictTypeCode, dictTypeCode));
    }

    @Override
    public void create(GlobalDictItem dictItem) {
        try {
            persistenceService.save(toEntity(dictItem));
        } catch (DuplicateKeyException ignored) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_ITEM_CODE_DUPLICATED);
        }
    }

    @Override
    public void update(GlobalDictItem dictItem) {
        try {
            persistenceService.updateById(toEntity(dictItem));
        } catch (DuplicateKeyException ignored) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_ITEM_CODE_DUPLICATED);
        }
    }

    @Override
    public boolean existsAnyByIds(List<Long> ids) {
        return !persistenceService.listByIds(ids).isEmpty();
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        persistenceService.removeByIds(ids);
    }

    @Override
    public void changeTypeCode(String oldTypeCode, String newTypeCode) {
        List<GlobalDictItemEntity> items = persistenceService.list(
                Wrappers.<GlobalDictItemEntity>lambdaQuery()
                        .eq(GlobalDictItemEntity::getDictTypeCode, oldTypeCode)
                        .orderByAsc(GlobalDictItemEntity::getId)
        );
        try {
            for (GlobalDictItemEntity item : items) {
                item.setDictTypeCode(newTypeCode);
                persistenceService.updateById(item);
            }
        } catch (DuplicateKeyException ignored) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_TYPE_CODE_CONFLICT_WITH_ITEMS);
        }
    }

    @Override
    public DictPage<GlobalDictItem> page(QueryAst queryAst) {
        Page<GlobalDictItemEntity> page = persistenceService.pageBy(queryAst);
        return new DictPage<>(page.getRecords().stream().map(this::toDomain).toList(), page.getTotal());
    }

    @Override
    public List<GlobalDictItem> listByTypeCodes(Collection<String> dictTypeCodes) {
        List<String> normalizedTypeCodes = normalize(dictTypeCodes);
        if (normalizedTypeCodes.isEmpty()) {
            return List.of();
        }
        return persistenceService.list(
                        Wrappers.<GlobalDictItemEntity>lambdaQuery()
                                .in(GlobalDictItemEntity::getDictTypeCode, normalizedTypeCodes)
                                .orderByAsc(GlobalDictItemEntity::getDictTypeCode)
                                .orderByAsc(GlobalDictItemEntity::getSortOrder)
                                .orderByAsc(GlobalDictItemEntity::getId)
                ).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Map<String, Map<String, String>> findNamesByTypeCodes(Collection<String> dictTypeCodes) {
        List<String> normalizedTypeCodes = normalize(dictTypeCodes);
        if (normalizedTypeCodes.isEmpty()) {
            return Map.of();
        }
        List<GlobalDictItemEntity> items = persistenceService.list(
                Wrappers.<GlobalDictItemEntity>lambdaQuery()
                        .in(GlobalDictItemEntity::getDictTypeCode, normalizedTypeCodes)
                        .orderByAsc(GlobalDictItemEntity::getDictTypeCode)
                        .orderByAsc(GlobalDictItemEntity::getSortOrder)
                        .orderByAsc(GlobalDictItemEntity::getId)
        );

        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        for (GlobalDictItemEntity item : items) {
            if (!StringUtils.hasText(item.getDictTypeCode()) || !StringUtils.hasText(item.getDictItemCode())) {
                continue;
            }
            result.computeIfAbsent(item.getDictTypeCode(), ignored -> new LinkedHashMap<>())
                    .putIfAbsent(item.getDictItemCode(), item.getDictItemName());
        }
        return result;
    }

    @Override
    public int maxQueryComplexityScore() {
        return persistenceService.maxQueryComplexityScore();
    }

    private List<String> normalize(Collection<String> dictTypeCodes) {
        if (dictTypeCodes == null || dictTypeCodes.isEmpty()) {
            return List.of();
        }
        return dictTypeCodes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private GlobalDictItemEntity toEntity(GlobalDictItem dictItem) {
        GlobalDictItemEntity entity = new GlobalDictItemEntity();
        entity.setId(dictItem.getId());
        entity.setDictTypeCode(dictItem.getDictTypeCode());
        entity.setDictItemCode(dictItem.getDictItemCode());
        entity.setDictItemName(dictItem.getDictItemName());
        entity.setSortOrder(dictItem.getSortOrder());
        entity.setRemark(dictItem.getRemark());
        entity.setStatus(dictItem.getStatus());
        entity.setVersion(dictItem.getVersion());
        return entity;
    }

    private GlobalDictItem toDomain(GlobalDictItemEntity entity) {
        GlobalDictItem dictItem = new GlobalDictItem();
        dictItem.setId(entity.getId());
        dictItem.setDictTypeCode(entity.getDictTypeCode());
        dictItem.setDictItemCode(entity.getDictItemCode());
        dictItem.setDictItemName(entity.getDictItemName());
        dictItem.setSortOrder(entity.getSortOrder());
        dictItem.setRemark(entity.getRemark());
        dictItem.setStatus(entity.getStatus());
        dictItem.setCreateTime(entity.getCreateTime());
        dictItem.setUpdateTime(entity.getUpdateTime());
        dictItem.setCreateBy(entity.getCreateBy());
        dictItem.setUpdateBy(entity.getUpdateBy());
        dictItem.setVersion(entity.getVersion());
        return dictItem;
    }
}
