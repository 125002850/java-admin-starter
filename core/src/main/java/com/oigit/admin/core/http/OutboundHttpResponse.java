package com.oigit.admin.core.http;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record OutboundHttpResponse(
        int statusCode,
        byte[] body,
        boolean bodyTooLarge,
        boolean unsupportedContentEncoding,
        Duration retryAfter,
        Map<String, List<String>> headers) {

    public OutboundHttpResponse {
        body = body == null ? new byte[0] : body;
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    public String firstHeader(String name) {
        if (name == null) {
            return null;
        }
        List<String> values = headers.get(name.toLowerCase(Locale.ROOT));
        if (values == null || values.isEmpty()) {
            return null;
        }
        String value = values.get(0);
        return value == null || value.isBlank() ? null : value;
    }
}
