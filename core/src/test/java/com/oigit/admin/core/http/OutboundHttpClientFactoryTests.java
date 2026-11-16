package com.oigit.admin.core.http;

import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.net.Proxy;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class OutboundHttpClientFactoryTests {

    @Test
    void shouldDisableRedirectsAndAutomaticConnectionRetry() {
        OutboundHttpClientOptions options = OutboundHttpClientOptions.strict(
                Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3));

        OkHttpClient client = factory().get(options);

        assertThat(client.followRedirects()).isFalse();
        assertThat(client.followSslRedirects()).isFalse();
        assertThat(client.retryOnConnectionFailure()).isFalse();
        assertThat(client.connectTimeoutMillis()).isEqualTo(1000);
        assertThat(client.readTimeoutMillis()).isEqualTo(2000);
        assertThat(client.callTimeoutMillis()).isEqualTo(3000);
    }

    @Test
    void shouldCacheClientsByStrictOptions() {
        OutboundHttpClientFactory factory = factory();

        OkHttpClient first = factory.get(OutboundHttpClientOptions.strict(
                Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3)));
        OkHttpClient same = factory.get(OutboundHttpClientOptions.strict(
                Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3)));
        OkHttpClient different = factory.get(OutboundHttpClientOptions.strict(
                Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(5)));

        assertThat(first).isSameAs(same);
        assertThat(first).isNotSameAs(different);
        assertThat(first.callTimeoutMillis()).isEqualTo(3000);
        assertThat(different.callTimeoutMillis()).isEqualTo(5000);
    }

    @Test
    void shouldUseConfiguredHttpsProxyButAlwaysBypassLoopbackAndNoProxyHosts() {
        OutboundHttpProxyProperties properties = new OutboundHttpProxyProperties();
        properties.setHttpsUrl("http://proxy.example.test:7890");
        properties.setNoProxy(".internal.example.test");
        OkHttpClient client = new OutboundHttpClientFactory(properties).get(
                OutboundHttpClientOptions.strict(
                        Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3)));

        assertThat(client.proxySelector().select(URI.create("https://qyapi.weixin.qq.com")))
                .singleElement()
                .satisfies(proxy -> {
                    assertThat(proxy.type()).isEqualTo(Proxy.Type.HTTP);
                    assertThat(proxy.address().toString()).contains("proxy.example.test", "7890");
                });
        assertThat(client.proxySelector().select(URI.create("https://service.internal.example.test")))
                .containsExactly(Proxy.NO_PROXY);
        assertThat(client.proxySelector().select(URI.create("http://127.0.0.1:8080")))
                .containsExactly(Proxy.NO_PROXY);
    }

    @Test
    void retryingOptionsShouldOnlyEnableConnectionFailureRetry() {
        OkHttpClient client = factory().get(OutboundHttpClientOptions.retrying(
                Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3)));

        assertThat(client.retryOnConnectionFailure()).isTrue();
        assertThat(client.followRedirects()).isFalse();
        assertThat(client.followSslRedirects()).isFalse();
    }

    private OutboundHttpClientFactory factory() {
        return new OutboundHttpClientFactory(new OutboundHttpProxyProperties());
    }
}
