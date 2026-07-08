package com.demo.iam.security;

import com.demo.iam.config.IamProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final IamProperties iamProperties;
    private final ObjectMapper objectMapper;

    public JwtService(IamProperties iamProperties, ObjectMapper objectMapper) {
        this.iamProperties = iamProperties;
        this.objectMapper = objectMapper;
    }

    public AccessToken issueAccessToken(Long staffId) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(Math.max(1, iamProperties.getAccessTokenTtlMinutes()) * 60);
        String jwtId = UUID.randomUUID().toString();

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", String.valueOf(staffId));
        payload.put("jti", jwtId);
        payload.put("iat", issuedAt.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        String signingInput = encodeJson(header) + "." + encodeJson(payload);
        String signature = sign(signingInput);
        return new AccessToken(
                signingInput + "." + signature,
                jwtId,
                LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault())
        );
    }

    public JwtClaims parse(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtAuthenticationException();
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new JwtAuthenticationException();
        }
        String signingInput = parts[0] + "." + parts[1];
        String expectedSignature = sign(signingInput);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new JwtAuthenticationException();
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(URL_DECODER.decode(parts[1]), MAP_TYPE);
            Long staffId = Long.valueOf(String.valueOf(payload.get("sub")));
            String jwtId = String.valueOf(payload.get("jti"));
            Instant issuedAt = Instant.ofEpochSecond(numberValue(payload.get("iat")));
            Instant expiresAt = Instant.ofEpochSecond(numberValue(payload.get("exp")));
            if (!expiresAt.isAfter(Instant.now())) {
                throw new JwtAuthenticationException();
            }
            return new JwtClaims(staffId, jwtId, issuedAt, expiresAt);
        } catch (RuntimeException | java.io.IOException ex) {
            throw new JwtAuthenticationException();
        }
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("failed to encode jwt", ex);
        }
    }

    private String sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(iamProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return URL_ENCODER.encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("failed to sign jwt", ex);
        }
    }

    private long numberValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    public record AccessToken(String value, String jwtId, LocalDateTime expiresAt) {
    }
}
