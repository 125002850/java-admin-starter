package com.demo.system.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "创建用户响应")
public class UserRegisterRspDTO {

    @Schema(description = "新建用户ID")
    private Long userId;

    public UserRegisterRspDTO() {}

    public UserRegisterRspDTO(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
