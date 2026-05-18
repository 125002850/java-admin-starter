package com.demo.mdm.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "全局字典项创建请求")
public class GlobalDictItemCreateReqDTO {

    @NotBlank
    @Schema(description = "字典类型编码", example = "GENDER")
    private String dictTypeCode;

    @NotBlank
    @Schema(description = "字典项编码", example = "MALE")
    private String dictItemCode;

    @NotBlank
    @Schema(description = "字典项名称", example = "男")
    private String dictItemName;

    public String getDictTypeCode() {
        return dictTypeCode;
    }

    public void setDictTypeCode(String dictTypeCode) {
        this.dictTypeCode = dictTypeCode;
    }

    public String getDictItemCode() {
        return dictItemCode;
    }

    public void setDictItemCode(String dictItemCode) {
        this.dictItemCode = dictItemCode;
    }

    public String getDictItemName() {
        return dictItemName;
    }

    public void setDictItemName(String dictItemName) {
        this.dictItemName = dictItemName;
    }
}
