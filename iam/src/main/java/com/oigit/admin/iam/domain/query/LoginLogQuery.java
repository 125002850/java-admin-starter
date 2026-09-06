package com.oigit.admin.iam.domain.query;

import com.oigit.admin.iam.enums.LoginResult;

public class LoginLogQuery {
    public long pageNo;

    public long getPageNo() {
        return pageNo;
    }

    public void setPageNo(long pageNo) {
        this.pageNo = pageNo;
    }

    public long pageSize;

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }

    public String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String staffName;

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public LoginResult result;

    public LoginResult getResult() {
        return result;
    }

    public void setResult(LoginResult result) {
        this.result = result;
    }

    public String ip;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public TimeRange operationTimeRange;

    public TimeRange getOperationTimeRange() {
        return operationTimeRange;
    }

    public void setOperationTimeRange(TimeRange operationTimeRange) {
        this.operationTimeRange = operationTimeRange;
    }
}
