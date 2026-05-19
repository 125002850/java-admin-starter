package com.demo.core.operator;

public final class OperatorContext {

    private static final ThreadLocal<Long> OPERATOR_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> OPERATOR_NAME_HOLDER = new ThreadLocal<>();

    private OperatorContext() {
    }

    public static void set(Long operatorId, String operatorName) {
        OPERATOR_ID_HOLDER.set(operatorId);
        if (operatorName != null) {
            OPERATOR_NAME_HOLDER.set(operatorName);
        }
    }

    public static Long getOperatorId() {
        return OPERATOR_ID_HOLDER.get();
    }

    public static String getOperatorName() {
        return OPERATOR_NAME_HOLDER.get();
    }

    public static void clear() {
        OPERATOR_ID_HOLDER.remove();
        OPERATOR_NAME_HOLDER.remove();
    }
}
