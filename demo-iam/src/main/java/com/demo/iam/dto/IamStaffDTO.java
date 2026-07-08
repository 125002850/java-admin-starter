package com.demo.iam.dto;

import com.demo.core.web.PageReqDTO;
import com.demo.iam.dto.IamCommonDTO.DateTimeRangeReqDTO;
import com.demo.iam.enums.IamStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class IamStaffDTO {

    private IamStaffDTO() {
    }

    @Schema(description = "员工分页请求")
    public static class StaffPageReqDTO extends PageReqDTO {
        @Schema(description = "关键字，匹配用户名、工号、姓名、手机号", example = "admin")
        private String keyword;

        @Schema(description = "部门ID", example = "1")
        private Long deptId;

        @Schema(description = "员工状态", example = "ENABLED")
        private IamStatus status;

        @Schema(description = "员工工号，模糊匹配", example = "E1001")
        private String staffCode;

        @Schema(description = "用户名，模糊匹配", example = "zhangsan")
        private String username;

        @Schema(description = "员工姓名，模糊匹配", example = "张三")
        private String staffName;

        @Schema(description = "创建时间范围")
        private DateTimeRangeReqDTO createTimeRange;

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }

        public Long getDeptId() {
            return deptId;
        }

        public void setDeptId(Long deptId) {
            this.deptId = deptId;
        }

        public IamStatus getStatus() {
            return status;
        }

        public void setStatus(IamStatus status) {
            this.status = status;
        }

        public String getStaffCode() {
            return staffCode;
        }

        public void setStaffCode(String staffCode) {
            this.staffCode = staffCode;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getStaffName() {
            return staffName;
        }

        public void setStaffName(String staffName) {
            this.staffName = staffName;
        }

        public DateTimeRangeReqDTO getCreateTimeRange() {
            return createTimeRange;
        }

        public void setCreateTimeRange(DateTimeRangeReqDTO createTimeRange) {
            this.createTimeRange = createTimeRange;
        }
    }

    @Schema(description = "员工ID请求")
    public static class StaffIdReqDTO {
        @NotNull
        @Schema(description = "员工ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long staffId;

        public Long getStaffId() {
            return staffId;
        }

        public void setStaffId(Long staffId) {
            this.staffId = staffId;
        }
    }

    @Schema(description = "创建员工请求")
    public static class StaffCreateReqDTO {
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

    @Schema(description = "更新员工请求")
    public static class StaffUpdateReqDTO {
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

    @Schema(description = "员工状态更新请求")
    public static class StaffStatusUpdateReqDTO extends StaffIdReqDTO {
        @NotNull
        @Schema(description = "状态", example = "DISABLED", requiredMode = Schema.RequiredMode.REQUIRED)
        private IamStatus status;

        public IamStatus getStatus() {
            return status;
        }

        public void setStatus(IamStatus status) {
            this.status = status;
        }
    }

    @Schema(description = "员工密码重置请求")
    public static class StaffPasswordResetReqDTO extends StaffIdReqDTO {
        @NotBlank
        @Schema(description = "新密码", requiredMode = Schema.RequiredMode.REQUIRED)
        private String newPassword;

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }

    @Schema(description = "员工角色分配请求")
    public static class StaffRolesAssignReqDTO extends StaffIdReqDTO {
        @NotNull
        @Schema(description = "角色ID集合", requiredMode = Schema.RequiredMode.REQUIRED)
        private List<Long> roleIds = new ArrayList<>();

        public List<Long> getRoleIds() {
            return roleIds;
        }

        public void setRoleIds(List<Long> roleIds) {
            this.roleIds = roleIds;
        }
    }

    @Schema(description = "员工响应")
    public static class StaffRspDTO {
        private Long staffId;
        private String username;
        private String staffCode;
        private String staffName;
        private Long deptId;
        private String deptName;
        private String phone;
        private String email;
        private String avatar;
        private String status;
        private boolean mustChangePassword;
        private String remark;
        private List<IamAuthDTO.RoleSummaryRspDTO> roles = new ArrayList<>();
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
        private Long createBy;
        private Long updateBy;

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

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
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

        public List<IamAuthDTO.RoleSummaryRspDTO> getRoles() {
            return roles;
        }

        public void setRoles(List<IamAuthDTO.RoleSummaryRspDTO> roles) {
            this.roles = roles;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public void setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
        }

        public LocalDateTime getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(LocalDateTime updateTime) {
            this.updateTime = updateTime;
        }

        public Long getCreateBy() {
            return createBy;
        }

        public void setCreateBy(Long createBy) {
            this.createBy = createBy;
        }

        public Long getUpdateBy() {
            return updateBy;
        }

        public void setUpdateBy(Long updateBy) {
            this.updateBy = updateBy;
        }
    }
}
