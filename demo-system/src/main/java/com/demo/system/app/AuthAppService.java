package com.demo.system.app;

import com.demo.system.controller.dto.LoginReqDTO;
import com.demo.system.controller.dto.LoginRspDTO;
import com.demo.system.infra.entity.SysUserEntity;
import com.demo.system.service.AuthService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthAppService {

    private final AuthService authService;

    public AuthAppService(AuthService authService) {
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public LoginRspDTO login(LoginReqDTO reqDTO) {
        SysUserEntity user = authService.authenticate(reqDTO.getTenantId(), reqDTO.getUsername(), reqDTO.getPassword());
        return new LoginRspDTO(user.getId(), user.getTenantId(), user.getUsername());
    }
}
