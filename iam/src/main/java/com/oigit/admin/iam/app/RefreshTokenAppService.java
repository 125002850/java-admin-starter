package com.oigit.admin.iam.app;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.iam.domain.gateway.ClientRequestGateway;
import com.oigit.admin.iam.domain.gateway.RefreshTokenCodec;
import com.oigit.admin.iam.domain.model.ClientRequestInfo;
import com.oigit.admin.iam.domain.model.IamRefreshToken;
import com.oigit.admin.iam.domain.repository.RefreshTokenRepository;
import com.oigit.admin.iam.domain.service.RefreshTokenPolicy;
import com.oigit.admin.iam.enums.IamErrorCode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RefreshTokenAppService {
    private final RefreshTokenRepository repository;
    private final AuthenticationOptions authenticationOptions;
    private final ClientRequestGateway clientRequestGateway;
    private final RefreshTokenCodec codec;

    public RefreshTokenAppService(
            RefreshTokenRepository repository,
            AuthenticationOptions authenticationOptions,
            ClientRequestGateway clientRequestGateway,
            RefreshTokenCodec codec) {
        this.repository = repository;
        this.authenticationOptions = authenticationOptions;
        this.clientRequestGateway = clientRequestGateway;
        this.codec = codec;
    }

    @Transactional
    public IssuedRefreshToken issue(Long staffId) {
        ClientRequestInfo request = clientRequestGateway.current();
        LocalDateTime now = LocalDateTime.now();
        String plainToken = codec.generateToken();
        IamRefreshToken token = new IamRefreshToken();
        token.setStaffId(staffId);
        token.setTokenHash(codec.hash(plainToken));
        token.setSessionId(UUID.randomUUID().toString());
        token.setDeviceId(UUID.randomUUID().toString());
        token.setIp(request.ip());
        token.setUserAgent(request.userAgent());
        token.setIssuedTime(now);
        token.setExpireTime(now.plusDays(Math.max(1, authenticationOptions.refreshTokenTtlDays())));
        repository.save(token);
        return new IssuedRefreshToken(plainToken, token);
    }

    @Transactional
    public IamRefreshToken validateForRefresh(String plainToken) {
        if (plainToken == null || plainToken.isBlank()) {
            throw new BizException(IamErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }
        IamRefreshToken token = repository.findByHash(codec.hash(plainToken));
        LocalDateTime now = LocalDateTime.now();
        try {
            RefreshTokenPolicy.validate(token, now);
        } catch (BizException ex) {
            if (ex.getErrorCode() == IamErrorCode.AUTH_REFRESH_TOKEN_EXPIRED) {
                revoke(token, "EXPIRED");
            }
            throw ex;
        }
        token.setLastUsedTime(now);
        repository.save(token);
        return token;
    }

    @Transactional
    public IssuedRefreshToken rotate(IamRefreshToken oldToken) {
        if (!repository.revokeIfActive(oldToken.getId(), LocalDateTime.now(), "ROTATED")) {
            throw new BizException(IamErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }
        return issue(oldToken.getStaffId());
    }

    @Transactional
    public void revokeCurrent(String plainToken, String reason) {
        IamRefreshToken token = findByPlainToken(plainToken);
        if (token != null && token.getRevokedTime() == null) {
            revoke(token, reason);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllByStaffId(Long staffId, String reason) {
        repository.revokeAllByStaffId(staffId, LocalDateTime.now(), reason);
    }

    public IamRefreshToken findByPlainToken(String plainToken) {
        return plainToken == null || plainToken.isBlank()
                ? null
                : repository.findByHash(codec.hash(plainToken));
    }

    private void revoke(IamRefreshToken token, String reason) {
        token.setRevokedTime(LocalDateTime.now());
        token.setRevokeReason(reason);
        repository.save(token);
    }

    public record IssuedRefreshToken(String plainToken, IamRefreshToken token) {}
}
