package com.oigit.admin.dict.dto.rsp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "按类型分组的字典选项")
public class DictOptionGroupRspDTO {

    @Schema(description = "字典类型编码", example = "ENABLE_STATUS")
    private String dictTypeCode;
    @Schema(description = "字典项；包含启用和禁用项，不包含逻辑删除项")
    private List<DictOptionRspDTO> items;

    public DictOptionGroupRspDTO() {
    }

    public DictOptionGroupRspDTO(String dictTypeCode, List<DictOptionRspDTO> items) {
        this.dictTypeCode = dictTypeCode;
        this.items = items;
    }

    public String getDictTypeCode() {
        return dictTypeCode;
    }

    public List<DictOptionRspDTO> getItems() {
        return items;
    }
}
