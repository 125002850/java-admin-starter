package com.example.admin.iam.infra.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.admin.core.mybatis.BaseEntity;
import java.time.LocalDateTime;

@TableName("sys_refresh_token")
public class IamRefreshTokenEntity extends BaseEntity {

    @TableId
    private Long id;

    @TableField("staff_id")
    private Long staffId;

    @TableField("token_hash")
    private String tokenHash;

    @TableField("session_id")
    private String sessionId;

    @TableField("device_id")
    private String deviceId;

    @TableField("ip")
    private String ip;

    @TableField("user_agent")
    private String userAgent;

    @TableField("issued_time")
    private LocalDateTime issuedTime;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("last_used_time")
    private LocalDateTime lastUsedTime;

    @TableField("revoked_time")
    private LocalDateTime revokedTime;

    @TableField("revoke_reason")
    private String revokeReason;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getIssuedTime() {
        return issuedTime;
    }

    public void setIssuedTime(LocalDateTime issuedTime) {
        this.issuedTime = issuedTime;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    public LocalDateTime getLastUsedTime() {
        return lastUsedTime;
    }

    public void setLastUsedTime(LocalDateTime lastUsedTime) {
        this.lastUsedTime = lastUsedTime;
    }

    public LocalDateTime getRevokedTime() {
        return revokedTime;
    }

    public void setRevokedTime(LocalDateTime revokedTime) {
        this.revokedTime = revokedTime;
    }

    public String getRevokeReason() {
        return revokeReason;
    }

    public void setRevokeReason(String revokeReason) {
        this.revokeReason = revokeReason;
    }
}
