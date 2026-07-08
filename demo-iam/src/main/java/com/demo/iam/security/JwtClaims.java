package com.demo.iam.security;

import java.time.Instant;

public record JwtClaims(Long staffId, String jwtId, Instant issuedAt, Instant expiresAt) {
}
