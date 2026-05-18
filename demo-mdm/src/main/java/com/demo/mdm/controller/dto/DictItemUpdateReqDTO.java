package com.demo.mdm.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "租户字典项修改请求")
public class DictItemUpdateReqDTO {

    @NotNull
    @Schema(description = "字典项ID", example = "1")
    private Long id;

    @NotBlank
    @Schema(description = "字典类型编码", example = "user_status")
    private String dictTypeCode;

    @NotBlank
    @Schema(description = "字典项编码", example = "ENABLED")
    private String dictItemCode;

    @NotBlank
    @Schema(description = "字典项名称", example = "启用")
    private String dictItemName;

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
