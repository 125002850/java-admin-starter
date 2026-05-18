package com.demo.system.app;

import com.demo.system.controller.dto.UserDeleteReqDTO;
import com.demo.system.controller.dto.UserPasswordUpdateReqDTO;
import com.demo.system.controller.dto.UserProfileUpdateReqDTO;
import com.demo.system.controller.dto.UserRegisterReqDTO;
import com.demo.system.controller.dto.UserRegisterRspDTO;
import com.demo.system.controller.dto.UserStatusUpdateReqDTO;
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

    @Transactional
    public void updateStatus(UserStatusUpdateReqDTO reqDTO) {
        userService.updateStatus(reqDTO.getId(), reqDTO.getEnabled());
    }

    @Transactional
    public void updatePassword(UserPasswordUpdateReqDTO reqDTO) {
        userService.updatePassword(reqDTO.getId(), reqDTO.getPassword());
    }

    @Transactional
    public void updateProfile(UserProfileUpdateReqDTO reqDTO) {
        userService.updateProfile(reqDTO.getId(), reqDTO.getDisplayName(), reqDTO.getMobile(), reqDTO.getEmail());
    }

    @Transactional
    public void delete(UserDeleteReqDTO reqDTO) {
        userService.delete(reqDTO.getId());
    }
}
