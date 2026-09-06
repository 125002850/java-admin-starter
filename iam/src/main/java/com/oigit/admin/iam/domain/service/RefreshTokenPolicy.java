package com.oigit.admin.iam.domain.service;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.iam.domain.model.IamRefreshToken;
import com.oigit.admin.iam.enums.IamErrorCode;

import java.time.LocalDateTime;

public final class RefreshTokenPolicy {
    private RefreshTokenPolicy() {}

    public static void validate(IamRefreshToken token, LocalDateTime now) {
        if (token == null || token.getRevokedTime() != null) {
            throw new BizException(IamErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }
        if (!token.getExpireTime().isAfter(now)) {
            throw new BizException(IamErrorCode.AUTH_REFRESH_TOKEN_EXPIRED);
        }
    }
}
