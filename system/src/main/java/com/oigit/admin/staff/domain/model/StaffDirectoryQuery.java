package com.oigit.admin.staff.domain.model;

public record StaffDirectoryQuery(
        String keyword,
        String staffCode,
        String userName,
        String account,
        String mobile,
        String sex
) {
}
