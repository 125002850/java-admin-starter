package com.oigit.admin.queryscheme.dto.rsp;

import com.oigit.admin.core.web.AuditRspDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "个人查询方案")
public class QuerySchemeRspDTO extends AuditRspDTO {
    @Schema(description = "方案 ID")
    private Long id;
    @Schema(description = "动态查询场景编码")
    private String sceneCode;
    @Schema(description = "方案名称")
    private String schemeName;
    @Schema(description = "版本化查询条件 JSON")
    private String contentJson;
    @Schema(description = "方案内容版本")
    private Integer schemaVersion;
    @Schema(description = "是否默认方案")
    private boolean defaultScheme;
    @Schema(description = "乐观锁版本")
    private Integer version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSceneCode() { return sceneCode; }
    public void setSceneCode(String sceneCode) { this.sceneCode = sceneCode; }
    public String getSchemeName() { return schemeName; }
    public void setSchemeName(String schemeName) { this.schemeName = schemeName; }
    public String getContentJson() { return contentJson; }
    public void setContentJson(String contentJson) { this.contentJson = contentJson; }
    public Integer getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(Integer schemaVersion) { this.schemaVersion = schemaVersion; }
    public boolean isDefaultScheme() { return defaultScheme; }
    public void setDefaultScheme(boolean defaultScheme) { this.defaultScheme = defaultScheme; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
