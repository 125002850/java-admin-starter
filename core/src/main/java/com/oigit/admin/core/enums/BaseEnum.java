package com.oigit.admin.core.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 业务枚举基础接口。
 * JSON 和 OpenAPI 只暴露稳定编码；描述由字典表统一提供给前端与导出。
 */
public interface BaseEnum {

    @JsonValue
    String getCode();

    String getDesc();
}
