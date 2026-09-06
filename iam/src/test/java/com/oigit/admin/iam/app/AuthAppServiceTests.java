package com.oigit.admin.iam.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.iam.domain.service.IamStaffService;
import com.oigit.admin.iam.dto.req.LoginReqDTO;
import com.oigit.admin.iam.enums.IamErrorCode;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AuthAppServiceTests {

    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 500})
    void failedLoginShouldKeepAuthenticationErrorAndThreadInterrupt(long failureDelayMillis) {
        AuthAppService service =
                new AuthAppService(
                        mock(IamStaffService.class),
                        null,
                        null,
                        null,
                        null,
                        null,
                        mock(LoginLogAppService.class),
                        new AuthenticationOptions(14, failureDelayMillis),
                        null);
        LoginReqDTO request = new LoginReqDTO();
        request.setUsername("missing-user");
        request.setPassword("wrong-password");

        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> service.login(request))
                    .isInstanceOfSatisfying(
                            BizException.class,
                            ex ->
                                    assertThat(ex.getErrorCode())
                                            .isEqualTo(IamErrorCode.AUTH_BAD_CREDENTIALS));
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }
}
