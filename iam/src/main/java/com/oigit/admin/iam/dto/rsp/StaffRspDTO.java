package com.oigit.admin.iam.dto.rsp;

import com.oigit.admin.core.web.AuditRspDTO;
import com.oigit.admin.iam.enums.IamStatus;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "员工响应")
public class StaffRspDTO extends AuditRspDTO {
    private Long staffId;
    private String username;
    private String staffCode;
    private String staffName;
    private Long deptId;
    private String deptName;
    private String phone;
    private String email;
    private String avatar;
    private IamStatus status;
    private boolean mustChangePassword;
    private String remark;
    private List<com.oigit.admin.iam.dto.rsp.RoleSummaryRspDTO> roles = new ArrayList<>();

    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getStaffCode() {
        return staffCode;
    }

    public void setStaffCode(String staffCode) {
        this.staffCode = staffCode;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public IamStatus getStatus() {
        return status;
    }

    public void setStatus(IamStatus status) {
        this.status = status;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public List<com.oigit.admin.iam.dto.rsp.RoleSummaryRspDTO> getRoles() {
        return roles;
    }

    public void setRoles(List<com.oigit.admin.iam.dto.rsp.RoleSummaryRspDTO> roles) {
        this.roles = roles;
    }
}
