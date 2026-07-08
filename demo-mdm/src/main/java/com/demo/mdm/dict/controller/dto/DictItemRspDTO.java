package com.demo.mdm.dict.controller.dto;

import com.demo.core.web.AuditRspDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "字典项响应")
public class DictItemRspDTO extends AuditRspDTO {

    @Schema(description = "字典项ID", example = "101")
    private Long id;
    @Schema(description = "字典类型编码", example = "GENDER")
    private final String dictTypeCode;
    @Schema(description = "字典项编码", example = "MALE")
    private final String dictItemCode;
    @Schema(description = "字典项名称", example = "男")
    private final String dictItemName;

    @Schema(description = "排序号", example = "1")
    private Integer sortOrder;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "状态：enable-启用，disable-禁用", allowableValues = {"enable", "disable"}, example = "enable")
    private String status;

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

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
