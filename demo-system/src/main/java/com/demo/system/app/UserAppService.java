package com.demo.system.app;

import com.demo.system.controller.dto.UserRegisterReqDTO;
import com.demo.system.controller.dto.UserRegisterRspDTO;
import com.demo.system.infra.entity.SysUserEntity;
import com.demo.system.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAppService {

    private final UserService userService;

    public UserAppService(UserService userService) {
        this.userService = userService;
    }

    @Transactional
    public UserRegisterRspDTO create(UserRegisterReqDTO reqDTO) {
        SysUserEntity entity = userService.create(
                reqDTO.getUsername(),
                reqDTO.getPassword(),
                reqDTO.getDisplayName(),
                reqDTO.getMobile(),
                reqDTO.getEmail()
        );
        return new UserRegisterRspDTO(entity.getId());
    }
}
