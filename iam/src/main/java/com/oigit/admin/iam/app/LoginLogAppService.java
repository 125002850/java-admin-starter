package com.oigit.admin.iam.app;

import com.oigit.admin.iam.domain.gateway.ClientRequestGateway;
import com.oigit.admin.iam.domain.model.ClientRequestInfo;
import com.oigit.admin.iam.domain.model.IamLoginLog;
import com.oigit.admin.iam.domain.repository.LogRepository;
import com.oigit.admin.iam.enums.LoginEventType;
import com.oigit.admin.iam.enums.LoginFailureReason;
import com.oigit.admin.iam.enums.LoginResult;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LoginLogAppService {

    private final LogRepository logRepository;
    private final ClientRequestGateway clientRequestInfoResolver;

    public LoginLogAppService(
            LogRepository logRepository, ClientRequestGateway clientRequestInfoResolver) {
        this.logRepository = logRepository;
        this.clientRequestInfoResolver = clientRequestInfoResolver;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            LoginEventType eventType,
            LoginResult result,
            Long staffId,
            String username,
            LoginFailureReason failureReason,
            String tokenId) {
        ClientRequestInfo requestInfo = clientRequestInfoResolver.current();
        logRepository.saveLoginLog(
                new IamLoginLog(
                        null,
                        staffId,
                        username,
                        eventType,
                        result,
                        failureReason,
                        requestInfo.ip(),
                        requestInfo.userAgent(),
                        tokenId,
                        LocalDateTime.now()));
    }
}
