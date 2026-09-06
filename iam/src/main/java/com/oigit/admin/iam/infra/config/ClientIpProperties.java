package com.oigit.admin.iam.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "platform.iam.client-ip")
public class ClientIpProperties {

    private List<String> trustedProxyCidrs = new ArrayList<>();

    public List<String> getTrustedProxyCidrs() {
        return trustedProxyCidrs;
    }

    public void setTrustedProxyCidrs(List<String> trustedProxyCidrs) {
        this.trustedProxyCidrs =
                trustedProxyCidrs == null ? new ArrayList<>() : new ArrayList<>(trustedProxyCidrs);
    }
}
