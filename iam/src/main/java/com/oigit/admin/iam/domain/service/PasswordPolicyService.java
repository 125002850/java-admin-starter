package com.oigit.admin.iam.domain.service;

import static com.oigit.admin.iam.domain.service.IamDomainRules.hasText;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.iam.enums.IamErrorCode;

public class PasswordPolicyService {

    public void validate(String password) {
        if (!hasText(password)
                || password.length() < 8
                || password.length() > 32
                || password.chars().noneMatch(Character::isUpperCase)
                || password.chars().noneMatch(Character::isLowerCase)
                || password.chars().noneMatch(Character::isDigit)
                || password.chars().noneMatch(ch -> !Character.isLetterOrDigit(ch))) {
            throw new BizException(IamErrorCode.AUTH_PASSWORD_POLICY_INVALID);
        }
    }
}
