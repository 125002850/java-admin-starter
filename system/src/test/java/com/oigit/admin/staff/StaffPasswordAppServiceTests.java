package com.oigit.admin.staff;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.operator.OperatorContext;
import com.oigit.admin.staff.app.StaffPasswordAppService;
import com.oigit.admin.staff.domain.gateway.StaffPasswordGateway;
import com.oigit.admin.staff.dto.req.StaffPasswordChangeReqDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class StaffPasswordAppServiceTests {
    private final StaffPasswordGateway gateway = mock(StaffPasswordGateway.class);
    private final StaffPasswordAppService service = new StaffPasswordAppService(gateway);
    private final StaffPasswordChangeReqDTO request = new StaffPasswordChangeReqDTO("old", "new", "new");
    @AfterEach void cleanup() { OperatorContext.clear(); }

    @Test void usesOnlyTheCurrentGatewayAccountAndRequiresAnIdentity() {
        assertThatThrownBy(() -> service.change(request)).isInstanceOf(BizException.class);
        OperatorContext.set(1L, "display-name", null);
        assertThatThrownBy(() -> service.change(request)).isInstanceOf(BizException.class);
        verifyNoInteractions(gateway);
        OperatorContext.set(1L, "display-name", null, null, "STAFF-001");
        service.change(request);
        verify(gateway).change("STAFF-001", "old", "new", "new");
    }

    @Test void rejectsMismatchAndUnchangedPasswordsBeforeCallingTheGateway() {
        OperatorContext.set(1L, "display", null, null, "STAFF-001");
        assertThatThrownBy(() -> service.change(new StaffPasswordChangeReqDTO("old", "new", "other"))).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> service.change(new StaffPasswordChangeReqDTO("old", "old", "old"))).isInstanceOf(BizException.class);
        verifyNoInteractions(gateway);
    }
}
