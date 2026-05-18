package com.demo.mdm.controller.dto;

import com.demo.core.web.PageReqDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "全局字典列表查询请求")
public class GlobalDictTypeListReqDTO extends PageReqDTO {

    @Schema(description = "关键字，按字典类型编码或名称模糊查询", example = "status")
    private String keyword;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
