package com.demo.mdm.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "租户字典项删除请求")
public class DictItemDeleteReqDTO {

    @NotNull
    @Schema(description = "字典项ID", example = "1")
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
