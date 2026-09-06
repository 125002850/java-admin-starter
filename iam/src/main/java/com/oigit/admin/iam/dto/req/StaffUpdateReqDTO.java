package com.oigit.admin.iam.dto.req;

import com.oigit.admin.iam.enums.IamStatus;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "更新员工请求")
public class StaffUpdateReqDTO {
    @NotNull
    @Schema(description = "员工ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long staffId;

    @NotBlank
    @Schema(description = "员工工号", example = "E1001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String staffCode;

    @NotBlank
    @Schema(description = "员工姓名", example = "张三", requiredMode = Schema.RequiredMode.REQUIRED)
    private String staffName;

    @NotNull
    @Schema(description = "部门ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long deptId;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "头像文件标识")
    private String avatar;

    @Schema(description = "状态", example = "ENABLED")
    private IamStatus status;

    @Schema(description = "备注")
    private String remark;

    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
