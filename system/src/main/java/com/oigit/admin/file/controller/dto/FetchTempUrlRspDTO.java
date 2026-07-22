package com.oigit.admin.file.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "文件临时访问地址响应")
public class FetchTempUrlRspDTO {

    @Schema(description = "对象键", example = "avatar/user/2026/05/19/example.png")
    private String objectKey;

    @Schema(description = "临时访问地址", example = "/local-files/avatar/user/2026/05/19/example.png")
    private String tempUrl;

    public FetchTempUrlRspDTO() {
    }

    public FetchTempUrlRspDTO(String objectKey, String tempUrl) {
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
