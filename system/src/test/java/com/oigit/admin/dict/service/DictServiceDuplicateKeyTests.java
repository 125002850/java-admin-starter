package com.oigit.admin.dict.service;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.query.executor.MybatisPlusQueryExecutor;
import com.oigit.admin.dict.enums.DictErrorCode;
import com.oigit.admin.dict.infra.entity.GlobalDictItemEntity;
import com.oigit.admin.dict.infra.entity.GlobalDictTypeEntity;
import com.oigit.admin.dict.infra.mapper.GlobalDictItemMapper;
import com.oigit.admin.dict.infra.mapper.GlobalDictTypeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DictServiceDuplicateKeyTests {

    @Mock
    private GlobalDictTypeMapper globalDictTypeMapper;
    @Mock
    private GlobalDictItemMapper globalDictItemMapper;

    private DictService dictService;
    private final MybatisPlusQueryExecutor mybatisPlusQueryExecutor = new MybatisPlusQueryExecutor();

    @BeforeEach
    void setUp() {
        dictService = new DictService(globalDictTypeMapper, globalDictItemMapper, mybatisPlusQueryExecutor);
    }

    @Test
    void updateGlobalType_should_translate_duplicate_key_from_type_update() {
        when(globalDictTypeMapper.selectOne(any())).thenReturn(globalType(1L, "gender"));
        when(globalDictTypeMapper.selectCount(any())).thenReturn(0L);
        doThrow(new DuplicateKeyException("uk_sys_dict_type_global_code"))
                .when(globalDictTypeMapper).updateById(any(GlobalDictTypeEntity.class));

        assertThatThrownBy(() -> dictService.updateGlobalType(1L, "sex", "性别"))
                .isInstanceOf(BizException.class)
                .hasMessage(DictErrorCode.GLOBAL_DICT_TYPE_CODE_DUPLICATED.getMsg());
    }

    @Test
    void updateGlobalType_should_translate_duplicate_key_from_item_sync() {
        when(globalDictTypeMapper.selectOne(any())).thenReturn(globalType(1L, "gender"));
        when(globalDictTypeMapper.selectCount(any())).thenReturn(0L);
        when(globalDictItemMapper.selectList(any())).thenReturn(java.util.List.of(globalItem(21L, "gender", "MALE")));
        doThrow(new DuplicateKeyException("uk_sys_dict_item_global_type_code"))
                .when(globalDictItemMapper).updateById(any(GlobalDictItemEntity.class));

        assertThatThrownBy(() -> dictService.updateGlobalType(1L, "sex", "性别"))
                .isInstanceOf(BizException.class)
                .hasMessage(DictErrorCode.GLOBAL_DICT_TYPE_CODE_CONFLICT_WITH_ITEMS.getMsg());
    }

    @Test
    void updateGlobalItem_should_translate_duplicate_key_to_biz_exception() {
        when(globalDictItemMapper.selectOne(any())).thenReturn(globalItem(1L, "gender", "MALE"));
        when(globalDictTypeMapper.selectCount(any())).thenReturn(1L);
        when(globalDictItemMapper.selectCount(any())).thenReturn(0L);
        doThrow(new DuplicateKeyException("uk_sys_dict_item_global_type_code"))
                .when(globalDictItemMapper).updateById(any(GlobalDictItemEntity.class));

        assertThatThrownBy(() -> dictService.updateGlobalItem(1L, "gender", "FEMALE", "女", null, null, null))
                .isInstanceOf(BizException.class)
                .hasMessage(DictErrorCode.GLOBAL_DICT_ITEM_CODE_DUPLICATED.getMsg());
    }

    private GlobalDictTypeEntity globalType(Long id, String dictTypeCode) {
        GlobalDictTypeEntity entity = new GlobalDictTypeEntity();
        entity.setId(id);
        entity.setDictTypeCode(dictTypeCode);
        entity.setDictTypeName(dictTypeCode);
        entity.setDeleted(0L);
        return entity;
    }

    private GlobalDictItemEntity globalItem(Long id, String dictTypeCode, String dictItemCode) {
        GlobalDictItemEntity entity = new GlobalDictItemEntity();
        entity.setId(id);
        entity.setDictTypeCode(dictTypeCode);
        entity.setDictItemCode(dictItemCode);
        entity.setDictItemName(dictItemCode);
        entity.setDeleted(0L);
        return entity;
    }
}
