package com.demo.system.controller.dto;

public class LoginRspDTO {

    private final Long userId;
    private final Long tenantId;
    private final String username;

    public LoginRspDTO(Long userId, Long tenantId, String username) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.username = username;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getUsername() {
        return username;
    }
}
