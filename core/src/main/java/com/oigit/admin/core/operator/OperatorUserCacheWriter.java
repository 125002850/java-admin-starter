package com.oigit.admin.core.operator;

/**
 * Port used by the gateway adapter to refresh non-authoritative SSO display data.
 * The persistence implementation belongs to the staff infrastructure module.
 */
public interface OperatorUserCacheWriter {

    void upsert(Long userId, String userName, String userPhone, String realName, String userCode);
}
