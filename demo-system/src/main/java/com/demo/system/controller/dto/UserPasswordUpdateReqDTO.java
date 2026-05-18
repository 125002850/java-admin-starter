package com.demo.system.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "修改用户密码请求")
public class UserPasswordUpdateReqDTO {

    @NotNull
    @Schema(description = "用户ID", example = "1")
    private Long id;

    @NotBlank
    @Size(min = 6, max = 255)
    @Schema(description = "新密码，长度至少6位", example = "newpass456")
    private String password;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
