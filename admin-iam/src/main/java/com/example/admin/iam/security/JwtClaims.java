package com.example.admin.iam.security;

import java.time.Instant;

public record JwtClaims(Long staffId, String jwtId, Instant issuedAt, Instant expiresAt) {
}
