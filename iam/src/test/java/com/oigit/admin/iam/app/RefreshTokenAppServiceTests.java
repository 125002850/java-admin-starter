package com.oigit.admin.iam.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.iam.domain.gateway.RefreshTokenCodec;
import com.oigit.admin.iam.domain.model.ClientRequestInfo;
import com.oigit.admin.iam.domain.model.IamRefreshToken;
import com.oigit.admin.iam.domain.repository.RefreshTokenRepository;
import com.oigit.admin.iam.enums.IamErrorCode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RefreshTokenAppServiceTests {

    @ParameterizedTest
    @CsvSource({"-1, 1", "0, 1", "3, 3"})
    void issuedTokenShouldUseConfiguredTtlWithOneDayMinimum(
            long configuredDays, long expectedDays) {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        RefreshTokenCodec codec = mock(RefreshTokenCodec.class);
        when(codec.generateToken()).thenReturn("plain-refresh-token");
        when(codec.hash("plain-refresh-token")).thenReturn("hashed-refresh-token");
        RefreshTokenAppService service =
                new RefreshTokenAppService(
                        repository,
                        new AuthenticationOptions(configuredDays, 0),
                        () -> new ClientRequestInfo("127.0.0.1", "JUnit"),
                        codec);

        var issued = service.issue(1L);

        assertThat(issued.token().getExpireTime())
                .isEqualTo(issued.token().getIssuedTime().plusDays(expectedDays));
        assertThat(issued.plainToken()).isEqualTo("plain-refresh-token");
        assertThat(issued.token().getTokenHash()).isEqualTo("hashed-refresh-token");
        verify(repository).save(issued.token());
    }

    @Test
    void aConcurrentRotationLoserMustNotIssueAnotherRefreshToken() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        RefreshTokenCodec codec = mock(RefreshTokenCodec.class);
        RefreshTokenAppService service = new RefreshTokenAppService(repository, null, null, codec);
        IamRefreshToken oldToken = new IamRefreshToken();
        oldToken.setId(11L);
        oldToken.setStaffId(1L);
        when(repository.revokeIfActive(eq(11L), any(), eq("ROTATED"))).thenReturn(false);

        assertThatThrownBy(() -> service.rotate(oldToken))
                .isInstanceOfSatisfying(
                        BizException.class,
                        ex ->
                                assertThat(ex.getErrorCode())
                                        .isEqualTo(IamErrorCode.AUTH_REFRESH_TOKEN_INVALID));
        verify(codec, never()).generateToken();
        verify(repository, never()).save(any());
    }
}
