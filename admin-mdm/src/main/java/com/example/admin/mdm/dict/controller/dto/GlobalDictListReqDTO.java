package com.example.admin.mdm.dict.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "全局字典项查询请求")
public class GlobalDictListReqDTO {

    @NotBlank
    @Schema(description = "字典类型编码", example = "GENDER")
    private String dictTypeCode;

    public String getDictTypeCode() {
        return dictTypeCode;
    }

    public void setDictTypeCode(String dictTypeCode) {
        this.dictTypeCode = dictTypeCode;
    }
}
