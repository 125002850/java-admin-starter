package com.example.admin.export.controller.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "导出提交请求")
public class ExportSubmitReqDTO {

    @NotBlank
    @Schema(description = "导出场景编码", example = "mdm.global.dict.type.list")
    private String sceneCode;

    @Schema(description = "导出查询参数，结构由具体场景定义")
    private JsonNode query;

    public String getSceneCode() {
        return sceneCode;
    }

    public void setSceneCode(String sceneCode) {
        this.sceneCode = sceneCode;
    }

    public JsonNode getQuery() {
        return query;
    }

    public void setQuery(JsonNode query) {
        this.query = query;
    }
}
