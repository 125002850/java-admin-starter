package com.demo.mdm.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "全局字典项删除请求（支持多选）")
public class GlobalDictItemDeleteReqDTO {

    @NotEmpty
    @Schema(description = "字典项ID列表", example = "[1, 2, 3]")
    private List<Long> ids;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }
}
