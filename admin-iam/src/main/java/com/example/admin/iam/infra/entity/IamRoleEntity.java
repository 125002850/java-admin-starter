package com.example.admin.iam.infra.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.admin.core.mybatis.BaseEntity;
import com.example.admin.iam.enums.DataScopeType;
import com.example.admin.iam.enums.IamStatus;

@TableName("sys_role")
public class IamRoleEntity extends BaseEntity {

    @TableId
    private Long id;

    @TableField("role_code")
    private String roleCode;

    @TableField("role_name")
    private String roleName;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("status")
    private IamStatus status;

    @TableField("data_scope_type")
    private DataScopeType dataScopeType;

    @TableField("system_builtin")
    private Boolean systemBuiltIn;

    @TableField("remark")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public IamStatus getStatus() {
        return status;
    }

    public void setStatus(IamStatus status) {
        this.status = status;
    }

    public DataScopeType getDataScopeType() {
        return dataScopeType;
    }

    public void setDataScopeType(DataScopeType dataScopeType) {
        this.dataScopeType = dataScopeType;
    }

    public Boolean getSystemBuiltIn() {
        return systemBuiltIn;
    }

    public void setSystemBuiltIn(Boolean systemBuiltIn) {
        this.systemBuiltIn = systemBuiltIn;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
