package com.demo.staff.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "员工信息响应")
public class StaffInfoRspDTO {

    @Schema(description = "员工工号", example = "EMP001")
    private String staffCode;

    @Schema(description = "SSO账号ID", example = "1001")
    private String ssoAccountId;

    @Schema(description = "姓名", example = "张三")
    private String userName;

    @Schema(description = "性别", example = "男")
    private String sex;

    @Schema(description = "登录账号", example = "zhangsan")
    private String account;

    @Schema(description = "手机号", example = "13800138000")
    private String mobile;

    public String getStaffCode() {
        return staffCode;
    }

    public void setStaffCode(String staffCode) {
        this.staffCode = staffCode;
    }

    public String getSsoAccountId() {
        return ssoAccountId;
    }

    public void setSsoAccountId(String ssoAccountId) {
        this.ssoAccountId = ssoAccountId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }
}
