package com.oigit.admin.staff.infra.client;

import com.oigit.appcik.CIClient;
import com.oigit.appcik.core.service.sso.model.StaffInfoPageQueryReq;
import com.oigit.appcik.core.service.sso.model.StaffInfoRsp;
import com.oigit.common.page.PageInfo;
import com.oigit.admin.staff.domain.gateway.StaffDirectoryGateway;
import com.oigit.admin.staff.domain.model.StaffDirectoryQuery;
import com.oigit.admin.staff.domain.model.StaffInfo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CiStaffDirectoryGateway implements StaffDirectoryGateway {

    private static final int CIK_PAGE_SIZE = 500;

    private final CIClient ciClient;

    public CiStaffDirectoryGateway(CIClient ciClient) {
        this.ciClient = ciClient;
    }

    @Override
    public List<StaffInfo> listAll(StaffDirectoryQuery query) {
        List<StaffInfo> rows = new ArrayList<>();
        long total = -1L;
        int page = 1;

        while (true) {
            PageInfo<StaffInfoRsp> result = ciClient.sso().staff().pageQuery(buildRequest(query, page));
            if (result == null || result.getList() == null || result.getList().isEmpty()) {
                break;
            }
            List<StaffInfoRsp> pageRows = result.getList();
            pageRows.stream().map(this::toDomain).forEach(rows::add);
            if (result.getTotal() > 0) {
                total = result.getTotal();
            }
            if ((total > 0 && rows.size() >= total) || pageRows.size() < CIK_PAGE_SIZE) {
                break;
            }
            page++;
        }
        return rows;
    }

    private StaffInfoPageQueryReq buildRequest(StaffDirectoryQuery query, int page) {
        StaffInfoPageQueryReq request = new StaffInfoPageQueryReq();
        request.setPageSize(CIK_PAGE_SIZE);
        request.setPage(page);
        request.setStatus(1);
        request.setStaffStatus(0);

        if (hasText(query.keyword())) {
            request.setStaffCode(query.keyword());
        } else {
            if (hasText(query.staffCode())) {
                request.setStaffCode(query.staffCode());
            }
            if (hasText(query.userName())) {
                request.setName(query.userName());
            }
            if (hasText(query.account())) {
                request.setAccount(query.account());
            }
        }
        if (hasText(query.mobile())) {
            request.setMobile(query.mobile());
        }
        return request;
    }

    private StaffInfo toDomain(StaffInfoRsp response) {
        return new StaffInfo(
                response.getStaffCode(),
                response.getSsoAccountId(),
                response.getUserName(),
                response.getSex(),
                response.getAccount(),
                response.getMobile()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
