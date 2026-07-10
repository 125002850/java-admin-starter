package com.example.admin.export.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "提交导出响应")
public class ExportSubmitRspDTO extends ExportRecordRspDTO {

    @Schema(description = "临时下载地址，过期后可通过导出中心下载接口重新获取", example = "https://example.com/download/export.csv")
    private String downloadUrl;

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}
