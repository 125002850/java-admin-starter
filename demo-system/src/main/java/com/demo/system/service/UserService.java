package com.demo.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.demo.core.exception.BizException;
import com.demo.core.tenant.TenantContext;
import com.demo.system.enums.TenantErrorCode;
import com.demo.system.enums.UserErrorCode;
import com.demo.system.enums.UserStatusEnum;
import com.demo.system.infra.entity.SysUserEntity;
import com.demo.system.infra.mapper.SysUserMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final SysUserMapper sysUserMapper;
    private final TenantService tenantService;
    private final PasswordEncoder passwordEncoder;

    public UserService(SysUserMapper sysUserMapper, TenantService tenantService, PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.tenantService = tenantService;
        this.passwordEncoder = passwordEncoder;
    }

    public SysUserEntity create(String username, String password, String displayName, String mobile, String email) {
        Long tenantId = TenantContext.requireTenantId();

        if (!tenantService.exists(tenantId)) {
            throw new BizException(TenantErrorCode.TENANT_NOT_FOUND);
        }

        SysUserEntity entity = new SysUserEntity();
        entity.setTenantId(tenantId);
        entity.setUsername(username);
        entity.setPassword(passwordEncoder.encode(password));
        entity.setStatus(UserStatusEnum.ENABLED.getValue());
        entity.setDisplayName(displayName);
        entity.setMobile(mobile);
        entity.setEmail(email);
        entity.setDeleted(0L);

        try {
            sysUserMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            throw new BizException(UserErrorCode.USERNAME_DUPLICATED);
        }
        return entity;
    }

    public void updateStatus(Long id, boolean enabled) {
        Long tenantId = TenantContext.requireTenantId();
        SysUserEntity entity = getByTenantAndId(tenantId, id);
        entity.setStatus(UserStatusEnum.fromEnabled(enabled).getValue());
        sysUserMapper.updateById(entity);
    }

    public SysUserEntity getByTenantAndId(Long tenantId, Long id) {
        SysUserEntity entity = sysUserMapper.selectOne(
                Wrappers.<SysUserEntity>lambdaQuery()
                        .eq(SysUserEntity::getTenantId, tenantId)
                        .eq(SysUserEntity::getId, id)
                        .last("limit 1")
        );
        if (entity == null) {
            throw new BizException(UserErrorCode.USER_NOT_FOUND);
        }
        return entity;
    }
}
