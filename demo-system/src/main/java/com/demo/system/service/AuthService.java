package com.demo.system.service;

import com.demo.core.exception.BizException;
import com.demo.system.enums.AuthErrorCode;
import com.demo.system.infra.entity.SysUserEntity;
import com.demo.system.infra.mapper.SysUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthService(SysUserMapper sysUserMapper, PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public SysUserEntity authenticate(Long tenantId, String username, String password) {
        List<SysUserEntity> users = sysUserMapper.selectForLogin(tenantId, username);
        if (users.size() > 1) {
            throw new BizException(AuthErrorCode.USERNAME_DUPLICATED);
        }
        SysUserEntity user = users.isEmpty() ? null : users.get(0);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BizException(AuthErrorCode.USERNAME_OR_PASSWORD_INVALID);
        }
        return user;
    }
}
