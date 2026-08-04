package com.oigit.admin.core.logging;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

final class LimitedContentCachingResponseWrapper extends HttpServletResponseWrapper {

    private final int contentLimit;
    private final ByteArrayOutputStream cachedContent;
    private ServletOutputStream outputStream;
    private PrintWriter writer;
    private long contentSize;

    LimitedContentCachingResponseWrapper(HttpServletResponse response, int contentLimit) {
        super(response);
        this.contentLimit = Math.max(1, contentLimit);
        this.cachedContent = new ByteArrayOutputStream(Math.min(this.contentLimit, 1024));
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (writer != null) {
            throw new IllegalStateException("getWriter() has already been called for this response");
        }
        if (outputStream == null) {
            outputStream = new CachingServletOutputStream(super.getOutputStream());
        }
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (outputStream != null) {
            throw new IllegalStateException("getOutputStream() has already been called for this response");
        }
        if (writer == null) {
            PrintWriter delegate = super.getWriter();
            writer = new PrintWriter(new CapturingWriter(delegate, responseCharset()));
        }
        return writer;
    }

    @Override
    public void flushBuffer() throws IOException {
        if (writer != null) {
            writer.flush();
        } else if (outputStream != null) {
            outputStream.flush();
        }
        super.flushBuffer();
    }

    @Override
    public void reset() {
        super.reset();
        resetCapturedContent();
    }

    @Override
    public void resetBuffer() {
        super.resetBuffer();
        resetCapturedContent();
    }

    byte[] getCachedContent() {
        return cachedContent.toByteArray();
    }

    boolean isContentTruncated() {
        return contentSize > contentLimit;
    }

    private void capture(byte[] bytes, int offset, int length) {
        contentSize += length;
        int remaining = contentLimit - cachedContent.size();
        if (remaining > 0) {
            cachedContent.write(bytes, offset, Math.min(remaining, length));
        }
    }

    private void capture(int value) {
        contentSize++;
        if (cachedContent.size() < contentLimit) {
            cachedContent.write(value);
        }
    }

    private void resetCapturedContent() {
        cachedContent.reset();
        contentSize = 0;
    }

    private Charset responseCharset() {
        try {
            return Charset.forName(getCharacterEncoding());
        } catch (IllegalArgumentException exception) {
            return StandardCharsets.UTF_8;
        }
    }

    private final class CachingServletOutputStream extends ServletOutputStream {

        private final ServletOutputStream delegate;

        private CachingServletOutputStream(ServletOutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            delegate.setWriteListener(writeListener);
        }

        @Override
        public void write(int value) throws IOException {
            delegate.write(value);
            capture(value);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            delegate.write(bytes, offset, length);
            capture(bytes, offset, length);
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }

    private final class CapturingWriter extends Writer {

        private final PrintWriter delegate;
        private final Charset charset;

        private CapturingWriter(PrintWriter delegate, Charset charset) {
            this.delegate = delegate;
            this.charset = charset;
        }

        @Override
        public void write(char[] chars, int offset, int length) {
            delegate.write(chars, offset, length);
            byte[] encoded = new String(chars, offset, length).getBytes(charset);
            capture(encoded, 0, encoded.length);
        }

        @Override
        public void flush() {
            delegate.flush();
        }

        @Override
        public void close() {
            delegate.close();
        }
    }
}
