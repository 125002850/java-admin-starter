package com.example.admin.iam.service;

import com.example.admin.iam.enums.LoginEventType;
import com.example.admin.iam.enums.LoginResult;
import com.example.admin.iam.infra.entity.IamLoginLogEntity;
import com.example.admin.iam.infra.mapper.IamLoginLogMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginLogService {

    private final IamLoginLogMapper loginLogMapper;

    public LoginLogService(IamLoginLogMapper loginLogMapper) {
        this.loginLogMapper = loginLogMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            LoginEventType eventType,
            LoginResult result,
            Long staffId,
            String username,
            String failureReason,
            String tokenId
    ) {
        ClientRequestInfo requestInfo = ClientRequestInfo.current();
        IamLoginLogEntity entity = new IamLoginLogEntity();
        entity.setEventType(eventType);
        entity.setResult(result);
        entity.setStaffId(staffId);
        entity.setUsername(username);
        entity.setFailureReason(failureReason);
        entity.setTokenId(tokenId);
        entity.setIp(requestInfo.ip());
        entity.setUserAgent(requestInfo.userAgent());
        entity.setOperationTime(LocalDateTime.now());
        entity.setDeleted(0L);
        loginLogMapper.insert(entity);
    }
}
