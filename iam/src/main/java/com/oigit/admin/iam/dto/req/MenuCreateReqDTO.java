package com.oigit.admin.iam.dto.req;

import com.oigit.admin.iam.enums.IamStatus;
import com.oigit.admin.iam.enums.MenuType;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "创建菜单请求")
public class MenuCreateReqDTO {
    @Schema(description = "父菜单ID")
    public Long parentId;

    @NotBlank
    @Schema(
            description = "菜单编码",
            example = "iam_staff",
            requiredMode = Schema.RequiredMode.REQUIRED)
    public String menuCode;

    @NotBlank
    @Schema(description = "菜单名称", example = "员工管理", requiredMode = Schema.RequiredMode.REQUIRED)
    public String menuName;

    @NotNull
    @Schema(description = "菜单类型", example = "MENU", requiredMode = Schema.RequiredMode.REQUIRED)
    public MenuType menuType;

    @Schema(description = "路由路径")
    public String routePath;

    @Schema(description = "组件路径")
    public String componentPath;

    @Schema(description = "图标")
    public String icon;

    @Schema(description = "排序")
    public Integer sortOrder;

    @Schema(description = "是否隐藏")
    public Boolean hidden;

    @Schema(description = "是否缓存")
    public Boolean cached;

    @Schema(description = "状态")
    public IamStatus status;

    @Schema(description = "权限标识")
    public String permissionCode;

    @Schema(description = "备注")
    public String remark;
}
