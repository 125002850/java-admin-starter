package com.demo.dict.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "全局字典类型全量查询请求（不分页）")
public class GlobalDictTypeListReqDTO {

    @Schema(description = "关键字，按字典类型编码或名称模糊查询", example = "status")
    private String keyword;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
