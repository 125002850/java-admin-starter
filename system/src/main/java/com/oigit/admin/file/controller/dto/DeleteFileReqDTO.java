package com.oigit.admin.file.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "删除文件请求")
public class DeleteFileReqDTO {

    @NotBlank(message = "对象键不能为空")
    @Schema(description = "对象键", example = "avatar/user/2026/05/19/example.png")
    private String objectKey;

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }
}
