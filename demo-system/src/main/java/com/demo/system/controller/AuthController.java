package com.demo.system.controller;

import com.demo.core.web.R;
import com.demo.system.app.AuthAppService;
import com.demo.system.controller.dto.LoginReqDTO;
import com.demo.system.controller.dto.LoginRspDTO;
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
@Tag(name = "系统认证", description = "登录与认证相关接口")
@RequestMapping("/api/system/auth")
public class AuthController {

    private final AuthAppService authAppService;

    public AuthController(AuthAppService authAppService) {
        this.authAppService = authAppService;
    }

    @Operation(summary = "租户用户登录", description = "根据租户、用户名和密码完成登录")
    @PostMapping("/login")
    public R<LoginRspDTO> login(@Valid @RequestBody LoginReqDTO reqDTO) {
        return R.ok(authAppService.login(reqDTO));
    }
}
