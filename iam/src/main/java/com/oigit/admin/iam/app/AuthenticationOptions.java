package com.oigit.admin.iam.app;

/** 应用层认证参数，由配置装配层提供。 */
public record AuthenticationOptions(long refreshTokenTtlDays, long failureDelayMillis) {}
