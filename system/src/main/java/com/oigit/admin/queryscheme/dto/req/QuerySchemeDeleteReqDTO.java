package com.oigit.admin.queryscheme.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "删除个人查询方案请求")
public class QuerySchemeDeleteReqDTO {
    @Schema(description = "方案 ID")
    @NotNull @Positive private Long id;
    @Schema(description = "乐观锁版本")
    @NotNull @PositiveOrZero private Integer version;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
