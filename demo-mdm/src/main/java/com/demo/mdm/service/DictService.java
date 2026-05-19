package com.demo.mdm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.core.exception.BizException;
import com.demo.mdm.enums.DictErrorCode;
import com.demo.mdm.infra.entity.GlobalDictItemEntity;
import com.demo.mdm.infra.entity.GlobalDictTypeEntity;
import com.demo.mdm.infra.mapper.GlobalDictItemMapper;
import com.demo.mdm.infra.mapper.GlobalDictTypeMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
public class DictService {

    private final GlobalDictTypeMapper globalDictTypeMapper;
    private final GlobalDictItemMapper globalDictItemMapper;

    public DictService(
            GlobalDictTypeMapper globalDictTypeMapper,
            GlobalDictItemMapper globalDictItemMapper
    ) {
        this.globalDictTypeMapper = globalDictTypeMapper;
        this.globalDictItemMapper = globalDictItemMapper;
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
        } catch (DuplicateKeyException exception) {
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
        } catch (DuplicateKeyException exception) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_TYPE_CODE_DUPLICATED);
        }
        if (!oldTypeCode.equals(dictTypeCode)) {
            try {
                syncGlobalItemTypeCode(oldTypeCode, dictTypeCode);
            } catch (DuplicateKeyException exception) {
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
        entity.setDeleted(1L);
        globalDictTypeMapper.updateById(entity);
    }

    public void createGlobalItem(String dictTypeCode, String dictItemCode, String dictItemName) {
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
        entity.setDeleted(0L);
        try {
            globalDictItemMapper.insert(entity);
        } catch (DuplicateKeyException exception) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_ITEM_CODE_DUPLICATED);
        }
    }

    public void updateGlobalItem(Long id, String dictTypeCode, String dictItemCode, String dictItemName) {
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
        try {
            globalDictItemMapper.updateById(entity);
        } catch (DuplicateKeyException exception) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_ITEM_CODE_DUPLICATED);
        }
    }

    public void deleteGlobalItem(Long id) {
        GlobalDictItemEntity entity = getGlobalItem(id);
        entity.setDeleted(1L);
        globalDictItemMapper.updateById(entity);
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
