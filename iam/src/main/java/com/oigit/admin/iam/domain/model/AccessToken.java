package com.oigit.admin.iam.domain.model;

import java.time.LocalDateTime;

public record AccessToken(String value, String jwtId, LocalDateTime expiresAt) {}
