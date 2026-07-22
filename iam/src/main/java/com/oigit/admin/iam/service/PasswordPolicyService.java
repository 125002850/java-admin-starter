package com.oigit.admin.iam.service;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.iam.enums.IamErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PasswordPolicyService {

    public void validate(String password) {
        if (!StringUtils.hasText(password)
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
