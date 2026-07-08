package com.demo.iam.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.demo.core.exception.BizException;
import com.demo.iam.config.IamProperties;
import com.demo.iam.enums.IamErrorCode;
import com.demo.iam.infra.entity.IamRefreshTokenEntity;
import com.demo.iam.infra.mapper.IamRefreshTokenMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final IamRefreshTokenMapper refreshTokenMapper;
    private final IamProperties iamProperties;

    public RefreshTokenService(IamRefreshTokenMapper refreshTokenMapper, IamProperties iamProperties) {
        this.refreshTokenMapper = refreshTokenMapper;
        this.iamProperties = iamProperties;
    }

    @Transactional
    public IssuedRefreshToken issue(Long staffId) {
        ClientRequestInfo requestInfo = ClientRequestInfo.current();
        LocalDateTime now = LocalDateTime.now();
        String token = generateToken();
        IamRefreshTokenEntity entity = new IamRefreshTokenEntity();
        entity.setStaffId(staffId);
        entity.setTokenHash(hash(token));
        entity.setSessionId(UUID.randomUUID().toString());
        entity.setDeviceId(UUID.randomUUID().toString());
        entity.setIp(requestInfo.ip());
        entity.setUserAgent(requestInfo.userAgent());
        entity.setIssuedTime(now);
        entity.setExpireTime(now.plusDays(Math.max(1, iamProperties.getRefreshTokenTtlDays())));
        entity.setDeleted(0L);
        refreshTokenMapper.insert(entity);
        return new IssuedRefreshToken(token, entity);
    }

    @Transactional
    public IamRefreshTokenEntity validateForRefresh(String plainToken) {
        IamRefreshTokenEntity entity = findByPlainToken(plainToken);
        LocalDateTime now = LocalDateTime.now();
        if (entity == null || entity.getRevokedTime() != null) {
            throw new BizException(IamErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }
        if (!entity.getExpireTime().isAfter(now)) {
            revoke(entity, "EXPIRED");
            throw new BizException(IamErrorCode.AUTH_REFRESH_TOKEN_EXPIRED);
        }
        entity.setLastUsedTime(now);
        refreshTokenMapper.updateById(entity);
        return entity;
    }

    @Transactional
    public IssuedRefreshToken rotate(IamRefreshTokenEntity oldToken) {
        revoke(oldToken, "ROTATED");
        return issue(oldToken.getStaffId());
    }

    @Transactional
    public void revokeCurrent(String plainToken, String reason) {
        if (!StringUtils.hasText(plainToken)) {
            return;
        }
        IamRefreshTokenEntity entity = findByPlainToken(plainToken);
        if (entity != null && entity.getRevokedTime() == null) {
            revoke(entity, reason);
        }
    }

    @Transactional
    public void revokeAllByStaffId(Long staffId, String reason) {
        LambdaUpdateWrapper<IamRefreshTokenEntity> update = Wrappers.<IamRefreshTokenEntity>lambdaUpdate()
                .eq(IamRefreshTokenEntity::getStaffId, staffId)
                .isNull(IamRefreshTokenEntity::getRevokedTime)
                .set(IamRefreshTokenEntity::getRevokedTime, LocalDateTime.now())
                .set(IamRefreshTokenEntity::getRevokeReason, reason);
        refreshTokenMapper.update(update);
    }

    public IamRefreshTokenEntity findByPlainToken(String plainToken) {
        if (!StringUtils.hasText(plainToken)) {
            return null;
        }
        return refreshTokenMapper.selectOne(
                Wrappers.<IamRefreshTokenEntity>lambdaQuery()
                        .eq(IamRefreshTokenEntity::getTokenHash, hash(plainToken))
                        .last("limit 1")
        );
    }

    private void revoke(IamRefreshTokenEntity entity, String reason) {
        entity.setRevokedTime(LocalDateTime.now());
        entity.setRevokeReason(reason);
        refreshTokenMapper.updateById(entity);
    }

    private String generateToken() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("failed to hash refresh token", ex);
        }
    }

    public record IssuedRefreshToken(String plainToken, IamRefreshTokenEntity entity) {
    }
}
