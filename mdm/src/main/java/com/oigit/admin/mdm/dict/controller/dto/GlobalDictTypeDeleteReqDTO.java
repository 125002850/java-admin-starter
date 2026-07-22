package com.oigit.admin.mdm.dict.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "全局字典类型删除请求")
public class GlobalDictTypeDeleteReqDTO {

    @NotNull
    @Schema(description = "字典类型ID", example = "1")
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
