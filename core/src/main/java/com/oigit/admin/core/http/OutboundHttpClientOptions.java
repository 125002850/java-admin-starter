package com.oigit.admin.core.http;

import java.time.Duration;

public record OutboundHttpClientOptions(
        Duration connectTimeout,
        Duration readTimeout,
        Duration callTimeout,
        boolean followRedirects,
        boolean followSslRedirects,
        boolean retryOnConnectionFailure) {

    public static OutboundHttpClientOptions strict(
            Duration connectTimeout,
            Duration readTimeout,
            Duration callTimeout) {
        return new OutboundHttpClientOptions(
                connectTimeout,
                readTimeout,
                callTimeout,
                false,
                false,
                false);
    }

    /** 仅用于可安全重放的查询或 token 获取请求。 */
    public static OutboundHttpClientOptions retrying(
            Duration connectTimeout,
            Duration readTimeout,
            Duration callTimeout) {
        return new OutboundHttpClientOptions(
                connectTimeout,
                readTimeout,
                callTimeout,
                false,
                false,
                true);
    }
}
