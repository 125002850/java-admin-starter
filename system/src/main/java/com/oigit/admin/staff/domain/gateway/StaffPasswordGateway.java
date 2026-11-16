package com.oigit.admin.staff.domain.gateway;

/** 仅修改已由应用确定的当前账号，厂商 SDK 留在 Infra。 */
public interface StaffPasswordGateway {
    void change(String account, String oldPassword, String newPassword, String repeatPassword);
}
