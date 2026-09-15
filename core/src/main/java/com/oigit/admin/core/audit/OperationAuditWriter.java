package com.oigit.admin.core.audit;

public interface OperationAuditWriter {

    void write(OperationAuditCommand command);
}
