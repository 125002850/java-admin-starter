package com.example.admin.file.controller.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

@Schema(description = "批量获取文件临时访问地址请求")
public class FetchTempUrlBatchReqDTO {

    @NotEmpty(message = "对象键列表不能为空")
    @Size(max = 200, message = "对象键数量不能超过200")
    @Schema(description = "对象键列表，最多200个，重复对象键会按首次出现顺序去重",
            example = "[\"exports/global-dict/a.csv\",\"exports/global-dict/b.csv\"]")
    private List<@NotBlank(message = "对象键不能为空") String> objectKeys;

    public List<String> getObjectKeys() {
        return objectKeys;
    }

    public void setObjectKeys(List<String> objectKeys) {
        this.objectKeys = objectKeys;
    }
}
