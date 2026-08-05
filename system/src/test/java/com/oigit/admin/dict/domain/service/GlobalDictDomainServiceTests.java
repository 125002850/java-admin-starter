package com.oigit.admin.dict.domain.service;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.dict.domain.model.GlobalDictType;
import com.oigit.admin.dict.domain.repository.GlobalDictItemRepository;
import com.oigit.admin.dict.domain.repository.GlobalDictTypeRepository;
import com.oigit.admin.dict.enums.DictErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalDictDomainServiceTests {

    @Mock
    private GlobalDictTypeRepository globalDictTypeRepository;
    @Mock
    private GlobalDictItemRepository globalDictItemRepository;
    @Mock
    private EnumDictionaryPolicy enumDictionaryPolicy;

    private GlobalDictDomainService domainService;

    @BeforeEach
    void setUp() {
        domainService = new GlobalDictDomainService(
                globalDictTypeRepository,
                globalDictItemRepository,
                enumDictionaryPolicy
        );
    }

    @Test
    void createGlobalType_should_reject_duplicated_code() {
        when(globalDictTypeRepository.existsByCode("gender", null)).thenReturn(true);

        assertThatThrownBy(() -> domainService.createGlobalType("gender", "性别"))
                .isInstanceOf(BizException.class)
                .hasMessage(DictErrorCode.GLOBAL_DICT_TYPE_CODE_DUPLICATED.getMsg());
    }

    @Test
    void updateGlobalType_should_update_type_and_sync_item_type_code() {
        GlobalDictType dictType = new GlobalDictType();
        dictType.setId(1L);
        dictType.setDictTypeCode("gender");
        dictType.setDictTypeName("性别");
        when(globalDictTypeRepository.findById(1L)).thenReturn(Optional.of(dictType));

        domainService.updateGlobalType(1L, "sex", "性别枚举");

        verify(globalDictTypeRepository).update(dictType);
        verify(globalDictItemRepository).changeTypeCode("gender", "sex");
    }

    @Test
    void deleteGlobalType_should_reject_type_with_items() {
        GlobalDictType dictType = new GlobalDictType();
        dictType.setId(1L);
        dictType.setDictTypeCode("gender");
        when(globalDictTypeRepository.findById(1L)).thenReturn(Optional.of(dictType));
        when(globalDictItemRepository.countByTypeCode("gender")).thenReturn(1L);

        assertThatThrownBy(() -> domainService.deleteGlobalType(1L))
                .isInstanceOf(BizException.class)
                .hasMessage(DictErrorCode.GLOBAL_DICT_TYPE_HAS_ITEMS.getMsg());
    }

    @Test
    void updateGlobalItem_should_reject_missing_item() {
        when(globalDictItemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> domainService.updateGlobalItem(
                99L, "gender", "MALE", "男", null, null, null
        ))
                .isInstanceOf(BizException.class)
                .hasMessage(DictErrorCode.GLOBAL_DICT_ITEM_NOT_FOUND.getMsg());
    }

    @Test
    void createGlobalItem_should_reject_enum_owned_dictionary_code() {
        when(globalDictTypeRepository.existsByCode("ENABLE_STATUS", null)).thenReturn(true);
        when(enumDictionaryPolicy.isEnumDictionary("ENABLE_STATUS")).thenReturn(true);

        assertThatThrownBy(() -> domainService.createGlobalItem(
                "ENABLE_STATUS", "archived", "已归档", 3, null, null
        ))
                .isInstanceOf(BizException.class)
                .hasMessage(DictErrorCode.ENUM_DICT_CONTRACT_PROTECTED.getMsg());
    }
}
