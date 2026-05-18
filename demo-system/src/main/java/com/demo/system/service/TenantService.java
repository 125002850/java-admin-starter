package com.demo.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.demo.core.exception.BizException;
import com.demo.system.enums.TenantErrorCode;
import com.demo.system.infra.entity.SysTenantGlobalEntity;
import com.demo.system.infra.mapper.SysTenantGlobalMapper;
import com.demo.system.infra.mapper.SysUserMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class TenantService {

    private final SysTenantGlobalMapper sysTenantGlobalMapper;
    private final SysUserMapper sysUserMapper;

    public TenantService(SysTenantGlobalMapper sysTenantGlobalMapper, SysUserMapper sysUserMapper) {
        this.sysTenantGlobalMapper = sysTenantGlobalMapper;
        this.sysUserMapper = sysUserMapper;
    }

    public SysTenantGlobalEntity create(String tenantName) {
        SysTenantGlobalEntity entity = new SysTenantGlobalEntity();
        entity.setTenantName(tenantName);
        entity.setDeleted(0L);
        try {
            sysTenantGlobalMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            throw new BizException(TenantErrorCode.TENANT_NAME_DUPLICATED);
        }
        return entity;
    }

    public void update(Long id, String tenantName) {
        SysTenantGlobalEntity entity = getById(id);
        entity.setTenantName(tenantName);
        try {
            sysTenantGlobalMapper.updateById(entity);
        } catch (DuplicateKeyException e) {
            throw new BizException(TenantErrorCode.TENANT_NAME_DUPLICATED);
        }
    }

    public void delete(Long id) {
        getById(id);
        long userCount = sysUserMapper.countByTenantId(id);
        if (userCount > 0) {
            throw new BizException(TenantErrorCode.TENANT_HAS_USERS);
        }
        SysTenantGlobalEntity entity = new SysTenantGlobalEntity();
        entity.setId(id);
        entity.setDeleted(1L);
        sysTenantGlobalMapper.updateById(entity);
    }

    public boolean exists(Long tenantId) {
        Long count = sysTenantGlobalMapper.selectCount(
                Wrappers.<SysTenantGlobalEntity>lambdaQuery()
                        .eq(SysTenantGlobalEntity::getId, tenantId)
        );
        return count != null && count > 0;
    }

    private SysTenantGlobalEntity getById(Long id) {
        SysTenantGlobalEntity entity = sysTenantGlobalMapper.selectById(id);
        if (entity == null) {
            throw new BizException(TenantErrorCode.TENANT_NOT_FOUND);
        }
        return entity;
    }
}
