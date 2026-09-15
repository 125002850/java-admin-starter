package com.oigit.admin.core.audit;

public final class OperationAuditRequestAttributes {

    public static final String METADATA = OperationAuditRequestAttributes.class.getName() + ".metadata";
    public static final String RESULT_CODE = OperationAuditRequestAttributes.class.getName() + ".resultCode";
    public static final String RESULT_MESSAGE = OperationAuditRequestAttributes.class.getName() + ".resultMessage";

    private OperationAuditRequestAttributes() {
    }
}
