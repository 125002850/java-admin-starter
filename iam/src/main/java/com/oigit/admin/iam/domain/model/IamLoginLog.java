package com.oigit.admin.iam.domain.model;

import com.oigit.admin.iam.enums.LoginEventType;
import com.oigit.admin.iam.enums.LoginFailureReason;
import com.oigit.admin.iam.enums.LoginResult;

import java.time.LocalDateTime;

/** 登录事件快照；持久化审计字段由 Entity 管理。 */
public record IamLoginLog(
        Long id,
        Long staffId,
        String username,
        LoginEventType eventType,
        LoginResult result,
        LoginFailureReason failureReason,
        String ip,
        String userAgent,
        String tokenId,
        LocalDateTime operationTime) {}
