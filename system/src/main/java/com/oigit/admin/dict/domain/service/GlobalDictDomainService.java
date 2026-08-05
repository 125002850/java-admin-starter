package com.oigit.admin.dict.domain.service;

import com.oigit.admin.core.enums.EnableStatusEnum;
import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.dict.domain.model.GlobalDictItem;
import com.oigit.admin.dict.domain.model.GlobalDictType;
import com.oigit.admin.dict.domain.repository.GlobalDictItemRepository;
import com.oigit.admin.dict.domain.repository.GlobalDictTypeRepository;
import com.oigit.admin.dict.enums.DictErrorCode;
import java.util.List;

public class GlobalDictDomainService {

    private final GlobalDictTypeRepository globalDictTypeRepository;
    private final GlobalDictItemRepository globalDictItemRepository;
    private final EnumDictionaryPolicy enumDictionaryPolicy;

    public GlobalDictDomainService(
            GlobalDictTypeRepository globalDictTypeRepository,
            GlobalDictItemRepository globalDictItemRepository
    ) {
        this(globalDictTypeRepository, globalDictItemRepository, EnumDictionaryPolicy.none());
    }

    public GlobalDictDomainService(
            GlobalDictTypeRepository globalDictTypeRepository,
            GlobalDictItemRepository globalDictItemRepository,
            EnumDictionaryPolicy enumDictionaryPolicy
    ) {
        this.globalDictTypeRepository = globalDictTypeRepository;
        this.globalDictItemRepository = globalDictItemRepository;
        this.enumDictionaryPolicy = enumDictionaryPolicy;
    }

    public void createGlobalType(String dictTypeCode, String dictTypeName) {
        if (globalDictTypeRepository.existsByCode(dictTypeCode, null)) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_TYPE_CODE_DUPLICATED);
        }
        GlobalDictType dictType = new GlobalDictType();
        dictType.setDictTypeCode(dictTypeCode);
        dictType.setDictTypeName(dictTypeName);
        globalDictTypeRepository.create(dictType);
    }

    public void updateGlobalType(Long id, String dictTypeCode, String dictTypeName) {
        GlobalDictType dictType = getGlobalType(id);
        if (enumDictionaryPolicy.isEnumDictionary(dictType.getDictTypeCode())
                && !dictType.getDictTypeCode().equals(dictTypeCode)) {
            throw new BizException(DictErrorCode.ENUM_DICT_CONTRACT_PROTECTED);
        }
        if (globalDictTypeRepository.existsByCode(dictTypeCode, id)) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_TYPE_CODE_DUPLICATED);
        }

        String oldTypeCode = dictType.getDictTypeCode();
        dictType.setDictTypeCode(dictTypeCode);
        dictType.setDictTypeName(dictTypeName);
        globalDictTypeRepository.update(dictType);
        if (!oldTypeCode.equals(dictTypeCode)) {
            globalDictItemRepository.changeTypeCode(oldTypeCode, dictTypeCode);
        }
    }

    public void deleteGlobalType(Long id) {
        GlobalDictType dictType = getGlobalType(id);
        rejectEnumDictionaryMutation(dictType.getDictTypeCode());
        if (globalDictItemRepository.countByTypeCode(dictType.getDictTypeCode()) > 0L) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_TYPE_HAS_ITEMS);
        }
        globalDictTypeRepository.deleteById(dictType.getId());
    }

    public void createGlobalItem(
            String dictTypeCode,
            String dictItemCode,
            String dictItemName,
            Integer sortOrder,
            String remark,
            EnableStatusEnum status
    ) {
        requireGlobalType(dictTypeCode);
        rejectEnumDictionaryMutation(dictTypeCode);
        if (globalDictItemRepository.existsByTypeAndCode(dictTypeCode, dictItemCode, null)) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_ITEM_CODE_DUPLICATED);
        }

        GlobalDictItem dictItem = new GlobalDictItem();
        dictItem.setDictTypeCode(dictTypeCode);
        dictItem.setDictItemCode(dictItemCode);
        dictItem.setDictItemName(dictItemName);
        dictItem.setSortOrder(sortOrder != null ? sortOrder : 0);
        dictItem.setRemark(remark);
        dictItem.setStatus(status != null ? status : EnableStatusEnum.ENABLE);
        globalDictItemRepository.create(dictItem);
    }

    public void updateGlobalItem(
            Long id,
            String dictTypeCode,
            String dictItemCode,
            String dictItemName,
            Integer sortOrder,
            String remark,
            EnableStatusEnum status
    ) {
        GlobalDictItem dictItem = getGlobalItem(id);
        requireGlobalType(dictTypeCode);
        if ((enumDictionaryPolicy.isEnumDictionary(dictItem.getDictTypeCode())
                || enumDictionaryPolicy.isEnumDictionary(dictTypeCode))
                && (!dictItem.getDictTypeCode().equals(dictTypeCode)
                || !dictItem.getDictItemCode().equals(dictItemCode))) {
            throw new BizException(DictErrorCode.ENUM_DICT_CONTRACT_PROTECTED);
        }
        if (globalDictItemRepository.existsByTypeAndCode(dictTypeCode, dictItemCode, id)) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_ITEM_CODE_DUPLICATED);
        }

        dictItem.setDictTypeCode(dictTypeCode);
        dictItem.setDictItemCode(dictItemCode);
        dictItem.setDictItemName(dictItemName);
        dictItem.setSortOrder(sortOrder != null ? sortOrder : dictItem.getSortOrder());
        dictItem.setRemark(remark);
        dictItem.setStatus(status != null ? status : dictItem.getStatus());
        globalDictItemRepository.update(dictItem);
    }

    public void deleteGlobalItems(List<Long> ids) {
        List<GlobalDictItem> items = globalDictItemRepository.findByIds(ids);
        long expectedSize = ids.stream().distinct().count();
        if (items.size() != expectedSize) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_ITEM_NOT_FOUND);
        }
        if (items.stream().anyMatch(item -> enumDictionaryPolicy.isEnumDictionary(item.getDictTypeCode()))) {
            throw new BizException(DictErrorCode.ENUM_DICT_CONTRACT_PROTECTED);
        }
        globalDictItemRepository.deleteByIds(ids);
    }

    private GlobalDictType getGlobalType(Long id) {
        return globalDictTypeRepository.findById(id)
                .orElseThrow(() -> new BizException(DictErrorCode.GLOBAL_DICT_TYPE_NOT_FOUND));
    }

    private GlobalDictItem getGlobalItem(Long id) {
        return globalDictItemRepository.findById(id)
                .orElseThrow(() -> new BizException(DictErrorCode.GLOBAL_DICT_ITEM_NOT_FOUND));
    }

    private void requireGlobalType(String dictTypeCode) {
        if (!globalDictTypeRepository.existsByCode(dictTypeCode, null)) {
            throw new BizException(DictErrorCode.GLOBAL_DICT_TYPE_NOT_FOUND);
        }
    }

    private void rejectEnumDictionaryMutation(String dictTypeCode) {
        if (enumDictionaryPolicy.isEnumDictionary(dictTypeCode)) {
            throw new BizException(DictErrorCode.ENUM_DICT_CONTRACT_PROTECTED);
        }
    }
}
