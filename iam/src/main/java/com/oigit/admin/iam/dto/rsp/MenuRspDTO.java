package com.oigit.admin.iam.dto.rsp;

import com.oigit.admin.core.web.AuditRspDTO;
import com.oigit.admin.iam.enums.IamStatus;
import com.oigit.admin.iam.enums.MenuType;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "菜单响应")
public class MenuRspDTO extends AuditRspDTO {
    public Long menuId;
    public Long parentId;
    public String menuCode;
    public String menuKey;
    public String menuName;
    public MenuType menuType;
    public String routePath;
    public String componentPath;
    public String icon;
    public Integer sortOrder;
    public Boolean hidden;
    public Boolean cached;
    public IamStatus status;
    public String permissionCode;
    public String remark;
    public List<MenuRspDTO> children = new ArrayList<>();
}
