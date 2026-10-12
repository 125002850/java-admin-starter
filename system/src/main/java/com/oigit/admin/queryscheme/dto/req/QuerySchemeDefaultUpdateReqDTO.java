package com.oigit.admin.queryscheme.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "设置或取消默认查询方案请求")
public class QuerySchemeDefaultUpdateReqDTO {
    @NotBlank @Size(max = 128) @Pattern(regexp = "[a-z0-9.-]+")
    @Schema(description = "动态查询场景编码")
    private String sceneCode;
    @Positive
    @Schema(description = "默认方案 ID；不传表示取消默认方案")
    private Long schemeId;
    public String getSceneCode() { return sceneCode; }
    public void setSceneCode(String sceneCode) { this.sceneCode = sceneCode; }
    public Long getSchemeId() { return schemeId; }
    public void setSchemeId(Long schemeId) { this.schemeId = schemeId; }
}
