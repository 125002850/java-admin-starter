package com.oigit.admin.iam.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentIam {

    private CurrentIam() {
    }

    public static Optional<IamPrincipal> principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof IamPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    public static Long staffIdOrNull() {
        return principal().map(IamPrincipal::getStaffId).orElse(null);
    }
}
