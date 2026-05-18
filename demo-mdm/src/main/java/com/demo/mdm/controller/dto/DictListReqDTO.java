package com.demo.mdm.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "字典项查询请求")
public class DictListReqDTO {

    @NotNull
    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    @NotBlank
    @Schema(description = "字典类型编码", example = "GENDER")
    private String dictTypeCode;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getDictTypeCode() {
        return dictTypeCode;
    }

    public void setDictTypeCode(String dictTypeCode) {
        this.dictTypeCode = dictTypeCode;
    }
}
