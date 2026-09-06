package com.oigit.admin.iam.dto.rsp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "IAM数据权限摘要")
public class DataScopeSummaryRspDTO {
    private String effectiveType;
    private List<Long> deptIds = new ArrayList<>();
    private List<String> deptNames = new ArrayList<>();
    private boolean includeSelf;
    private List<RoleScopeRspDTO> roleScopes = new ArrayList<>();
    private String description;

    public String getEffectiveType() {
        return effectiveType;
    }

    public void setEffectiveType(String effectiveType) {
        this.effectiveType = effectiveType;
    }

    public List<Long> getDeptIds() {
        return deptIds;
    }

    public void setDeptIds(List<Long> deptIds) {
        this.deptIds = deptIds;
    }

    public List<String> getDeptNames() {
        return deptNames;
    }

    public void setDeptNames(List<String> deptNames) {
        this.deptNames = deptNames;
    }

    public boolean isIncludeSelf() {
        return includeSelf;
    }

    public void setIncludeSelf(boolean includeSelf) {
        this.includeSelf = includeSelf;
    }

    public List<RoleScopeRspDTO> getRoleScopes() {
        return roleScopes;
    }

    public void setRoleScopes(List<RoleScopeRspDTO> roleScopes) {
        this.roleScopes = roleScopes;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
