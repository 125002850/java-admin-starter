package com.demo.system.controller;

import com.demo.core.web.R;
import com.demo.system.app.UserAppService;
import com.demo.system.controller.dto.UserRegisterReqDTO;
import com.demo.system.controller.dto.UserRegisterRspDTO;
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
}
