package com.demo.mdm.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "字典项响应")
public class DictItemRspDTO {

    @Schema(description = "字典项ID", example = "101")
    private final Long id;
    @Schema(description = "字典类型编码", example = "GENDER")
    private final String dictTypeCode;
    @Schema(description = "字典项编码", example = "MALE")
    private final String dictItemCode;
    @Schema(description = "字典项名称", example = "男")
    private final String dictItemName;

    public DictItemRspDTO(Long id, String dictTypeCode, String dictItemCode, String dictItemName) {
        this.id = id;
        this.dictTypeCode = dictTypeCode;
        this.dictItemCode = dictItemCode;
        this.dictItemName = dictItemName;
    }

    public Long getId() {
        return id;
    }

    public String getDictTypeCode() {
        return dictTypeCode;
    }

    public String getDictItemCode() {
        return dictItemCode;
    }

    public String getDictItemName() {
        return dictItemName;
    }
}
