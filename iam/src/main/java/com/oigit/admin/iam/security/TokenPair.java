package com.oigit.admin.iam.security;

import java.time.LocalDateTime;

public record TokenPair(String accessToken, String refreshToken, LocalDateTime accessTokenExpiresAt, String accessTokenId) {
}
