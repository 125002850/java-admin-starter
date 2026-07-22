package com.oigit.admin.file.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "文件上传请求")
public class UploadFileReqDTO {

    @NotBlank(message = "业务路径不能为空")
    @Pattern(regexp = "^[A-Za-z0-9/_-]+$", message = "业务路径格式非法")
    @Schema(description = "业务路径", example = "avatar/user")
    private String bizPath;

    @Schema(description = "对象键，可选", example = "avatar/user/custom-key.png")
    private String objectKey;

    public String getBizPath() {
        return bizPath;
    }

    public void setBizPath(String bizPath) {
        this.bizPath = bizPath;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }
}
