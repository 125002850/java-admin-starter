package com.oigit.admin.operationaudit.app;

import com.oigit.admin.core.audit.OperationAuditCommand;
import com.oigit.admin.core.audit.OperationAuditWriter;
import com.oigit.admin.operationaudit.domain.model.OperationAuditLog;
import com.oigit.admin.operationaudit.domain.repository.OperationAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OperationAuditPersistenceWriter implements OperationAuditWriter {

    private final OperationAuditLogRepository repository;

    public OperationAuditPersistenceWriter(OperationAuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(OperationAuditCommand command) {
        repository.create(new OperationAuditLog(
                null,
                command.moduleCode(),
                command.moduleName(),
                command.action(),
                command.description(),
                command.operatorId(),
                operatorName(command),
                command.operatorUsername(),
                command.operatorRealName(),
                command.requestMethod(),
                command.requestPath(),
                command.clientIp(),
                command.traceId(),
                command.requestParams(),
                command.resultStatus(),
                command.httpStatus(),
                command.resultCode(),
                command.errorMessage(),
                command.durationMs(),
                command.operationTime()
        ));
    }

    private String operatorName(OperationAuditCommand command) {
        if (StringUtils.hasText(command.operatorRealName())) {
            return command.operatorRealName();
        }
        if (StringUtils.hasText(command.operatorUsername())) {
            return command.operatorUsername();
        }
        return command.operatorId() == null ? null : String.valueOf(command.operatorId());
    }
}
