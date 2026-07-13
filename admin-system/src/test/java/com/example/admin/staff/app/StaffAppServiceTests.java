package com.example.admin.staff.app;

import com.oigit.appcik.CIClient;
import com.oigit.appcik.core.service.sso.SsoService;
import com.oigit.appcik.core.service.sso.model.StaffInfoPageQueryReq;
import com.oigit.appcik.core.service.sso.model.StaffInfoRsp;
import com.oigit.common.page.PageInfo;
import com.example.admin.staff.controller.dto.StaffInfoRspDTO;
import com.example.admin.staff.controller.dto.query.StaffListAllReqDTO;
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

class StaffAppServiceTests {

    @Test
    void listAll_should_fetch_all_cik_pages_and_keep_first_duplicate_staff_code() {
        CIClient ciClient = mock(CIClient.class);
        SsoService ssoService = mock(SsoService.class);
        SsoService.Staff staff = mock(SsoService.Staff.class);
        when(ciClient.sso()).thenReturn(ssoService);
        when(ssoService.staff()).thenReturn(staff);

        List<StaffInfoRsp> firstPage = staffRange(1, 500);
        List<StaffInfoRsp> secondPage = List.of(
                staff("E0500", "replacement"),
                staff("E0501", "name-501")
        );
        when(staff.pageQuery(any(StaffInfoPageQueryReq.class)))
                .thenReturn(PageInfo.returnPage(502L, firstPage))
                .thenReturn(PageInfo.returnPage(502L, secondPage));

        StaffAppService service = new StaffAppService(ciClient);
        List<StaffInfoRspDTO> result = service.listAll(new StaffListAllReqDTO());

        assertThat(result).hasSize(501);
        assertThat(result)
                .filteredOn(dto -> "E0500".equals(dto.getStaffCode()))
                .singleElement()
                .extracting(StaffInfoRspDTO::getUserName)
                .isEqualTo("name-500");
        assertThat(result.get(500).getStaffCode()).isEqualTo("E0501");

        ArgumentCaptor<StaffInfoPageQueryReq> captor = ArgumentCaptor.forClass(StaffInfoPageQueryReq.class);
        verify(staff, times(2)).pageQuery(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(StaffInfoPageQueryReq::getPage)
                .containsExactly(1, 2);
        assertThat(captor.getAllValues())
                .allSatisfy(req -> {
                    assertThat(req.getPageSize()).isEqualTo(500);
                    assertThat(req.getStatus()).isEqualTo(1);
                    assertThat(req.getStaffStatus()).isEqualTo(0);
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
        StaffInfoRsp rsp = new StaffInfoRsp();
        rsp.setStaffCode(staffCode);
        rsp.setUserName(userName);
        rsp.setSsoAccountId("sso-" + staffCode);
        rsp.setAccount("account-" + staffCode);
        rsp.setMobile("138" + staffCode.substring(1));
        rsp.setSex("M");
        return rsp;
    }
}
