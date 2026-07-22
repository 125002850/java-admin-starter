package com.oigit.admin.iam.service;

import com.oigit.admin.iam.config.ClientIpProperties;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

class ClientRequestInfoTests {

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void untrustedRemoteAddressShouldIgnoreSpoofedForwardedFor() {
        ClientRequestInfoResolver resolver = new ClientRequestInfoResolver(new ClientIpProperties());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("X-Forwarded-For", "198.51.100.99");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        ClientRequestInfo requestInfo = resolver.current();

        assertThat(requestInfo.ip()).isEqualTo("203.0.113.10");
    }

    @Test
    void trustedProxyShouldResolveClientFromForwardedFor() {
        ClientIpProperties properties = new ClientIpProperties();
        properties.setTrustedProxyCidrs(List.of("10.0.0.0/8"));
        ClientRequestInfoResolver resolver = new ClientRequestInfoResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.8");
        request.addHeader("X-Forwarded-For", "198.51.100.25");

        ClientRequestInfo requestInfo = resolver.resolve(request);

        assertThat(requestInfo.ip()).isEqualTo("198.51.100.25");
    }

    @Test
    void trustedProxyChainShouldStopAtFirstUntrustedAddressFromRight() {
        ClientIpProperties properties = new ClientIpProperties();
        properties.setTrustedProxyCidrs(List.of("10.0.0.0/8", "192.168.0.0/16"));
        ClientRequestInfoResolver resolver = new ClientRequestInfoResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.8");
        request.addHeader("X-Forwarded-For", "198.51.100.99, 203.0.113.25, 192.168.1.7");

        ClientRequestInfo requestInfo = resolver.resolve(request);

        assertThat(requestInfo.ip()).isEqualTo("203.0.113.25");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "unknown",
            "client.example.com",
            "300.1.1.1",
            "11111111111111111111111111111111111111111111111111111111111111111"
    })
    void invalidForwardedAddressShouldFallBackToRemoteAddress(String forwardedAddress) {
        ClientIpProperties properties = new ClientIpProperties();
        properties.setTrustedProxyCidrs(List.of("10.0.0.0/8"));
        ClientRequestInfoResolver resolver = new ClientRequestInfoResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.8");
        request.addHeader("X-Forwarded-For", forwardedAddress);

        ClientRequestInfo requestInfo = resolver.resolve(request);

        assertThat(requestInfo.ip()).isEqualTo("10.0.0.8");
    }

    @Test
    void trustedProxyShouldUseRealIpWhenForwardedForIsAbsent() {
        ClientIpProperties properties = new ClientIpProperties();
        properties.setTrustedProxyCidrs(List.of("10.0.0.0/8"));
        ClientRequestInfoResolver resolver = new ClientRequestInfoResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.8");
        request.addHeader("X-Real-IP", "198.51.100.26");

        ClientRequestInfo requestInfo = resolver.resolve(request);

        assertThat(requestInfo.ip()).isEqualTo("198.51.100.26");
    }

    @Test
    void trustedProxyShouldResolveBracketedIpv6FromForwardedHeader() {
        ClientIpProperties properties = new ClientIpProperties();
        properties.setTrustedProxyCidrs(List.of("10.0.0.0/8"));
        ClientRequestInfoResolver resolver = new ClientRequestInfoResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.8");
        request.addHeader("Forwarded", "for=\"[2001:db8::17]:4711\";proto=https");

        ClientRequestInfo requestInfo = resolver.resolve(request);

        assertThat(requestInfo.ip()).isEqualTo("2001:db8:0:0:0:0:0:17");
    }

    @Test
    void userAgentShouldBeTruncatedToDatabaseColumnLength() {
        ClientRequestInfoResolver resolver = new ClientRequestInfoResolver(new ClientIpProperties());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("User-Agent", "a".repeat(600));

        ClientRequestInfo requestInfo = resolver.resolve(request);

        assertThat(requestInfo.userAgent()).hasSize(512);
    }

    @Test
    void ipv4MappedIpv6ShouldBeNormalizedToIpv4() {
        ClientRequestInfoResolver resolver = new ClientRequestInfoResolver(new ClientIpProperties());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("::ffff:192.0.2.1");

        ClientRequestInfo requestInfo = resolver.resolve(request);

        assertThat(requestInfo.ip()).isEqualTo("192.0.2.1");
    }
}
