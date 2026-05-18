package com.demo.mdm.service;

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
import org.junit.jupiter.api.AfterEach;
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
    private DictTypeMapper dictTypeMapper;
    @Mock
    private DictItemMapper dictItemMapper;
    @Mock
    private GlobalDictTypeMapper globalDictTypeMapper;
    @Mock
    private GlobalDictItemMapper globalDictItemMapper;

    private DictService dictService;

    @BeforeEach
    void setUp() {
        dictService = new DictService(dictTypeMapper, dictItemMapper, globalDictTypeMapper, globalDictItemMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void updateType_should_translate_duplicate_key_from_type_update() {
        TenantContext.setTenantId(100L);
        when(dictTypeMapper.selectOne(any())).thenReturn(tenantType(1L, 100L, "user_status"));
        when(dictTypeMapper.selectCount(any())).thenReturn(0L);
        doThrow(new DuplicateKeyException("uk_mdm_dict_type_tenant_code"))
                .when(dictTypeMapper).updateById(any(DictTypeEntity.class));

        assertThatThrownBy(() -> dictService.updateType(1L, "gender", "性别"))
                .isInstanceOf(BizException.class)
                .hasMessage(DictErrorCode.DICT_TYPE_CODE_DUPLICATED.getMsg());
    }

    @Test
    void updateType_should_translate_duplicate_key_from_item_sync() {
        TenantContext.setTenantId(100L);
        when(dictTypeMapper.selectOne(any())).thenReturn(tenantType(1L, 100L, "user_status"));
        when(dictTypeMapper.selectCount(any())).thenReturn(0L);
        when(dictItemMapper.selectList(any())).thenReturn(java.util.List.of(tenantItem(11L, 100L, "user_status", "ENABLED")));
        doThrow(new DuplicateKeyException("uk_mdm_dict_item_tenant_type_code"))
                .when(dictItemMapper).updateById(any(DictItemEntity.class));

        assertThatThrownBy(() -> dictService.updateType(1L, "gender", "性别"))
                .isInstanceOf(BizException.class)
                .hasMessage(DictErrorCode.DICT_TYPE_CODE_CONFLICT_WITH_ITEMS.getMsg());
    }

    @Test
    void updateItem_should_translate_duplicate_key_to_biz_exception() {
        TenantContext.setTenantId(100L);
        when(dictItemMapper.selectOne(any())).thenReturn(tenantItem(1L, 100L, "user_status", "ENABLED"));
        when(dictTypeMapper.selectCount(any())).thenReturn(1L);
        when(dictItemMapper.selectCount(any())).thenReturn(0L);
        doThrow(new DuplicateKeyException("uk_mdm_dict_item_tenant_type_code"))
                .when(dictItemMapper).updateById(any(DictItemEntity.class));

        assertThatThrownBy(() -> dictService.updateItem(1L, "gender", "MALE", "男"))
                .isInstanceOf(BizException.class)
                .hasMessage(DictErrorCode.DICT_ITEM_CODE_DUPLICATED.getMsg());
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

        assertThatThrownBy(() -> dictService.updateGlobalItem(1L, "gender", "FEMALE", "女"))
                .isInstanceOf(BizException.class)
                .hasMessage(DictErrorCode.GLOBAL_DICT_ITEM_CODE_DUPLICATED.getMsg());
    }

    private DictTypeEntity tenantType(Long id, Long tenantId, String dictTypeCode) {
        DictTypeEntity entity = new DictTypeEntity();
        entity.setId(id);
        entity.setTenantId(tenantId);
        entity.setDictTypeCode(dictTypeCode);
        entity.setDictTypeName(dictTypeCode);
        entity.setDeleted(0L);
        return entity;
    }

    private DictItemEntity tenantItem(Long id, Long tenantId, String dictTypeCode, String dictItemCode) {
        DictItemEntity entity = new DictItemEntity();
        entity.setId(id);
        entity.setTenantId(tenantId);
        entity.setDictTypeCode(dictTypeCode);
        entity.setDictItemCode(dictItemCode);
        entity.setDictItemName(dictItemCode);
        entity.setDeleted(0L);
        return entity;
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
