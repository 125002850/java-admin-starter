package com.oigit.admin.iam.domain.gateway;

public interface RefreshTokenCodec {
    String generateToken();

    String hash(String plainToken);
}
