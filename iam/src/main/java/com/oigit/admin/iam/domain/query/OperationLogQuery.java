package com.oigit.admin.iam.domain.query;

import com.oigit.admin.iam.enums.OperationLogAction;
import com.oigit.admin.iam.enums.OperationLogModule;

public class OperationLogQuery {
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

    public Long operatorId;

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String operatorUsername;

    public String getOperatorUsername() {
        return operatorUsername;
    }

    public void setOperatorUsername(String operatorUsername) {
        this.operatorUsername = operatorUsername;
    }

    public String operatorStaffName;

    public String getOperatorStaffName() {
        return operatorStaffName;
    }

    public void setOperatorStaffName(String operatorStaffName) {
        this.operatorStaffName = operatorStaffName;
    }

    public OperationLogModule module;

    public OperationLogModule getModule() {
        return module;
    }

    public void setModule(OperationLogModule module) {
        this.module = module;
    }

    public OperationLogAction action;

    public OperationLogAction getAction() {
        return action;
    }

    public void setAction(OperationLogAction action) {
        this.action = action;
    }

    public Boolean success;

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String requestPath;

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public TimeRange operationTimeRange;

    public TimeRange getOperationTimeRange() {
        return operationTimeRange;
    }

    public void setOperationTimeRange(TimeRange operationTimeRange) {
        this.operationTimeRange = operationTimeRange;
    }
}
