package com.oigit.admin.iam.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "菜单树查询请求")
public class MenuTreeReqDTO {
    @Schema(description = "关键字，匹配菜单编码、名称或权限标识")
    public String keyword;
}
