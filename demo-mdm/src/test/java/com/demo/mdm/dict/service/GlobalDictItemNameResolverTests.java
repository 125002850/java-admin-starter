package com.demo.mdm.dict.service;

import com.demo.mdm.dict.infra.entity.GlobalDictItemEntity;
import com.demo.mdm.dict.infra.mapper.GlobalDictItemMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalDictItemNameResolverTests {

    @Mock
    private GlobalDictItemMapper globalDictItemMapper;

    @Test
    void resolveItemNames_should_group_items_by_dict_type() {
        GlobalDictItemNameResolver resolver = new GlobalDictItemNameResolver(globalDictItemMapper);
        when(globalDictItemMapper.selectList(any())).thenReturn(List.of(
                item(1L, "YES_NO", "1", "是"),
                item(2L, "YES_NO", "0", "否"),
                item(3L, "PLATFORM_CATEGORY", "5", "平台分类")
        ));

        Map<String, Map<String, String>> result = resolver.resolveItemNames(
                Arrays.asList(" YES_NO ", "PLATFORM_CATEGORY", "YES_NO", "", null)
        );

        assertThat(result)
                .containsEntry("YES_NO", Map.of("1", "是", "0", "否"))
                .containsEntry("PLATFORM_CATEGORY", Map.of("5", "平台分类"));
    }

    @Test
    void resolveItemNames_should_skip_query_when_types_are_empty() {
        GlobalDictItemNameResolver resolver = new GlobalDictItemNameResolver(globalDictItemMapper);

        Map<String, Map<String, String>> result = resolver.resolveItemNames(Arrays.asList("", null, " "));

        assertThat(result).isEmpty();
        verify(globalDictItemMapper, never()).selectList(any());
    }

    private GlobalDictItemEntity item(Long id, String dictTypeCode, String dictItemCode, String dictItemName) {
        GlobalDictItemEntity entity = new GlobalDictItemEntity();
        entity.setId(id);
        entity.setDictTypeCode(dictTypeCode);
        entity.setDictItemCode(dictItemCode);
        entity.setDictItemName(dictItemName);
        return entity;
    }
}
