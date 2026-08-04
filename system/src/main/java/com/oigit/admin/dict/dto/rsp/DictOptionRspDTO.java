package com.oigit.admin.dict.dto.rsp;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "精简字典选项")
public class DictOptionRspDTO {

    @Schema(description = "字典项编码", example = "enable")
    private String code;
    @Schema(description = "字典项显示名", example = "启用")
    private String name;
    @Schema(description = "状态编码；禁用项仍返回以便历史数据显示", example = "enable")
    private String status;
    @Schema(description = "排序号", example = "1")
    private Integer sortOrder;

    public DictOptionRspDTO() {
    }

    public DictOptionRspDTO(String code, String name, String status, Integer sortOrder) {
        this.code = code;
        this.name = name;
        this.status = status;
        this.sortOrder = sortOrder;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }
}
