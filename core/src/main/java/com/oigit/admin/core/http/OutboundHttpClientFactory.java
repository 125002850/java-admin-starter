package com.oigit.admin.core.http;

import okhttp3.OkHttpClient;
import okhttp3.Dns;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.net.ProxySelector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class OutboundHttpClientFactory {

    private final Map<OutboundHttpClientOptions, OkHttpClient> clients = new ConcurrentHashMap<>();
    private final ProxySelector proxySelector;

    public OutboundHttpClientFactory(OutboundHttpProxyProperties proxyProperties) {
        this.proxySelector = new ConfiguredOutboundProxySelector(proxyProperties, ProxySelector.getDefault());
    }

    OkHttpClient get(OutboundHttpClientOptions options) {
        return clients.computeIfAbsent(options, this::build);
    }

    OkHttpClient get(OutboundHttpClientOptions options, OutboundHostResolver resolver) {
        if (resolver == null) {
            return get(options);
        }
        Dns dns = resolver::lookup;
        return get(options).newBuilder().dns(dns).build();
    }

    private OkHttpClient build(OutboundHttpClientOptions options) {
        return new OkHttpClient.Builder()
                .connectTimeout(options.connectTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(options.readTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .writeTimeout(options.readTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .callTimeout(options.callTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .followRedirects(options.followRedirects())
                .followSslRedirects(options.followSslRedirects())
                .retryOnConnectionFailure(options.retryOnConnectionFailure())
                .proxySelector(proxySelector)
                .build();
    }
}
