package com.demo.mdm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.demo.core.tenant.TenantContext;
import com.demo.mdm.infra.entity.DictItemEntity;
import com.demo.mdm.infra.entity.DictTypeEntity;
import com.demo.mdm.infra.mapper.DictItemMapper;
import com.demo.mdm.infra.mapper.DictTypeMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class DictService {

    private final DictTypeMapper dictTypeMapper;
    private final DictItemMapper dictItemMapper;

    public DictService(DictTypeMapper dictTypeMapper, DictItemMapper dictItemMapper) {
        this.dictTypeMapper = dictTypeMapper;
        this.dictItemMapper = dictItemMapper;
    }

    public List<DictItemEntity> listItemsByType(Long tenantId, String dictTypeCode) {
        TenantContext.setTenantId(tenantId);
        try {
            Long typeCount = dictTypeMapper.selectCount(
                    Wrappers.<DictTypeEntity>lambdaQuery()
                            .eq(DictTypeEntity::getTenantId, tenantId)
                            .eq(DictTypeEntity::getDictTypeCode, dictTypeCode)
            );
            if (typeCount == null || typeCount == 0L) {
                return Collections.emptyList();
            }

            return dictItemMapper.selectList(
                    Wrappers.<DictItemEntity>lambdaQuery()
                            .eq(DictItemEntity::getTenantId, tenantId)
                            .eq(DictItemEntity::getDictTypeCode, dictTypeCode)
                            .orderByAsc(DictItemEntity::getId)
            );
        } finally {
            TenantContext.clear();
        }
    }
}
