package com.oigit.admin.operationaudit.domain.model;

import java.util.List;
import java.util.Objects;

public record OperationAuditLogPage(List<OperationAuditLog> records, long total) {

    public OperationAuditLogPage {
        records = List.copyOf(Objects.requireNonNull(records, "records must not be null"));
    }
}
