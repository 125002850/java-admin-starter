package com.oigit.admin.iam.app;

import com.oigit.admin.iam.domain.query.LoginLogQuery;
import com.oigit.admin.iam.domain.query.OperationLogQuery;
import com.oigit.admin.iam.domain.query.RoleQuery;
import com.oigit.admin.iam.domain.query.StaffQuery;
import com.oigit.admin.iam.domain.query.TimeRange;
import com.oigit.admin.iam.dto.req.DateTimeRangeReqDTO;
import com.oigit.admin.iam.dto.req.LoginLogPageReqDTO;
import com.oigit.admin.iam.dto.req.OperationLogPageReqDTO;
import com.oigit.admin.iam.dto.req.RolePageReqDTO;
import com.oigit.admin.iam.dto.req.StaffPageReqDTO;

final class IamQueryMapper {
    private IamQueryMapper() {}

    static StaffQuery toQuery(StaffPageReqDTO dto) {
        StaffQuery query = new StaffQuery();
        query.pageNo = dto.getPageNo();
        query.pageSize = dto.getPageSize();
        query.keyword = dto.getKeyword();
        query.deptId = dto.getDeptId();
        query.deptIds = dto.getDeptIds();
        query.includeDescendants = dto.getIncludeDescendants();
        query.status = dto.getStatus();
        query.statuses = dto.getStatuses();
        query.staffCode = dto.getStaffCode();
        query.username = dto.getUsername();
        query.staffName = dto.getStaffName();
        query.createTimeRange = toRange(dto.getCreateTimeRange());
        return query;
    }

    static RoleQuery toQuery(RolePageReqDTO dto) {
        RoleQuery query = new RoleQuery();
        query.pageNo = dto.getPageNo();
        query.pageSize = dto.getPageSize();
        query.keyword = dto.keyword;
        query.status = dto.status;
        return query;
    }

    static LoginLogQuery toQuery(LoginLogPageReqDTO dto) {
        LoginLogQuery query = new LoginLogQuery();
        query.pageNo = dto.getPageNo();
        query.pageSize = dto.getPageSize();
        query.username = dto.username;
        query.staffName = dto.staffName;
        query.result = dto.result;
        query.ip = dto.ip;
        query.operationTimeRange = toRange(dto.operationTimeRange);
        return query;
    }

    static OperationLogQuery toQuery(OperationLogPageReqDTO dto) {
        OperationLogQuery query = new OperationLogQuery();
        query.pageNo = dto.getPageNo();
        query.pageSize = dto.getPageSize();
        query.operatorId = dto.operatorId;
        query.operatorUsername = dto.operatorUsername;
        query.operatorStaffName = dto.operatorStaffName;
        query.module = dto.module;
        query.action = dto.action;
        query.success = dto.success;
        query.requestPath = dto.requestPath;
        query.operationTimeRange = toRange(dto.operationTimeRange);
        return query;
    }

    private static TimeRange toRange(DateTimeRangeReqDTO dto) {
        return dto == null ? null : new TimeRange(dto.getStartTime(), dto.getEndTime());
    }
}
