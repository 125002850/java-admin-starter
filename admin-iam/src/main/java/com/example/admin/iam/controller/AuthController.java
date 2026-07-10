package com.example.admin.iam.controller;

import com.example.admin.core.web.R;
import com.example.admin.iam.app.AuthAppService;
import com.example.admin.iam.dto.IamAuthDTO.ChangePasswordReqDTO;
import com.example.admin.iam.dto.IamAuthDTO.ChangePasswordRspDTO;
import com.example.admin.iam.dto.IamAuthDTO.LoginReqDTO;
import com.example.admin.iam.dto.IamAuthDTO.LoginRspDTO;
import com.example.admin.iam.dto.IamAuthDTO.LogoutReqDTO;
import com.example.admin.iam.dto.IamAuthDTO.MeRspDTO;
import com.example.admin.iam.dto.IamAuthDTO.RefreshReqDTO;
import com.example.admin.iam.dto.IamAuthDTO.TokenRspDTO;
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
@Tag(name = "IAM认证", description = "本地 IAM 登录、token、当前用户与密码相关接口")
@RequestMapping("/api/iam/auth")
public class AuthController {

    private final AuthAppService authAppService;

    public AuthController(AuthAppService authAppService) {
        this.authAppService = authAppService;
    }

    @Operation(summary = "登录", description = "使用本地员工用户名和密码登录", operationId = "iamAuthLogin")
    @PostMapping("/login")
    public R<LoginRspDTO> login(@Valid @RequestBody LoginReqDTO reqDTO) {
        return R.ok(authAppService.login(reqDTO));
    }

    @Operation(summary = "刷新 token", description = "使用 refresh token 轮换新的 access token 与 refresh token", operationId = "iamAuthRefresh")
    @PostMapping("/refresh")
    public R<TokenRspDTO> refresh(@Valid @RequestBody RefreshReqDTO reqDTO) {
        return R.ok(authAppService.refresh(reqDTO));
    }

    @Operation(summary = "退出登录", description = "失效当前 refresh token", operationId = "iamAuthLogout")
    @PostMapping("/logout")
    public R<Void> logout(@RequestBody(required = false) LogoutReqDTO reqDTO) {
        authAppService.logout(reqDTO);
        return R.ok();
    }

    @Operation(summary = "获取当前用户", description = "返回当前员工、角色、权限、菜单和数据权限快照", operationId = "iamAuthMe")
    @PostMapping("/me")
    public R<MeRspDTO> me() {
        return R.ok(authAppService.me());
    }

    @Operation(summary = "修改本人密码", description = "校验旧密码并修改为新密码，成功后重新签发 token", operationId = "iamAuthPasswordChange")
    @PostMapping("/password/change")
    public R<ChangePasswordRspDTO> changePassword(@Valid @RequestBody ChangePasswordReqDTO reqDTO) {
        return R.ok(authAppService.changePassword(reqDTO));
    }
}
