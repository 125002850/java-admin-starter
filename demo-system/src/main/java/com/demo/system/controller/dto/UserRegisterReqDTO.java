package com.demo.system.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "创建用户请求")
public class UserRegisterReqDTO {

    @NotBlank
    @Size(min = 1, max = 64)
    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    @NotBlank
    @Size(min = 6, max = 255)
    @Schema(description = "密码，长度至少6位", example = "pass123456")
    private String password;

    @Size(max = 64)
    @Schema(description = "显示名称", example = "张三")
    private String displayName;

    @Size(max = 32)
    @Schema(description = "手机号", example = "13800138000")
    private String mobile;

    @Size(max = 128)
    @Schema(description = "邮箱", example = "zhangsan@test.com")
    private String email;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
