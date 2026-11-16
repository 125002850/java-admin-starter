package com.oigit.admin.core.http;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OutboundHttpResponseTests {

    @Test
    void shouldMapContentTooLargeResponse() {
        OutboundHttpResponse response = new OutboundHttpResponse(
                200, new byte[0], true, false, null, null);
        assertThat(response.bodyTooLarge()).isTrue();
        assertThat(response.body()).isEmpty();
    }

    @Test
    void shouldMapUnsupportedContentEncoding() {
        OutboundHttpResponse response = new OutboundHttpResponse(
                200, new byte[0], false, true, null, null);
        assertThat(response.unsupportedContentEncoding()).isTrue();
    }

    @Test
    void shouldMapRetryAfter() {
        OutboundHttpResponse response = new OutboundHttpResponse(
                429, new byte[0], false, false, Duration.ofSeconds(30), null);
        assertThat(response.retryAfter()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void shouldMapFeishuTraceId() {
        OutboundHttpResponse response = new OutboundHttpResponse(
                200, new byte[0], false, false, null,
                Map.of("x-tt-logid", List.of("feishu-trace-abc")));
        assertThat(response.firstHeader("X-Tt-Logid")).isEqualTo("feishu-trace-abc");
    }

    @Test
    void shouldMapNullTraceIdWhenMissing() {
        OutboundHttpResponse response = new OutboundHttpResponse(
                200, new byte[0], false, false, null, null);
        assertThat(response.firstHeader("X-Tt-Logid")).isNull();
    }
}
