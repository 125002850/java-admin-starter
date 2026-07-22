package com.oigit.admin.dict.controller.dto;

import com.oigit.admin.core.enums.EnableStatusEnum;
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

    @Schema(description = "排序号，升序排列", example = "1")
    private Integer sortOrder;

    @Schema(description = "备注", example = "性别标识")
    private String remark;

    @Schema(description = "状态：enable-启用，disable-禁用", allowableValues = {"enable", "disable"}, example = "enable")
    private EnableStatusEnum status;

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

    public EnableStatusEnum getStatus() {
        return status;
    }

    public void setStatus(EnableStatusEnum status) {
        this.status = status;
    }
}
