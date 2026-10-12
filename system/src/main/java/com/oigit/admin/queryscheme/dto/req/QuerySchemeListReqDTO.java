package com.oigit.admin.queryscheme.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "查询方案列表请求")
public class QuerySchemeListReqDTO {
    @NotBlank
    @Size(max = 128)
    @Pattern(regexp = "[a-z0-9.-]+")
    @Schema(description = "动态查询场景编码", example = "mdm.global.dict.item.page")
    private String sceneCode;

    public String getSceneCode() { return sceneCode; }
    public void setSceneCode(String sceneCode) { this.sceneCode = sceneCode; }
}
