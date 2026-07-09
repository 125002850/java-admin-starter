package com.demo.dict.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "全局字典类型修改请求")
public class GlobalDictTypeUpdateReqDTO {

    @NotNull
    @Schema(description = "字典类型ID", example = "1")
    private Long id;

    @NotBlank
    @Schema(description = "字典类型编码", example = "gender")
    private String dictTypeCode;

    @NotBlank
    @Schema(description = "字典类型名称", example = "性别")
    private String dictTypeName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDictTypeCode() {
        return dictTypeCode;
    }

    public void setDictTypeCode(String dictTypeCode) {
        this.dictTypeCode = dictTypeCode;
    }

    public String getDictTypeName() {
        return dictTypeName;
    }

    public void setDictTypeName(String dictTypeName) {
        this.dictTypeName = dictTypeName;
    }
}
