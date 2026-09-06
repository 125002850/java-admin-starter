package com.oigit.admin.iam.infra.security;

import java.time.Instant;

public record JwtClaims(Long staffId, String jwtId, Instant issuedAt, Instant expiresAt) {}
