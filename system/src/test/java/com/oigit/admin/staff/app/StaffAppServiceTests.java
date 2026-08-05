package com.oigit.admin.staff.app;

import com.oigit.admin.staff.domain.gateway.StaffDirectoryGateway;
import com.oigit.admin.staff.domain.model.StaffDirectoryQuery;
import com.oigit.admin.staff.domain.model.StaffInfo;
import com.oigit.admin.staff.dto.req.query.StaffListAllReqDTO;
import com.oigit.admin.staff.dto.rsp.StaffInfoRspDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StaffAppServiceTests {

    @Test
    void listAll_should_use_domain_gateway_deduplicate_and_filter() {
        StaffDirectoryGateway gateway = mock(StaffDirectoryGateway.class);
        StaffDirectoryQuery expectedQuery = new StaffDirectoryQuery("张", null, null, null, null, "M");
        when(gateway.listAll(expectedQuery)).thenReturn(List.of(
                staff("E0001", "张三", "M"),
                staff("E0001", "替换姓名", "M"),
                staff("E0002", "李四", "M")
        ));
        StaffListAllReqDTO request = new StaffListAllReqDTO();
        request.setKeyword("张");
        request.setSex("M");

        List<StaffInfoRspDTO> result = new StaffAppService(gateway).listAll(request);

        assertThat(result).singleElement().satisfies(row -> {
            assertThat(row.getStaffCode()).isEqualTo("E0001");
            assertThat(row.getUserName()).isEqualTo("张三");
        });
        ArgumentCaptor<StaffDirectoryQuery> captor = ArgumentCaptor.forClass(StaffDirectoryQuery.class);
        verify(gateway).listAll(captor.capture());
        assertThat(captor.getValue()).isEqualTo(expectedQuery);
    }

    private StaffInfo staff(String staffCode, String userName, String sex) {
        return new StaffInfo(
                staffCode,
                "sso-" + staffCode,
                userName,
                sex,
                "account-" + staffCode,
                "138" + staffCode.substring(1)
        );
    }
}
