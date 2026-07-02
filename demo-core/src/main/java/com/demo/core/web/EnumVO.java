package com.demo.core.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 通用枚举值对象，作为 BaseEnum 枚举在 JSON 和 OpenAPI 中的标准输出结构。
 */
@Schema(description = "枚举对象")
public class EnumVO {

    @Schema(description = "编码", example = "enable")
    private final String code;

    @Schema(description = "描述", example = "启用")
    private final String desc;

    public EnumVO(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
