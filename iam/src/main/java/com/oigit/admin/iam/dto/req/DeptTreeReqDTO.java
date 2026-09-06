package com.oigit.admin.iam.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "部门树查询请求")
public class DeptTreeReqDTO {
    @Schema(description = "关键字，匹配部门编码或名称")
    public String keyword;
}
