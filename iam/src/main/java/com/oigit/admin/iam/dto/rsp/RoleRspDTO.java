package com.oigit.admin.iam.dto.rsp;

import com.oigit.admin.core.web.AuditRspDTO;
import com.oigit.admin.iam.enums.DataScopeType;
import com.oigit.admin.iam.enums.IamStatus;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "角色响应")
public class RoleRspDTO extends AuditRspDTO {
    public Long roleId;
    public String roleCode;
    public String roleName;
    public Integer sortOrder;
    public IamStatus status;
    public DataScopeType dataScopeType;
    public Boolean systemBuiltIn;
    public String remark;
    public List<Long> menuIds = new ArrayList<>();
    public List<Long> dataScopeDeptIds = new ArrayList<>();
}
