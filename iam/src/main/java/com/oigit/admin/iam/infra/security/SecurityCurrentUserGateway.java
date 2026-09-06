package com.oigit.admin.iam.infra.security;

import com.oigit.admin.iam.domain.gateway.CurrentUserGateway;
import com.oigit.admin.iam.domain.model.PermissionSnapshot;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityCurrentUserGateway implements CurrentUserGateway {
    @Override
    public Optional<PermissionSnapshot> current() {
        return CurrentIam.principal().map(IamPrincipal::getSnapshot);
    }
}
