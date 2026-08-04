package com.oigit.admin.staff.infra.client;

import com.oigit.appcik.CIClient;
import com.oigit.appcik.core.service.sso.SsoService;
import com.oigit.appcik.core.service.sso.model.StaffInfoPageQueryReq;
import com.oigit.appcik.core.service.sso.model.StaffInfoRsp;
import com.oigit.common.page.PageInfo;
import com.oigit.admin.staff.domain.model.StaffDirectoryQuery;
import com.oigit.admin.staff.domain.model.StaffInfo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CiStaffDirectoryGatewayTests {

    @Test
    void listAll_should_fetch_all_pages_and_hide_cik_models() {
        CIClient ciClient = mock(CIClient.class);
        SsoService ssoService = mock(SsoService.class);
        SsoService.Staff staffClient = mock(SsoService.Staff.class);
        when(ciClient.sso()).thenReturn(ssoService);
        when(ssoService.staff()).thenReturn(staffClient);
        when(staffClient.pageQuery(any(StaffInfoPageQueryReq.class)))
                .thenReturn(PageInfo.returnPage(501L, staffRange(1, 500)))
                .thenReturn(PageInfo.returnPage(501L, List.of(staff("E0501", "name-501"))));

        List<StaffInfo> result = new CiStaffDirectoryGateway(ciClient).listAll(
                new StaffDirectoryQuery(null, null, null, null, null, null)
        );

        assertThat(result).hasSize(501);
        assertThat(result.get(500).staffCode()).isEqualTo("E0501");
        ArgumentCaptor<StaffInfoPageQueryReq> captor = ArgumentCaptor.forClass(StaffInfoPageQueryReq.class);
        verify(staffClient, times(2)).pageQuery(captor.capture());
        assertThat(captor.getAllValues()).extracting(StaffInfoPageQueryReq::getPage).containsExactly(1, 2);
        assertThat(captor.getAllValues()).allSatisfy(request -> {
            assertThat(request.getPageSize()).isEqualTo(500);
            assertThat(request.getStatus()).isEqualTo(1);
            assertThat(request.getStaffStatus()).isEqualTo(0);
        });
    }

    private List<StaffInfoRsp> staffRange(int start, int end) {
        List<StaffInfoRsp> result = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            result.add(staff(String.format("E%04d", i), "name-" + i));
        }
        return result;
    }

    private StaffInfoRsp staff(String staffCode, String userName) {
        StaffInfoRsp response = new StaffInfoRsp();
        response.setStaffCode(staffCode);
        response.setUserName(userName);
        response.setSsoAccountId("sso-" + staffCode);
        response.setAccount("account-" + staffCode);
        response.setMobile("138" + staffCode.substring(1));
        response.setSex("M");
        return response;
    }
}
