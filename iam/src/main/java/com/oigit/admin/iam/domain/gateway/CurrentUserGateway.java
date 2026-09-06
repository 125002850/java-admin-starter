package com.oigit.admin.iam.domain.gateway;

import com.oigit.admin.iam.domain.model.PermissionSnapshot;

import java.util.Optional;

public interface CurrentUserGateway {
    Optional<PermissionSnapshot> current();
}
