package com.oigit.admin.queryscheme.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Schema(description = "更新个人查询方案请求")
public class QuerySchemeUpdateReqDTO {
    @Schema(description = "方案 ID")
    @NotNull @Positive private Long id;
    @Schema(description = "方案名称")
    @NotBlank @Size(max = 64) private String schemeName;
    @Schema(description = "版本化查询条件 JSON")
    @NotBlank @Size(max = 65535) private String contentJson;
    @Schema(description = "乐观锁版本")
    @NotNull @PositiveOrZero private Integer version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSchemeName() { return schemeName; }
    public void setSchemeName(String schemeName) { this.schemeName = schemeName; }
    public String getContentJson() { return contentJson; }
    public void setContentJson(String contentJson) { this.contentJson = contentJson; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
