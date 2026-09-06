package com.oigit.admin.iam.dto.req;

import com.oigit.admin.core.web.PageReqDTO;
import com.oigit.admin.iam.enums.IamStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "角色分页请求")
public class RolePageReqDTO extends PageReqDTO {
    @Schema(description = "关键字，匹配角色编码或名称")
    public String keyword;

    @Schema(description = "状态")
    public IamStatus status;
}
