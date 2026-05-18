package com.demo.system.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "修改用户信息请求")
public class UserProfileUpdateReqDTO {

    @NotNull
    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Size(max = 64)
    @Schema(description = "显示名称", example = "张三")
    private String displayName;

    @Size(max = 32)
    @Schema(description = "手机号", example = "13900139000")
    private String mobile;

    @Size(max = 128)
    @Schema(description = "邮箱", example = "zhangsan@test.com")
    private String email;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
