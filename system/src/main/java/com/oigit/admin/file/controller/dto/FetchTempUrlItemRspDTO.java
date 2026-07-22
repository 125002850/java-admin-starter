package com.oigit.admin.file.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "文件临时访问地址项")
public class FetchTempUrlItemRspDTO {

    @Schema(description = "对象键", example = "exports/global-dict/a.csv")
    private String objectKey;

    @Schema(description = "临时访问地址", example = "/local-files/exports/global-dict/a.csv")
    private String tempUrl;

    public FetchTempUrlItemRspDTO() {
    }

    public FetchTempUrlItemRspDTO(String objectKey, String tempUrl) {
        this.objectKey = objectKey;
        this.tempUrl = tempUrl;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getTempUrl() {
        return tempUrl;
    }

    public void setTempUrl(String tempUrl) {
        this.tempUrl = tempUrl;
    }
}
