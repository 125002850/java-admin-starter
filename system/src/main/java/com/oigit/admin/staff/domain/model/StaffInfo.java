package com.oigit.admin.staff.domain.model;

public record StaffInfo(
        String staffCode,
        String ssoAccountId,
        String userName,
        String sex,
        String account,
        String mobile
) {
}
