package com.demo.mdm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.core.exception.BizException;
import com.demo.core.tenant.TenantContext;
import com.demo.mdm.enums.DictErrorCode;
import com.demo.mdm.infra.entity.DictItemEntity;
import com.demo.mdm.infra.entity.DictTypeEntity;
import com.demo.mdm.infra.entity.GlobalDictItemEntity;
import com.demo.mdm.infra.entity.GlobalDictTypeEntity;
import com.demo.mdm.infra.mapper.DictItemMapper;
import com.demo.mdm.infra.mapper.DictTypeMapper;
import com.demo.mdm.infra.mapper.GlobalDictItemMapper;
import com.demo.mdm.infra.mapper.GlobalDictTypeMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class DictService {

    private final DictTypeMapper dictTypeMapper;
    private final DictItemMapper dictItemMapper;
    private final GlobalDictTypeMapper globalDictTypeMapper;
    private final GlobalDictItemMapper globalDictItemMapper;

    public DictService(
            DictTypeMapper dictTypeMapper,
            DictItemMapper dictItemMapper,
            GlobalDictTypeMapper globalDictTypeMapper,
            GlobalDictItemMapper globalDictItemMapper
    ) {
        this.dictTypeMapper = dictTypeMapper;
        this.dictItemMapper = dictItemMapper;
        this.globalDictTypeMapper = globalDictTypeMapper;
        this.globalDictItemMapper = globalDictItemMapper;
    }

    public List<DictItemEntity> listItemsByType(String dictTypeCode) {
        Long tenantId = TenantContext.requireTenantId();
        Long typeCount = dictTypeMapper.selectCount(
                Wrappers.<DictTypeEntity>lambdaQuery()
                        .eq(DictTypeEntity::getTenantId, tenantId)
                        .eq(DictTypeEntity::getDictTypeCode, dictTypeCode)
        );
        if (typeCount != null && typeCount > 0L) {
            return dictItemMapper.selectList(
                    Wrappers.<DictItemEntity>lambdaQuery()
                            .eq(DictItemEntity::getTenantId, tenantId)
                            .eq(DictItemEntity::getDictTypeCode, dictTypeCode)
                            .orderByAsc(DictItemEntity::getId)
            );
        }
        return toTenantDictItems(listGlobalItemsByType(dictTypeCode));
    }

    public Page<DictTypeEntity> listTypes(String keyword, long pageNo, long pageSize) {
        Long tenantId = TenantContext.requireTenantId();
        var query = Wrappers.<DictTypeEntity>lambdaQuery()
                .eq(DictTypeEntity::getTenantId, tenantId)
                .orderByAsc(DictTypeEntity::getId);
        if (StringUtils.hasText(keyword)) {
            query.and(wrapper -> wrapper
                    .like(DictTypeEntity::getDictTypeCode, keyword)
                    .or()
                    .like(DictTypeEntity::getDictTypeName, keyword));
        }
        Page<DictTypeEntity> page = new Page<>(pageNo, pageSize);
        return dictTypeMapper.selectPage(page, query);
    }

    public void createType(String dictTypeCode, String dictTypeName) {
        Long tenantId = TenantContext.requireTenantId();
        if (tenantDictTypeCodeExists(tenantId, dictTypeCode, null)) {
            throw new BizException(DictErrorCode.DICT_TYPE_CODE_DUPLICATED);
        }

        DictTypeEntity entity = new DictTypeEntity();
        entity.setTenantId(tenantId);
        entity.setDictTypeCode(dictTypeCode);
        entity.setDictTypeName(dictTypeName);
        entity.setDeleted(0L);
        try {
            dictTypeMapper.insert(entity);
        } catch (DuplicateKeyException exception) {
            throw new BizException(DictErrorCode.DICT_TYPE_CODE_DUPLICATED);
        }
    }

    public void updateType(Long id, String dictTypeCode, String dictTypeName) {
        Long tenantId = TenantContext.requireTenantId();
        DictTypeEntity entity = getTenantType(tenantId, id);
        if (tenantDictTypeCodeExists(tenantId, dictTypeCode, id)) {
            throw new BizException(DictErrorCode.DICT_TYPE_CODE_DUPLICATED);
        }

        String oldTypeCode = entity.getDictTypeCode();
        entity.setDictTypeCode(dictTypeCode);
        entity.setDictTypeName(dictTypeName);
        try {
            dictTypeMapper.updateById(entity);
        } catch (DuplicateKeyException exception) {
            throw new BizException(DictErrorCode.DICT_TYPE_CODE_DUPLICATED);
        }
        if (!oldTypeCode.equals(dictTypeCode)) {
            try {
                syncTenantItemTypeCode(tenantId, oldTypeCode, dictTypeCode);
            } catch (DuplicateKeyException exception) {
                throw new BizException(DictErrorCode.DICT_TYPE_CODE_CONFLICT_WITH_ITEMS);
            }
        }
    }

    public void deleteType(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        DictTypeEntity entity = getTenantType(tenantId, id);
        Long itemCount = dictItemMapper.selectCount(
                Wrappers.<DictItemEntity>lambdaQuery()
                        .eq(DictItemEntity::getTenantId, tenantId)
                        .eq(DictItemEntity::getDictTypeCode, entity.getDictTypeCode())
        );
        if (itemCount != null && itemCount > 0L) {
            throw new BizException(DictErrorCode.DICT_TYPE_HAS_ITEMS);
        }
        entity.setDeleted(1L);
        dictTypeMapper.updateById(entity);
    }

    public void createItem(String dictTypeCode, String dictItemCode, String dictItemName) {
        Long tenantId = TenantContext.requireTenantId();
        ensureTenantTypeExists(tenantId, dictTypeCode);
        if (tenantDictItemCodeExists(tenantId, dictTypeCode, dictItemCode, null)) {
            throw new BizException(DictErrorCode.DICT_ITEM_CODE_DUPLICATED);
        }

        DictItemEntity entity = new DictItemEntity();
        entity.setTenantId(tenantId);
        entity.setDictTypeCode(dictTypeCode);
        entity.setDictItemCode(dictItemCode);
        entity.setDictItemName(dictItemName);
        entity.setDeleted(0L);
        try {
            dictItemMapper.insert(entity);
        } catch (DuplicateKeyException exception) {
            throw new BizException(DictErrorCode.DICT_ITEM_CODE_DUPLICATED);
        }
    }

    public void updateItem(Long id, String dictTypeCode, String dictItemCode, String dictItemName) {
        Long tenantId = TenantContext.requireTenantId();
        DictItemEntity entity = getTenantItem(tenantId, id);
        ensureTenantTypeExists(tenantId, dictTypeCode);
        if (tenantDictItemCodeExists(tenantId, dictTypeCode, dictItemCode, id)) {
            throw new BizException(DictErrorCode.DICT_ITEM_CODE_DUPLICATED);
        }

        entity.setDictTypeCode(dictTypeCode);
        entity.setDictItemCode(dictItemCode);
        entity.setDictItemName(dictItemName);
        try {
            dictItemMapper.updateById(entity);
        } catch (DuplicateKeyException exception) {
            throw new BizException(DictErrorCode.DICT_ITEM_CODE_DUPLICATED);
        }
    }

    public void deleteItem(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        DictItemEntity entity = getTenantItem(tenantId, id);
        entity.setDeleted(1L);
        dictItemMapper.updateById(entity);
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

    private boolean globalDictTypeExists(String dictTypeCode) {
        return globalDictTypeCodeExists(dictTypeCode, null);
    }

    private void ensureTenantTypeExists(Long tenantId, String dictTypeCode) {
        Long typeCount = dictTypeMapper.selectCount(
                Wrappers.<DictTypeEntity>lambdaQuery()
                        .eq(DictTypeEntity::getTenantId, tenantId)
                        .eq(DictTypeEntity::getDictTypeCode, dictTypeCode)
        );
        if (typeCount == null || typeCount == 0L) {
            throw new BizException(DictErrorCode.DICT_TYPE_NOT_FOUND);
        }
    }

    private boolean tenantDictTypeCodeExists(Long tenantId, String dictTypeCode, Long excludeId) {
        var query = Wrappers.<DictTypeEntity>lambdaQuery()
                .eq(DictTypeEntity::getTenantId, tenantId)
                .eq(DictTypeEntity::getDictTypeCode, dictTypeCode);
        if (excludeId != null) {
            query.ne(DictTypeEntity::getId, excludeId);
        }
        Long count = dictTypeMapper.selectCount(query);
        return count != null && count > 0L;
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

    private boolean tenantDictItemCodeExists(Long tenantId, String dictTypeCode, String dictItemCode, Long excludeId) {
        var query = Wrappers.<DictItemEntity>lambdaQuery()
                .eq(DictItemEntity::getTenantId, tenantId)
                .eq(DictItemEntity::getDictTypeCode, dictTypeCode)
                .eq(DictItemEntity::getDictItemCode, dictItemCode);
        if (excludeId != null) {
            query.ne(DictItemEntity::getId, excludeId);
        }
        Long count = dictItemMapper.selectCount(query);
        return count != null && count > 0L;
    }

    private DictTypeEntity getTenantType(Long tenantId, Long id) {
        DictTypeEntity entity = dictTypeMapper.selectOne(
                Wrappers.<DictTypeEntity>lambdaQuery()
                        .eq(DictTypeEntity::getTenantId, tenantId)
                        .eq(DictTypeEntity::getId, id)
                        .last("limit 1")
        );
        if (entity == null) {
            throw new BizException(DictErrorCode.DICT_TYPE_NOT_FOUND);
        }
        return entity;
    }

    private DictItemEntity getTenantItem(Long tenantId, Long id) {
        DictItemEntity entity = dictItemMapper.selectOne(
                Wrappers.<DictItemEntity>lambdaQuery()
                        .eq(DictItemEntity::getTenantId, tenantId)
                        .eq(DictItemEntity::getId, id)
                        .last("limit 1")
        );
        if (entity == null) {
            throw new BizException(DictErrorCode.DICT_ITEM_NOT_FOUND);
        }
        return entity;
    }

    private void syncTenantItemTypeCode(Long tenantId, String oldTypeCode, String newTypeCode) {
        List<DictItemEntity> items = dictItemMapper.selectList(
                Wrappers.<DictItemEntity>lambdaQuery()
                        .eq(DictItemEntity::getTenantId, tenantId)
                        .eq(DictItemEntity::getDictTypeCode, oldTypeCode)
                        .orderByAsc(DictItemEntity::getId)
        );
        for (DictItemEntity item : items) {
            item.setDictTypeCode(newTypeCode);
            dictItemMapper.updateById(item);
        }
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

    private List<DictItemEntity> toTenantDictItems(List<GlobalDictItemEntity> globalItems) {
        if (globalItems.isEmpty()) {
            return Collections.emptyList();
        }

        List<DictItemEntity> items = new ArrayList<>(globalItems.size());
        for (GlobalDictItemEntity globalItem : globalItems) {
            DictItemEntity item = new DictItemEntity();
            item.setId(globalItem.getId());
            item.setDictTypeCode(globalItem.getDictTypeCode());
            item.setDictItemCode(globalItem.getDictItemCode());
            item.setDictItemName(globalItem.getDictItemName());
            items.add(item);
        }
        return items;
    }
}
