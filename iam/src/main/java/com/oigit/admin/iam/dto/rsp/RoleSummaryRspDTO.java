package com.oigit.admin.iam.dto.rsp;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "IAM角色摘要")
public class RoleSummaryRspDTO {
    private Long roleId;
    private String roleCode;
    private String roleName;
    private String status;
    private String dataScopeType;
    private Integer sortOrder;
    private Boolean systemBuiltIn;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDataScopeType() {
        return dataScopeType;
    }

    public void setDataScopeType(String dataScopeType) {
        this.dataScopeType = dataScopeType;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getSystemBuiltIn() {
        return systemBuiltIn;
    }

    public void setSystemBuiltIn(Boolean systemBuiltIn) {
        this.systemBuiltIn = systemBuiltIn;
    }
}
