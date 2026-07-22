package com.oigit.admin.iam.service;

import com.oigit.admin.iam.enums.LoginEventType;
import com.oigit.admin.iam.enums.LoginFailureReason;
import com.oigit.admin.iam.enums.LoginResult;
import com.oigit.admin.iam.infra.entity.IamLoginLogEntity;
import com.oigit.admin.iam.infra.mapper.IamLoginLogMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginLogService {

    private final IamLoginLogMapper loginLogMapper;
    private final ClientRequestInfoResolver clientRequestInfoResolver;

    public LoginLogService(
            IamLoginLogMapper loginLogMapper,
            ClientRequestInfoResolver clientRequestInfoResolver
    ) {
        this.loginLogMapper = loginLogMapper;
        this.clientRequestInfoResolver = clientRequestInfoResolver;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            LoginEventType eventType,
            LoginResult result,
            Long staffId,
            String username,
            LoginFailureReason failureReason,
            String tokenId
    ) {
        ClientRequestInfo requestInfo = clientRequestInfoResolver.current();
        IamLoginLogEntity entity = new IamLoginLogEntity();
        entity.setEventType(eventType);
        entity.setResult(result);
        entity.setStaffId(staffId);
        entity.setUsername(username);
        entity.setFailureReason(failureReason == null ? null : failureReason.getCode());
        entity.setTokenId(tokenId);
        entity.setIp(requestInfo.ip());
        entity.setUserAgent(requestInfo.userAgent());
        entity.setOperationTime(LocalDateTime.now());
        entity.setDeleted(0L);
        loginLogMapper.insert(entity);
    }
}
