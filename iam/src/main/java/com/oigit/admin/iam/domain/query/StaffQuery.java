package com.oigit.admin.iam.domain.query;

import com.oigit.admin.iam.enums.IamStatus;

import java.util.List;

public class StaffQuery {
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

    public String keyword;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Long deptId;

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public List<Long> deptIds;

    public List<Long> getDeptIds() {
        return deptIds;
    }

    public void setDeptIds(List<Long> deptIds) {
        this.deptIds = deptIds;
    }

    public Boolean includeDescendants;

    public Boolean getIncludeDescendants() {
        return includeDescendants;
    }

    public void setIncludeDescendants(Boolean includeDescendants) {
        this.includeDescendants = includeDescendants;
    }

    public IamStatus status;

    public IamStatus getStatus() {
        return status;
    }

    public void setStatus(IamStatus status) {
        this.status = status;
    }

    public List<IamStatus> statuses;

    public List<IamStatus> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<IamStatus> statuses) {
        this.statuses = statuses;
    }

    public String staffCode;

    public String getStaffCode() {
        return staffCode;
    }

    public void setStaffCode(String staffCode) {
        this.staffCode = staffCode;
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

    public TimeRange createTimeRange;

    public TimeRange getCreateTimeRange() {
        return createTimeRange;
    }

    public void setCreateTimeRange(TimeRange createTimeRange) {
        this.createTimeRange = createTimeRange;
    }
}
