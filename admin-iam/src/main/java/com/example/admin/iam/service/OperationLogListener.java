package com.example.admin.iam.service;

import com.example.admin.iam.event.OperationLogEvent;
import com.example.admin.iam.infra.entity.IamOperationLogEntity;
import com.example.admin.iam.infra.mapper.IamOperationLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
public class OperationLogListener {

    private static final Logger log = LoggerFactory.getLogger(OperationLogListener.class);

    private final IamOperationLogMapper operationLogMapper;

    public OperationLogListener(IamOperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Async("operationLogTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION, fallbackExecution = true)
    public void onOperationLog(OperationLogEvent event) {
        try {
            IamOperationLogEntity entity = new IamOperationLogEntity();
            entity.setOperatorId(event.operatorId());
            entity.setOperatorUsername(event.operatorUsername());
            entity.setOperatorStaffName(event.operatorStaffName());
            entity.setModule(event.module());
            entity.setAction(event.action());
            entity.setRequestPath(event.requestPath());
            entity.setHttpMethod(event.httpMethod());
            entity.setRequestSummary(event.requestSummary());
            entity.setResponseSummary(event.responseSummary());
            entity.setSuccess(event.success());
            entity.setErrorMessage(event.errorMessage());
            entity.setIp(event.ip());
            entity.setUserAgent(event.userAgent());
            entity.setCostMillis(event.costMillis());
            entity.setOperationTime(event.operationTime());
            entity.setCreateBy(event.operatorId());
            entity.setUpdateBy(event.operatorId());
            entity.setDeleted(0L);
            operationLogMapper.insert(entity);
        } catch (Exception ex) {
            log.warn("failed to persist operation log", ex);
        }
    }
}
