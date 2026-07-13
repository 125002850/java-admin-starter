package com.example.admin.iam.dto;

import com.example.admin.iam.enums.IamStatus;
import com.example.admin.iam.enums.MenuType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class IamMenuDTO {

    private IamMenuDTO() {
    }

    @Schema(description = "菜单树查询请求")
    public static class MenuTreeReqDTO {
        @Schema(description = "关键字，匹配菜单编码、名称或权限标识")
        public String keyword;
    }

    @Schema(description = "菜单ID请求")
    public static class MenuIdReqDTO {
        @NotNull
        @Schema(description = "菜单ID", example = "1000", requiredMode = Schema.RequiredMode.REQUIRED)
        public Long menuId;
    }

    @Schema(description = "创建菜单请求")
    public static class MenuCreateReqDTO {
        @Schema(description = "父菜单ID")
        public Long parentId;
        @NotBlank
        @Schema(description = "菜单编码", example = "iam_staff", requiredMode = Schema.RequiredMode.REQUIRED)
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

    @Schema(description = "更新菜单请求")
    public static class MenuUpdateReqDTO extends MenuCreateReqDTO {
        @NotNull
        @Schema(description = "菜单ID", example = "1000", requiredMode = Schema.RequiredMode.REQUIRED)
        public Long menuId;
    }

    @Schema(description = "菜单状态更新请求")
    public static class MenuStatusUpdateReqDTO extends MenuIdReqDTO {
        @NotNull
        @Schema(description = "状态", example = "DISABLED", requiredMode = Schema.RequiredMode.REQUIRED)
        public IamStatus status;
    }

    @Schema(description = "菜单响应")
    public static class MenuRspDTO {
        public Long menuId;
        public Long parentId;
        public String menuCode;
        public String menuKey;
        public String menuName;
        public String menuType;
        public String routePath;
        public String componentPath;
        public String icon;
        public Integer sortOrder;
        public Boolean hidden;
        public Boolean cached;
        public String status;
        public String permissionCode;
        public String remark;
        public List<MenuRspDTO> children = new ArrayList<>();
        public LocalDateTime createTime;
        public LocalDateTime updateTime;
        @Schema(description = "创建人用户名", example = "admin")
        public String createBy;
        @Schema(description = "更新人用户名", example = "admin")
        public String updateBy;
    }
}
