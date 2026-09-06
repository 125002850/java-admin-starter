package com.oigit.admin.iam.domain.gateway;

import com.oigit.admin.iam.domain.model.AccessToken;

public interface AccessTokenGateway {
    AccessToken issueAccessToken(Long staffId);
}
