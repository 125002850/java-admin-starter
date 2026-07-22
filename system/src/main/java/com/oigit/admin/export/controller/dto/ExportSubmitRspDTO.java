package com.oigit.admin.export.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "提交导出响应")
public class ExportSubmitRspDTO extends ExportRecordRspDTO {

    @Schema(description = "临时下载地址；异步任务提交时为空，任务成功后通过下载接口获取", example = "https://example.com/download/export.csv")
    private String downloadUrl;

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}
