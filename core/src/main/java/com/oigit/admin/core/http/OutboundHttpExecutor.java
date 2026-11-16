package com.oigit.admin.core.http;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OutboundHttpExecutor {

    private final OutboundHttpClientFactory clientFactory;

    public OutboundHttpExecutor(OutboundHttpClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public OutboundHttpResponse execute(
            OutboundHttpClientOptions options,
            Request request,
            int maxResponseBytes) throws IOException {
        try (Response response = clientFactory.get(options).newCall(request).execute()) {
            Map<String, List<String>> headers = normalizeHeaders(response);
            String contentEncoding = response.header("Content-Encoding");
            if (contentEncoding != null && !contentEncoding.isBlank()
                    && !"identity".equalsIgnoreCase(contentEncoding)) {
                return new OutboundHttpResponse(
                        response.code(), new byte[0], false, true, retryAfter(response), headers);
            }
            ResponseBody responseBody = response.body();
            Buffer buffer = new Buffer();
            long totalBytes = 0L;
            if (responseBody != null) {
                while (totalBytes <= maxResponseBytes) {
                    long read = responseBody.source().read(
                            buffer, maxResponseBytes + 1L - totalBytes);
                    if (read == -1L) {
                        break;
                    }
                    totalBytes += read;
                }
            }
            boolean tooLarge = totalBytes > maxResponseBytes;
            byte[] bytes = tooLarge ? new byte[0] : buffer.readByteArray();
            if (tooLarge) {
                buffer.clear();
            }
            return new OutboundHttpResponse(response.code(), bytes, tooLarge, false, retryAfter(response), headers);
        }
    }

    /**
     * 在响应关闭前流式消费响应体。调用方不得让 response.body() 逃逸出 handler。
     */
    public <T> T executeStreaming(
            OutboundHttpClientOptions options,
            Request request,
            OutboundHostResolver resolver,
            OutboundHttpStreamHandler<T> handler) throws IOException {
        try (Response response = clientFactory.get(options, resolver).newCall(request).execute()) {
            Map<String, List<String>> headers = normalizeHeaders(response);
            String contentEncoding = response.header("Content-Encoding");
            boolean unsupportedContentEncoding = contentEncoding != null && !contentEncoding.isBlank()
                    && !"identity".equalsIgnoreCase(contentEncoding);
            ResponseBody responseBody = response.body();
            return handler.handle(new OutboundHttpStreamResponse(
                    response.code(),
                    responseBody == null ? 0L : responseBody.contentLength(),
                    responseBody == null ? null : responseBody.byteStream(),
                    unsupportedContentEncoding,
                    retryAfter(response),
                    headers));
        }
    }

    private Map<String, List<String>> normalizeHeaders(Response response) {
        return response.headers().toMultimap().entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        entry -> entry.getKey().toLowerCase(Locale.ROOT),
                        Map.Entry::getValue,
                        (left, right) -> left));
    }

    private Duration retryAfter(Response response) {
        String value = response.header("Retry-After");
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            long seconds = Long.parseLong(value);
            return seconds > 0 ? Duration.ofSeconds(seconds) : null;
        } catch (NumberFormatException ignored) {
            try {
                Duration duration = Duration.between(ZonedDateTime.now(), ZonedDateTime.parse(value));
                return duration.isNegative() || duration.isZero() ? null : duration;
            } catch (DateTimeParseException ignoredDate) {
                return null;
            }
        }
    }
}
