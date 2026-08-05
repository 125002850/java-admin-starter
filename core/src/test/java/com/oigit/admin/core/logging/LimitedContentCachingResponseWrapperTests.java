package com.oigit.admin.core.logging;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class LimitedContentCachingResponseWrapperTests {

    @Test
    void shouldCaptureOnlyConfiguredBytesWithoutHoldingBackResponse() throws Exception {
        MockHttpServletResponse delegate = new MockHttpServletResponse();
        LimitedContentCachingResponseWrapper response = new LimitedContentCachingResponseWrapper(delegate, 5);

        response.getOutputStream().write("123456789".getBytes(StandardCharsets.UTF_8));
        response.flushBuffer();

        assertThat(delegate.getContentAsString()).isEqualTo("123456789");
        assertThat(new String(response.getCachedContent(), StandardCharsets.UTF_8)).isEqualTo("12345");
        assertThat(response.isContentTruncated()).isTrue();
    }

    @Test
    void shouldResetCapturedOutputStreamContent() throws Exception {
        MockHttpServletResponse delegate = new MockHttpServletResponse();
        LimitedContentCachingResponseWrapper response = new LimitedContentCachingResponseWrapper(delegate, 32);

        response.getOutputStream().write("first".getBytes(StandardCharsets.UTF_8));
        response.resetBuffer();
        response.getOutputStream().write("second".getBytes(StandardCharsets.UTF_8));
        response.flushBuffer();

        assertThat(delegate.getContentAsString()).isEqualTo("second");
        assertThat(new String(response.getCachedContent(), StandardCharsets.UTF_8)).isEqualTo("second");
        assertThat(response.isContentTruncated()).isFalse();
    }

    @Test
    void shouldCaptureWriterOutput() throws Exception {
        MockHttpServletResponse delegate = new MockHttpServletResponse();
        delegate.setCharacterEncoding("UTF-8");
        LimitedContentCachingResponseWrapper response = new LimitedContentCachingResponseWrapper(delegate, 32);

        response.getWriter().write("中文响应");
        response.flushBuffer();

        assertThat(delegate.getContentAsString()).isEqualTo("中文响应");
        assertThat(new String(response.getCachedContent(), StandardCharsets.UTF_8)).isEqualTo("中文响应");
    }
}
