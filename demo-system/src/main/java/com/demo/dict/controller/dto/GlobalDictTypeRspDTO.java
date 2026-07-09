package com.demo.dict.controller.dto;

import com.demo.core.web.AuditRspDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "全局字典类型响应")
public class GlobalDictTypeRspDTO extends AuditRspDTO {

    @Schema(description = "字典类型ID", example = "1")
    private Long id;

    @Schema(description = "字典类型编码", example = "gender")
    private String dictTypeCode;

    @Schema(description = "字典类型名称", example = "性别")
    private String dictTypeName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "状态：enable-启用，disable-禁用", allowableValues = {"enable", "disable"}, example = "enable")
    private String status;

    public GlobalDictTypeRspDTO() {
    }

    public GlobalDictTypeRspDTO(Long id, String dictTypeCode, String dictTypeName) {
        this.id = id;
        this.dictTypeCode = dictTypeCode;
        this.dictTypeName = dictTypeName;
    }

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
