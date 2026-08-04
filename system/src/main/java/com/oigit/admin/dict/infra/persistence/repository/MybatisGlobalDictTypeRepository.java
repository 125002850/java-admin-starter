package com.oigit.admin.dict.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.dict.domain.model.DictPage;
import com.oigit.admin.dict.domain.model.GlobalDictType;
import com.oigit.admin.dict.domain.repository.GlobalDictTypeRepository;
import com.oigit.admin.dict.enums.DictErrorCode;
import com.oigit.admin.dict.infra.persistence.entity.GlobalDictTypeEntity;
import com.oigit.admin.dict.infra.persistence.service.GlobalDictTypePersistenceService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisGlobalDictTypeRepository implements GlobalDictTypeRepository {

    private final GlobalDictTypePersistenceService persistenceService;

    public MybatisGlobalDictTypeRepository(GlobalDictTypePersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @Override
    public boolean existsByCode(String dictTypeCode, Long excludeId) {
        var query = Wrappers.<GlobalDictTypeEntity>lambdaQuery()
                .eq(GlobalDictTypeEntity::getDictTypeCode, dictTypeCode);
        if (excludeId != null) {
            query.ne(GlobalDictTypeEntity::getId, excludeId);
        }
        return persistenceService.count(query) > 0L;
    }

    @Override
    public Optional<GlobalDictType> findById(Long id) {
        return Optional.ofNullable(persistenceService.getById(id)).map(this::toDomain);
    }

    @Override
    public void create(GlobalDictType dictType) {
        try {
            persistenceService.save(toEntity(dictType));
        } catch (DuplicateKeyException ignored) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_TYPE_CODE_DUPLICATED);
        }
    }

    @Override
    public void update(GlobalDictType dictType) {
        try {
            persistenceService.updateById(toEntity(dictType));
        } catch (DuplicateKeyException ignored) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_TYPE_CODE_DUPLICATED);
        }
    }

    @Override
    public void deleteById(Long id) {
        persistenceService.removeById(id);
    }

    @Override
    public List<GlobalDictType> listAll(String keyword) {
        var query = Wrappers.<GlobalDictTypeEntity>lambdaQuery()
                .orderByAsc(GlobalDictTypeEntity::getId);
        if (StringUtils.hasText(keyword)) {
            query.and(wrapper -> wrapper
                    .like(GlobalDictTypeEntity::getDictTypeCode, keyword)
                    .or()
                    .like(GlobalDictTypeEntity::getDictTypeName, keyword));
        }
        return persistenceService.list(query).stream().map(this::toDomain).toList();
    }

    @Override
    public DictPage<GlobalDictType> page(QueryAst queryAst) {
        Page<GlobalDictTypeEntity> page = persistenceService.pageBy(queryAst);
        return new DictPage<>(page.getRecords().stream().map(this::toDomain).toList(), page.getTotal());
    }

    @Override
    public List<GlobalDictType> listForExport(QueryAst queryAst) {
        return persistenceService.listForExport(queryAst).stream().map(this::toDomain).toList();
    }

    @Override
    public int maxQueryComplexityScore() {
        return persistenceService.maxQueryComplexityScore();
    }

    private GlobalDictTypeEntity toEntity(GlobalDictType dictType) {
        GlobalDictTypeEntity entity = new GlobalDictTypeEntity();
        entity.setId(dictType.getId());
        entity.setDictTypeCode(dictType.getDictTypeCode());
        entity.setDictTypeName(dictType.getDictTypeName());
        entity.setRemark(dictType.getRemark());
        entity.setStatus(dictType.getStatus());
        entity.setVersion(dictType.getVersion());
        return entity;
    }

    private GlobalDictType toDomain(GlobalDictTypeEntity entity) {
        GlobalDictType dictType = new GlobalDictType();
        dictType.setId(entity.getId());
        dictType.setDictTypeCode(entity.getDictTypeCode());
        dictType.setDictTypeName(entity.getDictTypeName());
        dictType.setRemark(entity.getRemark());
        dictType.setStatus(entity.getStatus());
        dictType.setCreateTime(entity.getCreateTime());
        dictType.setUpdateTime(entity.getUpdateTime());
        dictType.setCreateBy(entity.getCreateBy());
        dictType.setUpdateBy(entity.getUpdateBy());
        dictType.setVersion(entity.getVersion());
        return dictType;
    }
}
