package com.example.admin.core.operator;

public final class OperatorContext {

    private static final ThreadLocal<Long> OPERATOR_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> OPERATOR_NAME_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> OPERATOR_PHONE_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> OPERATOR_REAL_NAME_HOLDER = new ThreadLocal<>();

    private OperatorContext() {
    }

    public static void set(Long operatorId, String operatorName, String operatorPhone) {
        set(operatorId, operatorName, operatorPhone, null);
    }

    public static void set(Long operatorId, String operatorName, String operatorPhone, String operatorRealName) {
        OPERATOR_ID_HOLDER.set(operatorId);
        if (operatorName != null) {
            OPERATOR_NAME_HOLDER.set(operatorName);
        }
        if (operatorPhone != null) {
            OPERATOR_PHONE_HOLDER.set(operatorPhone);
        }
        if (operatorRealName != null) {
            OPERATOR_REAL_NAME_HOLDER.set(operatorRealName);
        }
    }

    public static Long getOperatorId() {
        return OPERATOR_ID_HOLDER.get();
    }

    public static String getOperatorName() {
        return OPERATOR_NAME_HOLDER.get();
    }

    public static String getOperatorPhone() {
        return OPERATOR_PHONE_HOLDER.get();
    }

    public static String getOperatorRealName() {
        return OPERATOR_REAL_NAME_HOLDER.get();
    }

    public static void clear() {
        OPERATOR_ID_HOLDER.remove();
        OPERATOR_NAME_HOLDER.remove();
        OPERATOR_PHONE_HOLDER.remove();
        OPERATOR_REAL_NAME_HOLDER.remove();
    }
}
