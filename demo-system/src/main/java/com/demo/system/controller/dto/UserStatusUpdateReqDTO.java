package com.demo.system.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "修改用户状态请求")
public class UserStatusUpdateReqDTO {

    @NotNull
    @Schema(description = "用户ID", example = "1")
    private Long id;

    @NotNull
    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
