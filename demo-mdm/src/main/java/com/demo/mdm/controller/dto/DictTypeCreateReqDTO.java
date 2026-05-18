package com.demo.mdm.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "租户字典类型创建请求")
public class DictTypeCreateReqDTO {

    @NotBlank
    @Schema(description = "字典类型编码", example = "user_status")
    private String dictTypeCode;

    @NotBlank
    @Schema(description = "字典类型名称", example = "用户状态")
    private String dictTypeName;

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
