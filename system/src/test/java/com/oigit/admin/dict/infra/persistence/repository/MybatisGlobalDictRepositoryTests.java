package com.oigit.admin.dict.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.dict.domain.model.GlobalDictItem;
import com.oigit.admin.dict.domain.model.GlobalDictType;
import com.oigit.admin.dict.enums.DictErrorCode;
import com.oigit.admin.dict.infra.persistence.entity.GlobalDictItemEntity;
import com.oigit.admin.dict.infra.persistence.service.GlobalDictItemPersistenceService;
import com.oigit.admin.dict.infra.persistence.service.GlobalDictTypePersistenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MybatisGlobalDictRepositoryTests {

    @Mock
    private GlobalDictTypePersistenceService globalDictTypePersistenceService;
    @Mock
    private GlobalDictItemPersistenceService globalDictItemPersistenceService;

    @Test
    void typeRepository_should_translate_duplicate_key_from_update() {
        MybatisGlobalDictTypeRepository repository =
                new MybatisGlobalDictTypeRepository(globalDictTypePersistenceService);
        when(globalDictTypePersistenceService.updateById(any()))
                .thenThrow(new DuplicateKeyException("uk_sys_dict_type_global_code"));

        GlobalDictType dictType = new GlobalDictType();
        dictType.setId(1L);
        dictType.setDictTypeCode("sex");
        dictType.setDictTypeName("性别");

        assertThatThrownBy(() -> repository.update(dictType))
                .isInstanceOf(BizException.class)
                .hasMessage(DictErrorCode.GLOBAL_DICT_TYPE_CODE_DUPLICATED.getMsg());
    }

    @Test
    void itemRepository_should_translate_duplicate_key_from_update() {
        MybatisGlobalDictItemRepository repository =
                new MybatisGlobalDictItemRepository(globalDictItemPersistenceService);
        when(globalDictItemPersistenceService.updateById(any()))
                .thenThrow(new DuplicateKeyException("uk_sys_dict_item_global_type_code"));

        GlobalDictItem dictItem = new GlobalDictItem();
        dictItem.setId(1L);
        dictItem.setDictTypeCode("gender");
        dictItem.setDictItemCode("FEMALE");
        dictItem.setDictItemName("女");

        assertThatThrownBy(() -> repository.update(dictItem))
                .isInstanceOf(BizException.class)
                .hasMessage(DictErrorCode.GLOBAL_DICT_ITEM_CODE_DUPLICATED.getMsg());
    }

    @Test
    void itemRepository_should_translate_duplicate_key_when_type_code_changes() {
        MybatisGlobalDictItemRepository repository =
                new MybatisGlobalDictItemRepository(globalDictItemPersistenceService);
        when(globalDictItemPersistenceService.list(anyItemWrapper()))
                .thenReturn(List.of(item(21L, "gender", "MALE", "男")));
        when(globalDictItemPersistenceService.updateById(any()))
                .thenThrow(new DuplicateKeyException("uk_sys_dict_item_global_type_code"));

        assertThatThrownBy(() -> repository.changeTypeCode("gender", "sex"))
                .isInstanceOf(BizException.class)
                .hasMessage(DictErrorCode.GLOBAL_DICT_TYPE_CODE_CONFLICT_WITH_ITEMS.getMsg());
    }

    @Test
    void itemRepository_should_group_names_by_normalized_dict_type() {
        MybatisGlobalDictItemRepository repository =
                new MybatisGlobalDictItemRepository(globalDictItemPersistenceService);
        when(globalDictItemPersistenceService.list(anyItemWrapper())).thenReturn(List.of(
                item(1L, "YES_NO", "1", "是"),
                item(2L, "YES_NO", "0", "否"),
                item(3L, "PLATFORM_CATEGORY", "5", "平台分类")
        ));

        Map<String, Map<String, String>> result = repository.findNamesByTypeCodes(
                Arrays.asList(" YES_NO ", "PLATFORM_CATEGORY", "YES_NO", "", null)
        );

        assertThat(result)
                .containsEntry("YES_NO", Map.of("1", "是", "0", "否"))
                .containsEntry("PLATFORM_CATEGORY", Map.of("5", "平台分类"));
    }

    @Test
    void itemRepository_should_skip_query_when_types_are_empty() {
        MybatisGlobalDictItemRepository repository =
                new MybatisGlobalDictItemRepository(globalDictItemPersistenceService);

        Map<String, Map<String, String>> result = repository.findNamesByTypeCodes(
                Arrays.asList("", null, " ")
        );

        assertThat(result).isEmpty();
        verify(globalDictItemPersistenceService, never()).list(anyItemWrapper());
    }

    private GlobalDictItemEntity item(Long id, String dictTypeCode, String dictItemCode, String dictItemName) {
        GlobalDictItemEntity entity = new GlobalDictItemEntity();
        entity.setId(id);
        entity.setDictTypeCode(dictTypeCode);
        entity.setDictItemCode(dictItemCode);
        entity.setDictItemName(dictItemName);
        return entity;
    }

    private Wrapper<GlobalDictItemEntity> anyItemWrapper() {
        return org.mockito.ArgumentMatchers.any();
    }
}
