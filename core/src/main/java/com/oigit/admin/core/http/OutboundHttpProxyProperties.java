package com.oigit.admin.core.http;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 统一出站 HTTP 代理配置。代理 URL 可能包含敏感信息，禁止记录完整配置对象。 */
@Component
@ConfigurationProperties(prefix = "platform.http.proxy")
public class OutboundHttpProxyProperties {

    private String httpUrl;
    private String httpsUrl;
    private String noProxy;

    public String getHttpUrl() {
        return httpUrl;
    }

    public void setHttpUrl(String httpUrl) {
        this.httpUrl = httpUrl;
    }

    public String getHttpsUrl() {
        return httpsUrl;
    }

    public void setHttpsUrl(String httpsUrl) {
        this.httpsUrl = httpsUrl;
    }

    public String getNoProxy() {
        return noProxy;
    }

    public void setNoProxy(String noProxy) {
        this.noProxy = noProxy;
    }

    @Override
    public String toString() {
        return "OutboundHttpProxyProperties[httpUrl=<redacted>, httpsUrl=<redacted>, noProxy=<redacted>]";
    }
}
