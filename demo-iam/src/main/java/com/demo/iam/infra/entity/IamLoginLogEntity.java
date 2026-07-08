package com.demo.iam.infra.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.demo.core.mybatis.BaseEntity;
import com.demo.iam.enums.LoginEventType;
import com.demo.iam.enums.LoginResult;
import java.time.LocalDateTime;

@TableName("sys_login_log")
public class IamLoginLogEntity extends BaseEntity {

    @TableId
    private Long id;

    @TableField("staff_id")
    private Long staffId;

    @TableField("username")
    private String username;

    @TableField("event_type")
    private LoginEventType eventType;

    @TableField("result")
    private LoginResult result;

    @TableField("failure_reason")
    private String failureReason;

    @TableField("ip")
    private String ip;

    @TableField("user_agent")
    private String userAgent;

    @TableField("token_id")
    private String tokenId;

    @TableField("operation_time")
    private LocalDateTime operationTime;

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LoginEventType getEventType() {
        return eventType;
    }

    public void setEventType(LoginEventType eventType) {
        this.eventType = eventType;
    }

    public LoginResult getResult() {
        return result;
    }

    public void setResult(LoginResult result) {
        this.result = result;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
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

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public LocalDateTime getOperationTime() {
        return operationTime;
    }

    public void setOperationTime(LocalDateTime operationTime) {
        this.operationTime = operationTime;
    }
}
