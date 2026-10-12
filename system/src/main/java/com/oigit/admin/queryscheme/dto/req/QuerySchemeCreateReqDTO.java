package com.oigit.admin.queryscheme.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "新增个人查询方案请求")
public class QuerySchemeCreateReqDTO {
    @NotBlank @Size(max = 128) @Pattern(regexp = "[a-z0-9.-]+")
    @Schema(description = "动态查询场景编码", example = "mdm.global.dict.item.page")
    private String sceneCode;
    @NotBlank @Size(max = 64)
    @Schema(description = "方案名称", example = "启用的海运费项")
    private String schemeName;
    @NotBlank @Size(max = 65535)
    @Schema(description = "版本化查询条件 JSON")
    private String contentJson;
    @Schema(description = "创建后是否设为默认方案")
    private boolean defaultScheme;

    public String getSceneCode() { return sceneCode; }
    public void setSceneCode(String sceneCode) { this.sceneCode = sceneCode; }
    public String getSchemeName() { return schemeName; }
    public void setSchemeName(String schemeName) { this.schemeName = schemeName; }
    public String getContentJson() { return contentJson; }
    public void setContentJson(String contentJson) { this.contentJson = contentJson; }
    public boolean isDefaultScheme() { return defaultScheme; }
    public void setDefaultScheme(boolean defaultScheme) { this.defaultScheme = defaultScheme; }
}
