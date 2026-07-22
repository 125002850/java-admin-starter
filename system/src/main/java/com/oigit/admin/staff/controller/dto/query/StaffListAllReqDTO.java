package com.oigit.admin.staff.controller.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "查询全部员工列表请求")
public class StaffListAllReqDTO {

    @Schema(description = "关键字（模糊匹配工号、姓名、账号、手机号）", example = "张三")
    private String keyword;

    @Schema(description = "员工工号", example = "EMP001")
    private String staffCode;

    @Schema(description = "姓名", example = "张三")
    private String userName;

    @Schema(description = "登录账号", example = "zhangsan")
    private String account;

    @Schema(description = "手机号", example = "13800138000")
    private String mobile;

    @Schema(description = "性别", example = "男")
    private String sex;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getStaffCode() {
        return staffCode;
    }

    public void setStaffCode(String staffCode) {
        this.staffCode = staffCode;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
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

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }
}
