package com.demo.system.controller;

import com.demo.core.web.R;
import com.demo.system.app.UserAppService;
import com.demo.system.controller.dto.UserDeleteReqDTO;
import com.demo.system.controller.dto.UserPasswordUpdateReqDTO;
import com.demo.system.controller.dto.UserProfileUpdateReqDTO;
import com.demo.system.controller.dto.UserRegisterReqDTO;
import com.demo.system.controller.dto.UserRegisterRspDTO;
import com.demo.system.controller.dto.UserStatusUpdateReqDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@Tag(name = "用户管理", description = "租户内用户增删改接口")
@RequestMapping("/api/system/user")
public class UserController {

    private final UserAppService userAppService;

    public UserController(UserAppService userAppService) {
        this.userAppService = userAppService;
    }

    @Operation(summary = "创建用户", description = "在当前租户下创建用户，用户名租户内唯一，密码至少6位")
    @PostMapping("/create")
    public R<UserRegisterRspDTO> create(@Valid @RequestBody UserRegisterReqDTO reqDTO) {
        return R.ok(userAppService.create(reqDTO));
    }

    @Operation(summary = "修改用户状态", description = "启用或禁用当前租户下指定用户")
    @PostMapping("/status/update")
    public R<Void> updateStatus(@Valid @RequestBody UserStatusUpdateReqDTO reqDTO) {
        userAppService.updateStatus(reqDTO);
        return R.ok();
    }

    @Operation(summary = "修改用户密码", description = "修改当前租户下指定用户的密码，密码至少6位")
    @PostMapping("/password/update")
    public R<Void> updatePassword(@Valid @RequestBody UserPasswordUpdateReqDTO reqDTO) {
        userAppService.updatePassword(reqDTO);
        return R.ok();
    }

    @Operation(summary = "修改用户信息", description = "修改当前租户下指定用户的显示名称、手机号、邮箱")
    @PostMapping("/profile/update")
    public R<Void> updateProfile(@Valid @RequestBody UserProfileUpdateReqDTO reqDTO) {
        userAppService.updateProfile(reqDTO);
        return R.ok();
    }

    @Operation(summary = "删除用户", description = "逻辑删除当前租户下指定用户，删除后不可登录")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody UserDeleteReqDTO reqDTO) {
        userAppService.delete(reqDTO);
        return R.ok();
    }
}
