package com.demo.iam.service;

import com.demo.iam.enums.LoginEventType;
import com.demo.iam.enums.LoginResult;
import com.demo.iam.infra.entity.IamLoginLogEntity;
import com.demo.iam.infra.mapper.IamLoginLogMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class LoginLogService {

    private final IamLoginLogMapper loginLogMapper;

    public LoginLogService(IamLoginLogMapper loginLogMapper) {
        this.loginLogMapper = loginLogMapper;
    }

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
