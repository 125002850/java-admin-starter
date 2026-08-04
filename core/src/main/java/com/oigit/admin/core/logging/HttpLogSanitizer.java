package com.oigit.admin.core.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class HttpLogSanitizer {

    private static final String MASK = "***";

    private final ObjectMapper objectMapper;
    private final Set<String> sensitiveFields;

    HttpLogSanitizer(ObjectMapper objectMapper, Set<String> sensitiveFields) {
        this.objectMapper = objectMapper;
        this.sensitiveFields = new LinkedHashSet<>();
        sensitiveFields.forEach(field -> this.sensitiveFields.add(normalizeFieldName(field)));
    }

    String sanitizeQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return "-";
        }
        StringBuilder sanitized = new StringBuilder();
        String[] pairs = query.split("&", -1);
        for (String pair : pairs) {
            if (!sanitized.isEmpty()) {
                sanitized.append('&');
            }
            int separator = pair.indexOf('=');
            String rawKey = separator < 0 ? pair : pair.substring(0, separator);
            sanitized.append(singleLine(rawKey));
            if (separator >= 0) {
                sanitized.append('=');
                String decodedKey = decode(rawKey);
                sanitized.append(isSensitive(decodedKey) ? MASK : singleLine(pair.substring(separator + 1)));
            }
        }
        return sanitized.toString();
    }

    String sanitizeBody(byte[] body,
                        String contentType,
                        String characterEncoding,
                        boolean enabled,
                        boolean truncated,
                        int maxBytes) {
        if (!enabled) {
            return "<disabled>";
        }
        if (body.length == 0) {
            return "-";
        }
        if (truncated) {
            return "<omitted: body exceeds " + maxBytes + " bytes>";
        }
        MediaType mediaType = parseMediaType(contentType);
        if (mediaType == null) {
            return "<omitted: unknown content-type>";
        }
        Charset bodyCharset = bodyCharset(mediaType, characterEncoding);
        if (isJson(mediaType)) {
            return sanitizeJson(body, bodyCharset);
        }
        if (MediaType.APPLICATION_FORM_URLENCODED.includes(mediaType)) {
            return sanitizeQuery(new String(body, bodyCharset));
        }
        if (MediaType.MULTIPART_FORM_DATA.includes(mediaType)) {
            return "<omitted: multipart>";
        }
        return "<omitted: content-type=" + singleLine(mediaType.toString()) + ">";
    }

    String singleLine(String value) {
        if (value == null) {
            return "-";
        }
        return value.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ').trim();
    }

    private String sanitizeJson(byte[] body, Charset charset) {
        try {
            JsonNode root = objectMapper.readTree(new String(body, charset));
            mask(root);
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            return "<omitted: invalid json>";
        }
    }

    private void mask(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (isSensitive(field.getKey())) {
                    objectNode.put(field.getKey(), MASK);
                } else {
                    mask(field.getValue());
                }
            }
            return;
        }
        if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(this::mask);
        }
    }

    private boolean isSensitive(String fieldName) {
        String normalized = normalizeFieldName(fieldName);
        return sensitiveFields.contains(normalized)
                || normalized.contains("password")
                || normalized.contains("token")
                || normalized.contains("secret")
                || normalized.contains("ticket")
                || normalized.contains("authorization")
                || normalized.contains("cookie")
                || normalized.endsWith("phone")
                || normalized.endsWith("mobile")
                || normalized.endsWith("idcard")
                || normalized.endsWith("bankcard")
                || normalized.endsWith("cardno");
    }

    private String normalizeFieldName(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private MediaType parseMediaType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return null;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean isJson(MediaType mediaType) {
        return MediaType.APPLICATION_JSON.includes(mediaType)
                || "json".equalsIgnoreCase(mediaType.getSubtype())
                || mediaType.getSubtype().toLowerCase(Locale.ROOT).endsWith("+json");
    }

    private Charset bodyCharset(MediaType mediaType, String characterEncoding) {
        if (mediaType.getCharset() != null) {
            return mediaType.getCharset();
        }
        if (isJson(mediaType)) {
            return StandardCharsets.UTF_8;
        }
        if (!StringUtils.hasText(characterEncoding)) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(characterEncoding);
        } catch (IllegalArgumentException exception) {
            return StandardCharsets.UTF_8;
        }
    }

    private String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return value;
        }
    }
}
