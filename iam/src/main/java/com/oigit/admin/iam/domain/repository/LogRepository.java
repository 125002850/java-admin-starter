package com.oigit.admin.iam.domain.repository;

import com.oigit.admin.iam.domain.model.IamLoginLog;
import com.oigit.admin.iam.domain.model.IamOperationLog;
import com.oigit.admin.iam.domain.model.PageSlice;
import com.oigit.admin.iam.domain.query.LoginLogQuery;
import com.oigit.admin.iam.domain.query.OperationLogQuery;

public interface LogRepository {
    PageSlice<IamLoginLog> pageLoginLogs(LoginLogQuery query);

    IamLoginLog findLoginLog(Long logId);

    PageSlice<IamOperationLog> pageOperationLogs(OperationLogQuery query);

    IamOperationLog findOperationLog(Long logId);

    void saveLoginLog(IamLoginLog log);
}
