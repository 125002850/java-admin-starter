package com.demo.file.controller.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "批量获取文件临时访问地址响应")
public class FetchTempUrlBatchRspDTO {

    @Schema(description = "临时访问地址列表")
    private List<FetchTempUrlItemRspDTO> items;

    public FetchTempUrlBatchRspDTO() {
    }

    public FetchTempUrlBatchRspDTO(List<FetchTempUrlItemRspDTO> items) {
        this.items = items;
    }

    public List<FetchTempUrlItemRspDTO> getItems() {
        return items;
    }

    public void setItems(List<FetchTempUrlItemRspDTO> items) {
        this.items = items;
    }
}
