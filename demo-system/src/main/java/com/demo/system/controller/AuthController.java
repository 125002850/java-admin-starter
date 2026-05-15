package com.demo.system.controller;

import com.demo.core.web.R;
import com.demo.system.app.AuthAppService;
import com.demo.system.controller.dto.LoginReqDTO;
import com.demo.system.controller.dto.LoginRspDTO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/system/auth")
public class AuthController {

    private final AuthAppService authAppService;

    public AuthController(AuthAppService authAppService) {
        this.authAppService = authAppService;
    }

    @PostMapping("/login")
    public R<LoginRspDTO> login(@Valid @RequestBody LoginReqDTO reqDTO) {
        return R.ok(authAppService.login(reqDTO));
    }
}
