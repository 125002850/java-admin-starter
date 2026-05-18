package com.demo.mdm.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "租户字典类型响应")
public class DictTypeRspDTO {

    @Schema(description = "字典类型ID", example = "1")
    private final Long id;

    @Schema(description = "字典类型编码", example = "user_status")
    private final String dictTypeCode;

    @Schema(description = "字典类型名称", example = "用户状态")
    private final String dictTypeName;

    public DictTypeRspDTO(Long id, String dictTypeCode, String dictTypeName) {
        this.id = id;
        this.dictTypeCode = dictTypeCode;
        this.dictTypeName = dictTypeName;
    }

    public Long getId() {
        return id;
    }

    public String getDictTypeCode() {
        return dictTypeCode;
    }

    public String getDictTypeName() {
        return dictTypeName;
    }
}
