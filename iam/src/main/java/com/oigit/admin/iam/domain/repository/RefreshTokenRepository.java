package com.oigit.admin.iam.domain.repository;

import com.oigit.admin.iam.domain.model.IamRefreshToken;

import java.time.LocalDateTime;

public interface RefreshTokenRepository {
    IamRefreshToken findByHash(String tokenHash);

    void save(IamRefreshToken token);

    boolean revokeIfActive(Long tokenId, LocalDateTime revokedTime, String reason);

    void revokeAllByStaffId(Long staffId, LocalDateTime revokedTime, String reason);
}
