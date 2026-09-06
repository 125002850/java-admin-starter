package com.oigit.admin.iam.dto.req;

import com.oigit.admin.iam.enums.IamStatus;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "创建员工请求")
public class StaffCreateReqDTO {
    @NotBlank
    @Schema(description = "用户名", example = "zhangsan", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank
    @Schema(description = "员工工号", example = "E1001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String staffCode;

    @NotBlank
    @Schema(description = "员工姓名", example = "张三", requiredMode = Schema.RequiredMode.REQUIRED)
    private String staffName;

    @NotNull
    @Schema(description = "部门ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long deptId;

    @NotBlank
    @Schema(description = "初始密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "头像文件标识")
    private String avatar;

    @Schema(description = "状态", example = "ENABLED")
    private IamStatus status;

    @Schema(description = "角色ID集合")
    private List<Long> roleIds = new ArrayList<>();

    @Schema(description = "备注")
    private String remark;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
