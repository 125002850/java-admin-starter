package com.demo.mdm.dict.service;

import java.util.Collections;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.core.exception.BizException;
import com.demo.core.query.ast.QueryAst;
import com.demo.core.query.executor.MybatisPlusQueryExecutor;
import com.demo.core.query.scene.SceneQueryDefinition;
import com.demo.core.enums.EnableStatusEnum;
import com.demo.mdm.dict.enums.DictErrorCode;
import com.demo.mdm.dict.infra.entity.GlobalDictItemEntity;
import com.demo.mdm.dict.infra.entity.GlobalDictTypeEntity;
import com.demo.mdm.dict.infra.mapper.GlobalDictItemMapper;
import com.demo.mdm.dict.infra.mapper.GlobalDictTypeMapper;

@Service
public class DictService {

    private final GlobalDictTypeMapper globalDictTypeMapper;
    private final GlobalDictItemMapper globalDictItemMapper;
    private final MybatisPlusQueryExecutor mybatisPlusQueryExecutor;

    public DictService(
            GlobalDictTypeMapper globalDictTypeMapper,
            GlobalDictItemMapper globalDictItemMapper,
            MybatisPlusQueryExecutor mybatisPlusQueryExecutor
    ) {
        this.globalDictTypeMapper = globalDictTypeMapper;
        this.globalDictItemMapper = globalDictItemMapper;
        this.mybatisPlusQueryExecutor = mybatisPlusQueryExecutor;
    }

    public void createGlobalType(String dictTypeCode, String dictTypeName) {
        if (globalDictTypeCodeExists(dictTypeCode, null)) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_TYPE_CODE_DUPLICATED);
        }

        GlobalDictTypeEntity entity = new GlobalDictTypeEntity();
        entity.setDictTypeCode(dictTypeCode);
        entity.setDictTypeName(dictTypeName);
        entity.setDeleted(0L);
        try {
            globalDictTypeMapper.insert(entity);
        } catch (DuplicateKeyException ignored) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_TYPE_CODE_DUPLICATED);
        }
    }

    public Page<GlobalDictTypeEntity> listGlobalTypes(String keyword, long pageNo, long pageSize) {
        var query = Wrappers.<GlobalDictTypeEntity>lambdaQuery()
                .orderByAsc(GlobalDictTypeEntity::getId);
        if (StringUtils.hasText(keyword)) {
            query.and(wrapper -> wrapper
                    .like(GlobalDictTypeEntity::getDictTypeCode, keyword)
                    .or()
                    .like(GlobalDictTypeEntity::getDictTypeName, keyword));
        }
        Page<GlobalDictTypeEntity> page = new Page<>(pageNo, pageSize);
        return globalDictTypeMapper.selectPage(page, query);
    }

    public Page<GlobalDictTypeEntity> pageGlobalTypes(
            QueryAst queryAst,
            SceneQueryDefinition<GlobalDictTypeEntity> sceneQueryDefinition
    ) {
        return mybatisPlusQueryExecutor.selectPage(globalDictTypeMapper, queryAst, sceneQueryDefinition);
    }

    public Page<GlobalDictItemEntity> pageGlobalItems(
            QueryAst queryAst,
            SceneQueryDefinition<GlobalDictItemEntity> sceneQueryDefinition
    ) {
        return mybatisPlusQueryExecutor.selectPage(globalDictItemMapper, queryAst, sceneQueryDefinition);
    }

    public List<GlobalDictTypeEntity> listGlobalTypesForExport(
            QueryAst queryAst,
            SceneQueryDefinition<GlobalDictTypeEntity> sceneQueryDefinition
    ) {
        QueryAst exportQueryAst = new QueryAst();
        exportQueryAst.setRoot(queryAst.getRoot());
        exportQueryAst.setSorts(queryAst.getSorts());
        exportQueryAst.setPageNo(1L);
        exportQueryAst.setPageSize((long) sceneQueryDefinition.maxExportRows() + 1L);
        Page<GlobalDictTypeEntity> page = mybatisPlusQueryExecutor.selectPage(
                globalDictTypeMapper,
                exportQueryAst,
                sceneQueryDefinition
        );
        if (page.getTotal() > sceneQueryDefinition.maxExportRows()) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_EXPORT_ROW_LIMIT_EXCEEDED);
        }
        return page.getRecords();
    }

    public List<GlobalDictTypeEntity> listAllGlobalTypes(String keyword) {
        var query = Wrappers.<GlobalDictTypeEntity>lambdaQuery()
                .orderByAsc(GlobalDictTypeEntity::getId);
        if (StringUtils.hasText(keyword)) {
            query.and(wrapper -> wrapper
                    .like(GlobalDictTypeEntity::getDictTypeCode, keyword)
                    .or()
                    .like(GlobalDictTypeEntity::getDictTypeName, keyword));
        }
        return globalDictTypeMapper.selectList(query);
    }

    public void updateGlobalType(Long id, String dictTypeCode, String dictTypeName) {
        GlobalDictTypeEntity entity = getGlobalType(id);
        if (globalDictTypeCodeExists(dictTypeCode, id)) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_TYPE_CODE_DUPLICATED);
        }

        String oldTypeCode = entity.getDictTypeCode();
        entity.setDictTypeCode(dictTypeCode);
        entity.setDictTypeName(dictTypeName);
        try {
            globalDictTypeMapper.updateById(entity);
        } catch (DuplicateKeyException ignored) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_TYPE_CODE_DUPLICATED);
        }
        if (!oldTypeCode.equals(dictTypeCode)) {
            try {
                syncGlobalItemTypeCode(oldTypeCode, dictTypeCode);
            } catch (DuplicateKeyException ignored) {
                throw new BizException(DictErrorCode.GLOBAL_DICT_TYPE_CODE_CONFLICT_WITH_ITEMS);
            }
        }
    }

    public void deleteGlobalType(Long id) {
        GlobalDictTypeEntity entity = getGlobalType(id);
        Long itemCount = globalDictItemMapper.selectCount(
                Wrappers.<GlobalDictItemEntity>lambdaQuery()
                        .eq(GlobalDictItemEntity::getDictTypeCode, entity.getDictTypeCode())
        );
        if (itemCount != null && itemCount > 0L) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_TYPE_HAS_ITEMS);
        }
        globalDictTypeMapper.deleteById(entity.getId());
    }

    public void createGlobalItem(String dictTypeCode, String dictItemCode, String dictItemName, Integer sortOrder, String remark, EnableStatusEnum status) {
        if (!globalDictTypeCodeExists(dictTypeCode, null)) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_TYPE_NOT_FOUND);
        }
        if (globalDictItemCodeExists(dictTypeCode, dictItemCode, null)) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_ITEM_CODE_DUPLICATED);
        }

        GlobalDictItemEntity entity = new GlobalDictItemEntity();
        entity.setDictTypeCode(dictTypeCode);
        entity.setDictItemCode(dictItemCode);
        entity.setDictItemName(dictItemName);
        entity.setSortOrder(sortOrder != null ? sortOrder : 0);
        entity.setRemark(remark);
        entity.setStatus(status != null ? status : EnableStatusEnum.ENABLE);
        entity.setDeleted(0L);
        try {
            globalDictItemMapper.insert(entity);
        } catch (DuplicateKeyException ignored) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_ITEM_CODE_DUPLICATED);
        }
    }

    public void updateGlobalItem(Long id, String dictTypeCode, String dictItemCode, String dictItemName, Integer sortOrder, String remark, EnableStatusEnum status) {
        GlobalDictItemEntity entity = getGlobalItem(id);
        if (!globalDictTypeCodeExists(dictTypeCode, null)) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_TYPE_NOT_FOUND);
        }
        if (globalDictItemCodeExists(dictTypeCode, dictItemCode, id)) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_ITEM_CODE_DUPLICATED);
        }

        entity.setDictTypeCode(dictTypeCode);
        entity.setDictItemCode(dictItemCode);
        entity.setDictItemName(dictItemName);
        entity.setSortOrder(sortOrder != null ? sortOrder : entity.getSortOrder());
        entity.setRemark(remark);
        entity.setStatus(status != null ? status : entity.getStatus());
        try {
            globalDictItemMapper.updateById(entity);
        } catch (DuplicateKeyException ignored) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_ITEM_CODE_DUPLICATED);
        }
    }

    public void deleteGlobalItems(List<Long> ids) {
        List<GlobalDictItemEntity> entities = globalDictItemMapper.selectBatchIds(ids);
        if (entities.isEmpty()) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_ITEM_NOT_FOUND);
        }
        globalDictItemMapper.deleteByIds(ids);
    }

    public List<GlobalDictItemEntity> listGlobalItemsByType(String dictTypeCode) {
        Long typeCount = globalDictTypeMapper.selectCount(
                Wrappers.<GlobalDictTypeEntity>lambdaQuery()
                        .eq(GlobalDictTypeEntity::getDictTypeCode, dictTypeCode)
        );
        if (typeCount == null || typeCount == 0L) {
            return Collections.emptyList();
        }

        return globalDictItemMapper.selectList(
                Wrappers.<GlobalDictItemEntity>lambdaQuery()
                        .eq(GlobalDictItemEntity::getDictTypeCode, dictTypeCode)
                        .orderByAsc(GlobalDictItemEntity::getSortOrder)
                        .orderByAsc(GlobalDictItemEntity::getId)
        );
    }

    private boolean globalDictTypeCodeExists(String dictTypeCode, Long excludeId) {
        var query = Wrappers.<GlobalDictTypeEntity>lambdaQuery()
                .eq(GlobalDictTypeEntity::getDictTypeCode, dictTypeCode);
        if (excludeId != null) {
            query.ne(GlobalDictTypeEntity::getId, excludeId);
        }
        Long count = globalDictTypeMapper.selectCount(query);
        return count != null && count > 0L;
    }

    private boolean globalDictItemCodeExists(String dictTypeCode, String dictItemCode, Long excludeId) {
        var query = Wrappers.<GlobalDictItemEntity>lambdaQuery()
                .eq(GlobalDictItemEntity::getDictTypeCode, dictTypeCode)
                .eq(GlobalDictItemEntity::getDictItemCode, dictItemCode);
        if (excludeId != null) {
            query.ne(GlobalDictItemEntity::getId, excludeId);
        }
        Long count = globalDictItemMapper.selectCount(query);
        return count != null && count > 0L;
    }

    private GlobalDictTypeEntity getGlobalType(Long id) {
        GlobalDictTypeEntity entity = globalDictTypeMapper.selectOne(
                Wrappers.<GlobalDictTypeEntity>lambdaQuery()
                        .eq(GlobalDictTypeEntity::getId, id)
                        .last("limit 1")
        );
        if (entity == null) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_TYPE_NOT_FOUND);
        }
        return entity;
    }

    private void syncGlobalItemTypeCode(String oldTypeCode, String newTypeCode) {
        List<GlobalDictItemEntity> items = globalDictItemMapper.selectList(
                Wrappers.<GlobalDictItemEntity>lambdaQuery()
                        .eq(GlobalDictItemEntity::getDictTypeCode, oldTypeCode)
                        .orderByAsc(GlobalDictItemEntity::getId)
        );
        for (GlobalDictItemEntity item : items) {
            item.setDictTypeCode(newTypeCode);
            globalDictItemMapper.updateById(item);
        }
    }

    private GlobalDictItemEntity getGlobalItem(Long id) {
        GlobalDictItemEntity entity = globalDictItemMapper.selectOne(
                Wrappers.<GlobalDictItemEntity>lambdaQuery()
                        .eq(GlobalDictItemEntity::getId, id)
                        .last("limit 1")
        );
        if (entity == null) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_ITEM_NOT_FOUND);
        }
        return entity;
    }
}
