package com.oigit.admin.staff.domain.gateway;

import com.oigit.admin.staff.domain.model.StaffDirectoryQuery;
import com.oigit.admin.staff.domain.model.StaffInfo;

import java.util.List;

/** 公司统一 SSO 员工目录端口。 */
public interface StaffDirectoryGateway {

    List<StaffInfo> listAll(StaffDirectoryQuery query);
}
